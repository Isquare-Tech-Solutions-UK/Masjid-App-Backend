package com.masjidapp.repository;

import com.masjidapp.entity.Campaign;
import com.masjidapp.entity.CampaignStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CampaignRepository extends JpaRepository<Campaign, UUID> {

    List<Campaign> findByOrderByStatusAndEndDateDesc();
}
