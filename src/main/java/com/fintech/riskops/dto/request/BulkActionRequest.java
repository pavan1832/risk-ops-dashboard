package com.fintech.riskops.dto.request;

import com.fintech.riskops.enums.MerchantFeature;
import com.fintech.riskops.enums.RiskStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * Request payload for all bulk merchant operations.
 * A single DTO handles freeze, unfreeze, and feature toggles
 * to keep the API surface minimal.
 */
@Data
public class BulkActionRequest {

    @NotEmpty(message = "At least one merchant ID is required")
    private List<UUID> merchantIds;

    /** Required for freeze/unfreeze operations (null for feature toggles) */
    private RiskStatus targetStatus;

    /** Required for feature enable/disable operations (null for status changes) */
    private MerchantFeature feature;

    /** Optional note from the risk analyst explaining the action */
    private String reason;
}
