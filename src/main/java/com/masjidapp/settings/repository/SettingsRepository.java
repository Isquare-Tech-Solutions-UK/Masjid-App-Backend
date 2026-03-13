package com.masjidapp.settings.repository;

import com.masjidapp.settings.entity.MasjidSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SettingsRepository extends JpaRepository<MasjidSettings, UUID> {
    // Usually we only have one row, so we might not need special queries.
}
