package com.fintech.riskops.risk.engine;

import com.fintech.riskops.entity.Merchant;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Immutable context object passed to each RiskRule during evaluation.
 *
 * Design: Using a context object (vs passing individual params) means:
 * - Adding new data to evaluation doesn't break existing rule signatures
 * - Rules can selectively use only what they need
 * - Easy to test rules in isolation with mock contexts
 */
@Data
@Builder
public class RiskEvaluationContext {

    private final Merchant merchant;

    // Pre-fetched data to avoid N+1 queries in rules
    /** Transaction count in last 1 hour */
    private final long transactionsLastHour;

    /** Transaction count today */
    private final long transactionsToday;

    /** Total amount transacted today */
    private final BigDecimal dailyVolume;

    /** Largest single transaction amount today */
    private final BigDecimal maxSingleTransactionToday;
}
