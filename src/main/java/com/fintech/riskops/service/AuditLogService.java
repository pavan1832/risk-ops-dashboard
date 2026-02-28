package com.fintech.riskops.service;

import com.fintech.riskops.dto.response.AuditLogResponse;
import com.fintech.riskops.entity.AuditLog;
import com.fintech.riskops.enums.ActionType;
import com.fintech.riskops.repository.AuditLogRepository;
import com.opencsv.CSVWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Audit logging service.
 *
 * CRITICAL DESIGN DECISIONS:
 *
 * 1. Propagation.REQUIRES_NEW: Audit log writes use a SEPARATE transaction.
 *    This ensures audit records persist even if the main operation fails/rolls back.
 *    Without this, a failed merchant update would also roll back its audit entry,
 *    creating gaps in the compliance trail.
 *
 * 2. @Async for non-critical paths: Some audit writes (e.g., read-only event logs)
 *    can be async to avoid adding latency to API responses.
 *    For state-changing events, we log synchronously (REQUIRES_NEW) to guarantee
 *    the record exists before returning to the caller.
 *
 * 3. Immutability: No update/delete methods are exposed. Period.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Log an action. Uses REQUIRES_NEW so the audit write is independent
     * of the caller's transaction - audit record survives rollbacks.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog log(
        ActionType actionType,
        String entityType,
        String entityId,
        String oldValue,
        String newValue,
        String performedBy,
        String correlationId,
        String description
    ) {
        AuditLog entry = AuditLog.builder()
            .actionType(actionType)
            .entityType(entityType)
            .entityId(entityId)
            .oldValue(oldValue)
            .newValue(newValue)
            .performedBy(performedBy)
            .correlationId(correlationId)
            .description(description)
            .build();

        AuditLog saved = auditLogRepository.save(entry);
        log.debug("Audit logged: {} on {} {} by {}", actionType, entityType, entityId, performedBy);
        return saved;
    }

    /** Convenience overload without correlationId (single-entity operations) */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog log(
        ActionType actionType,
        String entityType,
        String entityId,
        String oldValue,
        String newValue,
        String performedBy,
        String description
    ) {
        return log(actionType, entityType, entityId, oldValue, newValue, performedBy, null, description);
    }

    /** Get full audit history for a specific entity */
    @Transactional(readOnly = true)
    public List<AuditLogResponse> getEntityHistory(String entityType, String entityId) {
        return auditLogRepository
            .findByEntityTypeAndEntityIdOrderByTimestampDesc(entityType, entityId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    /** Paginated, filtered audit log search */
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> search(
        ActionType actionType,
        String entityType,
        String performedBy,
        LocalDateTime from,
        LocalDateTime to,
        Pageable pageable
    ) {
        return auditLogRepository
            .searchAuditLogs(actionType, entityType, performedBy, from, to, pageable)
            .map(this::toResponse);
    }

    /**
     * Export audit logs to CSV for compliance team.
     * Returns CSV as a String; caller wraps in HTTP response with proper headers.
     */
    @Transactional(readOnly = true)
    public String exportToCsv(LocalDateTime from, LocalDateTime to) {
        List<AuditLog> logs = auditLogRepository.findByTimestampBetween(from, to);

        StringWriter sw = new StringWriter();
        try (CSVWriter writer = new CSVWriter(sw)) {
            // Header row
            writer.writeNext(new String[]{
                "ID", "Action Type", "Entity Type", "Entity ID",
                "Old Value", "New Value", "Performed By",
                "Correlation ID", "Description", "Timestamp"
            });

            DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            for (AuditLog log : logs) {
                writer.writeNext(new String[]{
                    log.getId().toString(),
                    log.getActionType().name(),
                    log.getEntityType(),
                    log.getEntityId(),
                    log.getOldValue() != null ? log.getOldValue() : "",
                    log.getNewValue() != null ? log.getNewValue() : "",
                    log.getPerformedBy(),
                    log.getCorrelationId() != null ? log.getCorrelationId() : "",
                    log.getDescription() != null ? log.getDescription() : "",
                    log.getTimestamp().format(fmt)
                });
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate CSV export", e);
        }

        return sw.toString();
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return AuditLogResponse.builder()
            .id(log.getId())
            .actionType(log.getActionType())
            .entityType(log.getEntityType())
            .entityId(log.getEntityId())
            .oldValue(log.getOldValue())
            .newValue(log.getNewValue())
            .performedBy(log.getPerformedBy())
            .correlationId(log.getCorrelationId())
            .description(log.getDescription())
            .timestamp(log.getTimestamp())
            .build();
    }
}
