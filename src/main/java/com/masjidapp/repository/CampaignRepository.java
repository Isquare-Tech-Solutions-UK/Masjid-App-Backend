package com.masjidapp.repository;

import com.masjidapp.entity.Campaign;
import com.masjidapp.entity.CampaignStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.UUID;

public interface CampaignRepository extends JpaRepository<Campaign, UUID> {

    Page<Campaign> findByOrderByStartDateDescEndDateAsc(Pageable pageable);
    Page<Campaign> findByStatusInOrderByStartDateDescEndDateAsc(
            Pageable pageable, Iterable<CampaignStatus> statuses);

    @Modifying
    @Query("""
        UPDATE Campaign c
        SET c.raisedAmount = COALESCE(c.raisedAmount, 0) + :amount,
            c.donorCount = COALESCE(c.donorCount, 0) + 1,
            c.updatedAt = CURRENT_TIMESTAMP
        WHERE c.id = :campaignId
    """)
    void incrementCampaign(UUID campaignId, BigDecimal amount);

    long countByStatus(CampaignStatus status);

}
