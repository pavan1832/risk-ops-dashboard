package com.fintech.riskops.service;

import com.fintech.riskops.dto.response.RiskEvaluationResponse;
import com.fintech.riskops.entity.Merchant;
import com.fintech.riskops.enums.ActionType;
import com.fintech.riskops.enums.RiskStatus;
import com.fintech.riskops.repository.TransactionRepository;
import com.fintech.riskops.risk.engine.RiskEngine;
import com.fintech.riskops.risk.engine.RiskEvaluationContext;
import com.fintech.riskops.risk.engine.RiskEvaluationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Orchestrates the full risk evaluation pipeline:
 * 1. Fetch merchant + transaction data
 * 2. Build evaluation context
 * 3. Run risk engine
 * 4. Persist score changes
 * 5. Auto-freeze if threshold exceeded
 * 6. Write audit trail
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RiskEvaluationService {

    private final MerchantService merchantService;
    private final TransactionRepository transactionRepository;
    private final RiskEngine riskEngine;
    private final AuditLogService auditLogService;

    /**
     * Evaluate risk for a single merchant.
     * This method is the core "evaluate" action that can be:
     * - Called manually by a risk analyst via API
     * - Triggered automatically after each transaction
     * - Run in batch by a scheduled job
     */
    @Transactional
    public RiskEvaluationResponse evaluate(UUID merchantId) {
        Merchant merchant = merchantService.findById(merchantId);
        RiskStatus previousStatus = merchant.getRiskStatus();

        // ---- Build context: fetch all data the rules need ----
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        RiskEvaluationContext context = RiskEvaluationContext.builder()
            .merchant(merchant)
            .transactionsLastHour(
                transactionRepository.countByMerchantIdSince(merchantId, oneHourAgo)
            )
            .transactionsToday(
                transactionRepository.countDailyTransactions(merchantId, startOfDay, endOfDay)
            )
            .dailyVolume(
                transactionRepository.sumAmountByMerchantIdSince(merchantId, startOfDay)
            )
            .maxSingleTransactionToday(
                // In production this would be a MAX(amount) query; using daily volume as proxy here
                transactionRepository.sumAmountByMerchantIdSince(merchantId, startOfDay)
            )
            .build();

        // ---- Run the engine ----
        RiskEvaluationResult result = riskEngine.evaluate(context);

        // ---- Persist updated risk score ----
        merchant.setRiskScore(result.getTotalScore());

        boolean autoFrozen = false;

        if (result.isShouldAutoFreeze() && !merchant.isFrozen()) {
            // Auto-freeze: score crossed the danger threshold
            log.warn("AUTO-FREEZE triggered for merchant {} (score: {})",
                merchantId, result.getTotalScore());
            merchant.freeze();
            merchant.setReviewPending(true);
            merchant.setFlaggedAt(LocalDateTime.now());
            autoFrozen = true;

            auditLogService.log(
                ActionType.RISK_AUTO_FROZEN,
                "MERCHANT",
                merchantId.toString(),
                previousStatus.name(),
                "FROZEN",
                "RISK_ENGINE",
                String.format("Auto-frozen: score %d/100 exceeded threshold. %s",
                    result.getTotalScore(), result.toSummary())
            );

        } else if (result.isRiskLevelChanged()) {
            // Risk level changed but not auto-frozen
            if (!merchant.isFrozen()) {
                merchant.setRiskStatus(result.getRiskLevel());
            }
            if (result.getRiskLevel() == RiskStatus.HIGH) {
                merchant.setReviewPending(true);
                merchant.setFlaggedAt(LocalDateTime.now());
            }

            auditLogService.log(
                ActionType.RISK_STATUS_CHANGED,
                "MERCHANT",
                merchantId.toString(),
                previousStatus.name(),
                result.getRiskLevel().name(),
                "RISK_ENGINE",
                result.toSummary()
            );
        }

        // Always log the evaluation itself for the full audit trail
        auditLogService.log(
            ActionType.RISK_EVALUATED,
            "MERCHANT",
            merchantId.toString(),
            "score:" + merchant.getRiskScore(),
            "score:" + result.getTotalScore(),
            "RISK_ENGINE",
            result.toSummary()
        );

        // Save merchant with updated score/status
        // (We access the repository indirectly since findById already loaded the managed entity)
        merchantService.findById(merchantId); // ensure in persistence context
        merchant = merchantService.findById(merchantId); // re-fetch after update
        merchant.setRiskScore(result.getTotalScore());
        if (!autoFrozen && result.isRiskLevelChanged() && !merchant.isFrozen()) {
            merchant.setRiskStatus(result.getRiskLevel());
        }
        if (autoFrozen) {
            merchant.freeze();
            merchant.setReviewPending(true);
            merchant.setFlaggedAt(LocalDateTime.now());
        }

        return RiskEvaluationResponse.builder()
            .merchantId(merchantId)
            .merchantName(merchant.getName())
            .riskScore(result.getTotalScore())
            .riskLevel(result.getRiskLevel())
            .previousRiskLevel(previousStatus)
            .riskLevelChanged(result.isRiskLevelChanged())
            .autoFrozen(autoFrozen)
            .ruleBreakdown(result.getRuleResults())
            .summary(result.toSummary())
            .build();
    }
}
