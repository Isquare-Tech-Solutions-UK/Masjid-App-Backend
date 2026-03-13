package com.masjidapp.repository;

import com.masjidapp.entity.Campaign;
import com.masjidapp.entity.CampaignStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CampaignRepository extends JpaRepository<Campaign, UUID> {

    Page<Campaign> findByOrderByStartDateDescEndDateAsc(Pageable pageable);
    Page<Campaign> findByStatusInOrderByStartDateDescEndDateAsc(
            Pageable pageable, Iterable<CampaignStatus> statuses);

}
