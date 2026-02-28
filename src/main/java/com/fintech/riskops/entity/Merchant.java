package com.fintech.riskops.entity;

import com.fintech.riskops.enums.MerchantFeature;
import com.fintech.riskops.enums.RiskStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Core merchant entity. In a real fintech system this would join to
 * KYC records, bank accounts, and compliance documents. For this
 * dashboard we focus on the risk-relevant fields.
 *
 * Design decisions:
 * - UUID primary key: avoids sequential ID enumeration attacks
 * - enabledFeatures stored as @ElementCollection: each feature toggle
 *   is a separate row in merchant_features, queryable by index
 * - riskScore is a cached value updated by the risk engine so the
 *   dashboard doesn't need to recompute on every page load
 */
@Entity
@Table(
    name = "merchants",
    indexes = {
        @Index(name = "idx_merchant_risk_status", columnList = "risk_status"),
        @Index(name = "idx_merchant_created_at", columnList = "created_at"),
        @Index(name = "idx_merchant_name", columnList = "name")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Merchant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "business_type", length = 100)
    private String businessType;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_status", nullable = false, length = 20)
    private RiskStatus riskStatus;

    /**
     * Cached risk score (0-100) from last risk engine evaluation.
     * Stored in DB to support dashboard queries without re-running engine.
     */
    @Column(name = "risk_score", nullable = false)
    @Builder.Default
    private int riskScore = 0;

    /**
     * Stores enabled features as enum strings.
     * Separate table allows: WHERE feature = 'WITHDRAWALS' queries.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "merchant_features",
        joinColumns = @JoinColumn(name = "merchant_id"),
        indexes = @Index(name = "idx_merchant_feature", columnList = "merchant_id, feature")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "feature", length = 50)
    @Builder.Default
    private Set<MerchantFeature> enabledFeatures = new HashSet<>();

    /**
     * Whether merchant has been reviewed by a risk analyst after flagging.
     * Drives the "pending reviews" dashboard metric.
     */
    @Column(name = "review_pending", nullable = false)
    @Builder.Default
    private boolean reviewPending = false;

    @Column(name = "flagged_at")
    private LocalDateTime flaggedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ---- Domain logic helpers ----

    public void freeze() {
        this.riskStatus = RiskStatus.FROZEN;
        this.enabledFeatures.clear(); // Frozen = zero permissions
    }

    public void unfreeze(RiskStatus restoreStatus) {
        this.riskStatus = restoreStatus;
    }

    public boolean isFrozen() {
        return this.riskStatus == RiskStatus.FROZEN;
    }

    public void enableFeature(MerchantFeature feature) {
        this.enabledFeatures.add(feature);
    }

    public void disableFeature(MerchantFeature feature) {
        this.enabledFeatures.remove(feature);
    }
}
