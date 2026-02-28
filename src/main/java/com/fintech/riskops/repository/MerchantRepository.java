package com.fintech.riskops.repository;

import com.fintech.riskops.entity.Merchant;
import com.fintech.riskops.enums.RiskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, UUID> {

    Page<Merchant> findByRiskStatus(RiskStatus status, Pageable pageable);

    List<Merchant> findByIdIn(List<UUID> ids);

    long countByRiskStatus(RiskStatus status);

    long countByReviewPendingTrue();

    // Merchants flagged today
    @Query("SELECT COUNT(m) FROM Merchant m WHERE m.flaggedAt >= :startOfDay")
    long countFlaggedSince(@Param("startOfDay") LocalDateTime startOfDay);

    // Merchants resolved today
    @Query("SELECT COUNT(m) FROM Merchant m WHERE m.resolvedAt >= :startOfDay")
    long countResolvedSince(@Param("startOfDay") LocalDateTime startOfDay);

    // Search by name (case-insensitive, supports partial match)
    Page<Merchant> findByNameContainingIgnoreCase(String name, Pageable pageable);

    // Risk status filter + name search combined
    @Query("SELECT m FROM Merchant m WHERE " +
           "(:status IS NULL OR m.riskStatus = :status) AND " +
           "(:name IS NULL OR LOWER(m.name) LIKE LOWER(CONCAT('%', :name, '%')))")
    Page<Merchant> findWithFilters(
        @Param("status") RiskStatus status,
        @Param("name") String name,
        Pageable pageable
    );

    /**
     * Bulk status update using a single JPQL UPDATE.
     * Much more efficient than loading + updating each merchant individually.
     * Returns count of affected rows for verification.
     */
    @Modifying
    @Query("UPDATE Merchant m SET m.riskStatus = :status, m.updatedAt = :now " +
           "WHERE m.id IN :ids")
    int bulkUpdateRiskStatus(
        @Param("ids") List<UUID> ids,
        @Param("status") RiskStatus status,
        @Param("now") LocalDateTime now
    );
}
