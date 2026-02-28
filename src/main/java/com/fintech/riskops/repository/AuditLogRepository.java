package com.fintech.riskops.repository;

import com.fintech.riskops.entity.AuditLog;
import com.fintech.riskops.enums.ActionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    /** All audit records for a specific entity (e.g., one merchant's full history) */
    List<AuditLog> findByEntityTypeAndEntityIdOrderByTimestampDesc(
        String entityType, String entityId
    );

    /** Filter by action type - useful for compliance reports ("show me all FREEZEs") */
    Page<AuditLog> findByActionTypeOrderByTimestampDesc(ActionType actionType, Pageable pageable);

    /** Filter by who performed the action */
    Page<AuditLog> findByPerformedByOrderByTimestampDesc(String performedBy, Pageable pageable);

    /** All records from one bulk operation (same correlationId) */
    List<AuditLog> findByCorrelationIdOrderByTimestampAsc(String correlationId);

    /**
     * Flexible multi-filter search for the audit log UI.
     * Null parameters are ignored (treated as "no filter on this field").
     */
    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:actionType IS NULL OR a.actionType = :actionType) AND " +
           "(:entityType IS NULL OR a.entityType = :entityType) AND " +
           "(:performedBy IS NULL OR a.performedBy = :performedBy) AND " +
           "(:from IS NULL OR a.timestamp >= :from) AND " +
           "(:to IS NULL OR a.timestamp <= :to) " +
           "ORDER BY a.timestamp DESC")
    Page<AuditLog> searchAuditLogs(
        @Param("actionType") ActionType actionType,
        @Param("entityType") String entityType,
        @Param("performedBy") String performedBy,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to,
        Pageable pageable
    );

    /** For CSV export: all logs within a date range (no pagination) */
    @Query("SELECT a FROM AuditLog a WHERE a.timestamp BETWEEN :from AND :to ORDER BY a.timestamp DESC")
    List<AuditLog> findByTimestampBetween(
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
    );
}
