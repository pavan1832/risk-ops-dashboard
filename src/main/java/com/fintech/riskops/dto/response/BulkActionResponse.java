package com.fintech.riskops.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class BulkActionResponse {
    private int requestedCount;
    private int successCount;
    private int failedCount;
    private List<UUID> successIds;
    private List<String> errors;
    private String correlationId;  // Links to audit log entries
    private String message;
}
