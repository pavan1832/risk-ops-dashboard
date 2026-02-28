package com.fintech.riskops.repository;

import com.fintech.riskops.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    /**
     * Velocity rule query: count transactions for a merchant in a time window.
     * Called by VelocityRule during risk evaluation.
     */
    @Query("SELECT COUNT(t) FROM Transaction t " +
           "WHERE t.merchant.id = :merchantId AND t.createdAt >= :since")
    long countByMerchantIdSince(
        @Param("merchantId") UUID merchantId,
        @Param("since") LocalDateTime since
    );

    /**
     * Amount threshold query: sum all transaction amounts for today.
     * Used by AmountThresholdRule to check daily volume limit.
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.merchant.id = :merchantId AND t.createdAt >= :since")
    BigDecimal sumAmountByMerchantIdSince(
        @Param("merchantId") UUID merchantId,
        @Param("since") LocalDateTime since
    );

    /**
     * Frequency rule: daily transaction count.
     */
    @Query("SELECT COUNT(t) FROM Transaction t " +
           "WHERE t.merchant.id = :merchantId " +
           "AND t.createdAt >= :startOfDay AND t.createdAt < :endOfDay")
    long countDailyTransactions(
        @Param("merchantId") UUID merchantId,
        @Param("startOfDay") LocalDateTime startOfDay,
        @Param("endOfDay") LocalDateTime endOfDay
    );
}
