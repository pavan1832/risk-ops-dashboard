package com.fintech.riskops.risk.rules;

import com.fintech.riskops.risk.engine.RiskEvaluationContext;
import com.fintech.riskops.risk.engine.RiskRule;
import com.fintech.riskops.risk.engine.RuleResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * VELOCITY RULE
 *
 * Detects abnormally high transaction rates within a short time window.
 * Classic fraud signal: stolen cards are used as fast as possible before
 * the victim notices and calls their bank.
 *
 * Example: A merchant processing 95 transactions in 1 hour when their
 * normal baseline is 10/hour should trigger HIGH risk.
 *
 * Scoring: Linear scale from 0 to maxWeight based on % of limit used.
 * At 50% of limit -> score = 20 (maxWeight=40)
 * At 100% of limit -> score = 40 (full weight)
 * Over limit -> score = 40 + bonus capped at maxWeight
 */
@Component
public class VelocityRule implements RiskRule {

    @Value("${risk.rules.velocity.max-transactions-per-hour:100}")
    private long maxTransactionsPerHour;

    @Value("${risk.rules.velocity.weight:40}")
    private int maxWeight;

    @Override
    public RuleResult evaluate(RiskEvaluationContext context) {
        long actual = context.getTransactionsLastHour();
        boolean triggered = actual >= maxTransactionsPerHour;

        // Proportional score: how close to the limit are they?
        double ratio = Math.min(1.0, (double) actual / maxTransactionsPerHour);
        int score = (int) Math.round(ratio * maxWeight);

        String reason = String.format(
            "%d transactions in last hour (limit: %d) → %.0f%% of threshold",
            actual, maxTransactionsPerHour, ratio * 100
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
        return "VELOCITY_RULE";
    }

    @Override
    public int getMaxWeight() {
        return maxWeight;
    }
}
