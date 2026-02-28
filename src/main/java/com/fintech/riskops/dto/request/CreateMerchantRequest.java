package com.fintech.riskops.dto.request;

import com.fintech.riskops.enums.MerchantFeature;
import com.fintech.riskops.enums.RiskStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Set;

@Data
public class CreateMerchantRequest {

    @NotBlank(message = "Merchant name is required")
    private String name;

    @NotBlank
    @Email(message = "Valid email required")
    private String email;

    private String businessType;

    @NotNull
    private RiskStatus riskStatus = RiskStatus.LOW;

    private Set<MerchantFeature> enabledFeatures;
}
