package com.fintech.riskops.risk.engine;

import com.fintech.riskops.enums.RiskStatus;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * Aggregated result from the full risk engine evaluation.
 * Returned to the caller (service layer) who decides what action to take.
 */
@Data
@Builder
public class RiskEvaluationResult {

    private final UUID merchantId;

    /** Composite score 0-100 (sum of all rule scores) */
    private final int totalScore;

    /** Derived risk level based on configurable thresholds */
    private final RiskStatus riskLevel;

    /** Whether the engine recommends auto-freezing this merchant */
    private final boolean shouldAutoFreeze;

    /** Whether the risk level changed from previous evaluation */
    private final boolean riskLevelChanged;

    /** Individual rule results for transparency in audit log */
    private final List<RuleResult> ruleResults;

    /**
     * Build a JSON-friendly summary for storage in audit log newValue field.
     */
    public String toSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Score: %d/100 | Level: %s", totalScore, riskLevel));
        if (shouldAutoFreeze) {
            sb.append(" | AUTO-FREEZE TRIGGERED");
        }
        sb.append(" | Rules: [");
        for (RuleResult r : ruleResults) {
            if (r.isTriggered()) {
                sb.append(r.getRuleName()).append("(+").append(r.getScore()).append("), ");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
