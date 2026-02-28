package com.fintech.riskops.dto.response;

import com.fintech.riskops.enums.RiskStatus;
import com.fintech.riskops.risk.engine.RuleResult;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class RiskEvaluationResponse {
    private UUID merchantId;
    private String merchantName;
    private int riskScore;
    private RiskStatus riskLevel;
    private RiskStatus previousRiskLevel;
    private boolean riskLevelChanged;
    private boolean autoFrozen;
    private List<RuleResult> ruleBreakdown;
    private String summary;
}
