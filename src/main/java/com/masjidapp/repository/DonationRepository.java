package com.masjidapp.repository;

import com.masjidapp.entity.Donation;
import com.masjidapp.entity.DonationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.UUID;

public interface DonationRepository extends JpaRepository<Donation, UUID> {

    Page<Donation> findByCampaignId(UUID uuid, Pageable pageable);
    Page<Donation> findByCampaignIdAndStatus(UUID uuid, DonationStatus status, Pageable pageable);

    @Modifying
    @Query("""
        UPDATE Donation d
        SET d.status = 'completed',
            d.completedAt = CURRENT_TIMESTAMP,
            d.updatedAt = CURRENT_TIMESTAMP
        WHERE d.id = :id AND d.status != 'completed'
    """)
    int markCompletedIfNotAlready(@Param("id") UUID id);

    @Query(value = "SELECT COALESCE(SUM(amount), 0) FROM donations WHERE campaign_id = :campaignId AND status = 'completed'", nativeQuery = true)
    BigDecimal sumCompletedAmountsByCampaignId(@Param("campaignId") UUID campaignId);

    @Query(value = "SELECT COUNT(*) FROM donations WHERE campaign_id = :campaignId AND status = 'completed'", nativeQuery = true)
    long countCompletedByCampaignId(@Param("campaignId") UUID campaignId);

    @Query(value = "SELECT COALESCE(SUM(amount), 0) FROM donations WHERE status = 'completed'", nativeQuery = true)
    BigDecimal sumAllCompleted();

    @Query(value = "SELECT COUNT(*) FROM donations WHERE status = 'completed'", nativeQuery = true)
    long countAllCompleted();

    @Query(value = "SELECT COALESCE(SUM(amount), 0) FROM donations WHERE status = 'completed' AND DATE_TRUNC('month', completed_at) = DATE_TRUNC('month', NOW())", nativeQuery = true)
    BigDecimal sumCompletedThisMonth();

}
