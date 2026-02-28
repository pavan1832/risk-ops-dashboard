package com.fintech.riskops.risk.engine;

import com.fintech.riskops.enums.RiskStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * RISK ENGINE ORCHESTRATOR
 *
 * Collects all registered RiskRule implementations (via Spring's DI),
 * runs them against the evaluation context, and aggregates the result.
 *
 * DESIGN: Spring automatically injects all beans implementing RiskRule.
 * Adding a new rule = create a new @Component class. Zero changes here.
 *
 * This is the purest expression of the Open/Closed Principle in this codebase.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RiskEngine {

    /** Spring injects all RiskRule implementations automatically */
    private final List<RiskRule> rules;

    @Value("${risk.thresholds.medium-risk-score:40}")
    private int mediumRiskThreshold;

    @Value("${risk.thresholds.high-risk-score:70}")
    private int highRiskThreshold;

    @Value("${risk.thresholds.auto-freeze-score:90}")
    private int autoFreezeThreshold;

    /**
     * Run all rules and compute the aggregate risk level.
     *
     * @param context Pre-populated evaluation context (merchant + transaction data)
     * @return Full evaluation result including per-rule breakdown
     */
    public RiskEvaluationResult evaluate(RiskEvaluationContext context) {
        log.debug("Running risk evaluation for merchant: {}", context.getMerchant().getId());

        List<RuleResult> ruleResults = new ArrayList<>();
        int totalScore = 0;

        // Execute each rule independently and accumulate scores
        for (RiskRule rule : rules) {
            try {
                RuleResult result = rule.evaluate(context);
                ruleResults.add(result);
                totalScore += result.getScore();

                if (result.isTriggered()) {
                    log.info("Rule {} TRIGGERED for merchant {} - Score: {}/{}. Reason: {}",
                        rule.getRuleName(),
                        context.getMerchant().getId(),
                        result.getScore(),
                        result.getMaxWeight(),
                        result.getReason()
                    );
                }
            } catch (Exception e) {
                // Rule failure should never prevent other rules from running
                log.error("Risk rule {} failed for merchant {}: {}",
                    rule.getRuleName(), context.getMerchant().getId(), e.getMessage(), e);
                ruleResults.add(RuleResult.builder()
                    .ruleName(rule.getRuleName())
                    .score(0)
                    .maxWeight(rule.getMaxWeight())
                    .triggered(false)
                    .reason("RULE_ERROR: " + e.getMessage())
                    .build());
            }
        }

        // Cap at 100 to keep consistent scale
        totalScore = Math.min(100, totalScore);

        RiskStatus riskLevel = determineRiskLevel(totalScore);
        boolean shouldAutoFreeze = totalScore >= autoFreezeThreshold;
        boolean riskLevelChanged = context.getMerchant().getRiskStatus() != riskLevel
            && riskLevel != RiskStatus.FROZEN; // Don't "change" already-frozen merchants

        return RiskEvaluationResult.builder()
            .merchantId(context.getMerchant().getId())
            .totalScore(totalScore)
            .riskLevel(riskLevel)
            .shouldAutoFreeze(shouldAutoFreeze)
            .riskLevelChanged(riskLevelChanged)
            .ruleResults(ruleResults)
            .build();
    }

    /**
     * Map numeric score to RiskStatus enum.
     * Thresholds are configurable in application.yml.
     */
    private RiskStatus determineRiskLevel(int score) {
        if (score >= highRiskThreshold) return RiskStatus.HIGH;
        if (score >= mediumRiskThreshold) return RiskStatus.MEDIUM;
        return RiskStatus.LOW;
    }
}
