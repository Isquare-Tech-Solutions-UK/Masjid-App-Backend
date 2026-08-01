package com.masjidapp.repository;

import com.masjidapp.entity.MasjidSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SettingsRepository extends JpaRepository<MasjidSettings, UUID> {
}
