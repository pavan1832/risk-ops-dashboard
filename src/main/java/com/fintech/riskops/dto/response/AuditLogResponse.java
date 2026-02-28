package com.fintech.riskops.dto.response;

import com.fintech.riskops.enums.ActionType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class AuditLogResponse {
    private UUID id;
    private ActionType actionType;
    private String entityType;
    private String entityId;
    private String oldValue;
    private String newValue;
    private String performedBy;
    private String correlationId;
    private String description;
    private LocalDateTime timestamp;
}
