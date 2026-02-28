package com.fintech.riskops.risk.engine;

import com.fintech.riskops.entity.Merchant;

/**
 * Strategy interface for risk evaluation rules.
 *
 * DESIGN PATTERN: Strategy Pattern
 * Each rule is an independent strategy that:
 * 1. Evaluates a specific risk dimension
 * 2. Returns a score contribution (0 to its max weight)
 * 3. Can be added/removed without touching other rules
 *
 * This allows risk analysts to:
 * - Enable/disable individual rules
 * - Tune weights in application.yml
 * - Add new rule types (e.g., GeographyRule, DeviceFingerprintRule)
 *   without modifying the engine
 *
 * Open/Closed Principle: Engine is open for extension (add new rules),
 * closed for modification (engine code doesn't change).
 */
public interface RiskRule {

    /**
     * Evaluate this risk dimension for the given merchant.
     *
     * @param context Contains merchant + supporting data needed for evaluation
     * @return RuleResult with score contribution and human-readable reason
     */
    RuleResult evaluate(RiskEvaluationContext context);

    /**
     * Unique name for this rule, used in audit logs and dashboards.
     */
    String getRuleName();

    /**
     * Maximum score contribution from this rule (configured in application.yml).
     * Sum of all rule weights = 100 (full risk score scale).
     */
    int getMaxWeight();
}
