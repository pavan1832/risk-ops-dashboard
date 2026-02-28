package com.fintech.riskops.risk.rules;

import com.fintech.riskops.risk.engine.RiskEvaluationContext;
import com.fintech.riskops.risk.engine.RiskRule;
import com.fintech.riskops.risk.engine.RuleResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * FREQUENCY RULE
 *
 * Monitors total daily transaction count across all time windows.
 * Complements VelocityRule (hourly) with a daily perspective.
 *
 * Why both velocity + frequency?
 * - A merchant doing 10 txn/hour for 24 hours (240 total) would look
 *   fine under velocity alone, but frequency catches the cumulative excess.
 * - Different fraud patterns hit different rules:
 *   - Burst fraud -> VelocityRule
 *   - Sustained high volume -> FrequencyRule
 *   - Large single transactions -> AmountThresholdRule
 */
@Component
public class FrequencyRule implements RiskRule {

    @Value("${risk.rules.frequency.max-daily-transactions:200}")
    private long maxDailyTransactions;

    @Value("${risk.rules.frequency.weight:25}")
    private int maxWeight;

    @Override
    public RuleResult evaluate(RiskEvaluationContext context) {
        long actual = context.getTransactionsToday();
        boolean triggered = actual >= maxDailyTransactions;

        double ratio = Math.min(1.0, (double) actual / maxDailyTransactions);
        int score = (int) Math.round(ratio * maxWeight);

        String reason = String.format(
            "%d daily transactions (limit: %d) → %.0f%% of threshold",
            actual, maxDailyTransactions, ratio * 100
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
        return "FREQUENCY_RULE";
    }

    @Override
    public int getMaxWeight() {
        return maxWeight;
    }
}
