package com.fintech.riskops.risk.engine;

import lombok.Builder;
import lombok.Data;

/**
 * Result from a single risk rule evaluation.
 * Captured in the audit log so analysts understand WHY a score was given.
 */
@Data
@Builder
public class RuleResult {

    private final String ruleName;

    /** Score contribution from this rule (0 to maxWeight) */
    private final int score;

    /** Maximum possible score from this rule */
    private final int maxWeight;

    /** True if this rule alone considers the merchant high-risk */
    private final boolean triggered;

    /** Human-readable explanation: "47 transactions in last hour (limit: 100)" */
    private final String reason;
}
