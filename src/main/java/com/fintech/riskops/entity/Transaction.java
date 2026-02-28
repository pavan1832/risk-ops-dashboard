package com.fintech.riskops.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a payment transaction initiated by a merchant.
 *
 * Design decisions:
 * - BigDecimal for amount: never use float/double for monetary values
 * - Indexed on (merchant_id, created_at) to support fast velocity queries
 *   in the risk engine (e.g., "transactions in last 1 hour for merchant X")
 * - flagged field allows soft-marking without deleting records (audit trail)
 */
@Entity
@Table(
    name = "transactions",
    indexes = {
        // Critical composite index for velocity/frequency rule queries
        @Index(name = "idx_txn_merchant_created", columnList = "merchant_id, created_at"),
        @Index(name = "idx_txn_flagged", columnList = "flagged"),
        @Index(name = "idx_txn_amount", columnList = "amount")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "USD";

    @Column(name = "transaction_type", length = 50)
    private String transactionType; // PAYMENT, WITHDRAWAL, REFUND

    @Column(name = "flagged", nullable = false)
    @Builder.Default
    private boolean flagged = false;

    @Column(name = "flag_reason", length = 500)
    private String flagReason;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
