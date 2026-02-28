package com.fintech.riskops.dto.response;

import com.fintech.riskops.enums.MerchantFeature;
import com.fintech.riskops.enums.RiskStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
public class MerchantResponse {
    private UUID id;
    private String name;
    private String email;
    private String businessType;
    private RiskStatus riskStatus;
    private int riskScore;
    private Set<MerchantFeature> enabledFeatures;
    private boolean reviewPending;
    private LocalDateTime flaggedAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
