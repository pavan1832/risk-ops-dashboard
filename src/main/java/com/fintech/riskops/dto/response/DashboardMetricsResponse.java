package com.fintech.riskops.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * Real-time dashboard metrics response.
 * Designed to power the risk ops team's daily monitoring view.
 */
@Data
@Builder
public class DashboardMetricsResponse {

    // Today's activity
    private long flaggedToday;
    private long resolvedToday;

    // Current state
    private long pendingReviews;
    private long frozenMerchants;
    private long highRiskMerchants;
    private long mediumRiskMerchants;
    private long lowRiskMerchants;
    private long totalMerchants;

    // Score distribution summary
    private double averageRiskScore;
}
