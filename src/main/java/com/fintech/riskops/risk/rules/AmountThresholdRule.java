package com.fintech.riskops.risk.rules;

import com.fintech.riskops.risk.engine.RiskEvaluationContext;
import com.fintech.riskops.risk.engine.RiskRule;
import com.fintech.riskops.risk.engine.RuleResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * AMOUNT THRESHOLD RULE
 *
 * Checks two dimensions:
 * 1. Single transaction amount - catches card testing with large amounts
 * 2. Daily volume - catches money laundering via structuring
 *
 * AML note: "Structuring" is when bad actors deliberately keep individual
 * transactions just below reporting thresholds. Daily volume monitoring
 * helps detect this pattern even when individual transactions look normal.
 *
 * Score = max(singleTxnScore, dailyVolumeScore) to avoid double-counting
 * when both limits are exceeded for the same reason.
 */
@Component
public class AmountThresholdRule implements RiskRule {

    @Value("${risk.rules.amount-threshold.max-single-transaction:50000.00}")
    private BigDecimal maxSingleTransaction;

    @Value("${risk.rules.amount-threshold.max-daily-volume:500000.00}")
    private BigDecimal maxDailyVolume;

    @Value("${risk.rules.amount-threshold.weight:35}")
    private int maxWeight;

    @Override
    public RuleResult evaluate(RiskEvaluationContext context) {
        BigDecimal singleTxn = context.getMaxSingleTransactionToday();
        BigDecimal dailyVol = context.getDailyVolume();

        if (singleTxn == null) singleTxn = BigDecimal.ZERO;
        if (dailyVol == null) dailyVol = BigDecimal.ZERO;

        // Calculate ratio for each dimension
        double singleRatio = singleTxn.divide(maxSingleTransaction, 4, RoundingMode.HALF_UP)
            .doubleValue();
        double dailyRatio = dailyVol.divide(maxDailyVolume, 4, RoundingMode.HALF_UP)
            .doubleValue();

        // Use the worse of the two ratios
        double ratio = Math.min(1.0, Math.max(singleRatio, dailyRatio));
        int score = (int) Math.round(ratio * maxWeight);

        boolean triggered = singleRatio >= 1.0 || dailyRatio >= 1.0;

        String reason = String.format(
            "Max single txn: $%.2f (limit: $%.2f, %.0f%%) | Daily volume: $%.2f (limit: $%.2f, %.0f%%)",
            singleTxn, maxSingleTransaction, singleRatio * 100,
            dailyVol, maxDailyVolume, dailyRatio * 100
        );

        return RuleResult.builder()
            .ruleName(getRuleName())
            .score(score)
            .maxWeight(maxWeight)
            .triggered(triggered)
            .reason(reason)
            .build();
    }

    @Override
    public String getRuleName() {
        return "AMOUNT_THRESHOLD_RULE";
    }

    @Override
    public int getMaxWeight() {
        return maxWeight;
    }
}
