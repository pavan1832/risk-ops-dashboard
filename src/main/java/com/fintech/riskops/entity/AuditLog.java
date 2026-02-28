package com.fintech.riskops.entity;

import com.fintech.riskops.enums.ActionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Immutable audit log record.
 *
 * COMPLIANCE CRITICAL:
 * - No @UpdateTimestamp: records must never change after creation
 * - No delete endpoints exposed for this entity
 * - oldValue / newValue stored as JSON strings for human readability
 *   and to avoid schema changes when entity structures evolve
 *
 * Design decisions:
 * - entityType as String (not enum) to support future entity types
 *   without requiring a code/DB migration
 * - performedBy stores the username/role string so audit records remain
 *   meaningful even if the user account is later deleted
 * - correlationId groups related records from the same bulk operation
 *   (e.g., "bulk freeze job #X froze 47 merchants" = 47 records, 1 correlationId)
 */
@Entity
@Table(
    name = "audit_logs",
    indexes = {
        @Index(name = "idx_audit_entity", columnList = "entity_type, entity_id"),
        @Index(name = "idx_audit_action_type", columnList = "action_type"),
        @Index(name = "idx_audit_performed_by", columnList = "performed_by"),
        @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
        @Index(name = "idx_audit_correlation", columnList = "correlation_id")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 50)
    private ActionType actionType;

    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    @Column(name = "entity_id", nullable = false, length = 36)
    private String entityId;

    /** JSON snapshot of old state (null for CREATE operations) */
    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    /** JSON snapshot of new state (null for DELETE operations) */
    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    /** Who performed this action (username or system process name) */
    @Column(name = "performed_by", nullable = false, length = 255)
    private String performedBy;

    /** Groups audit records from the same bulk operation */
    @Column(name = "correlation_id", length = 36)
    private String correlationId;

    /** Human-readable summary for quick compliance review */
    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        if (this.timestamp == null) {
            this.timestamp = LocalDateTime.now();
        }
    }
}
