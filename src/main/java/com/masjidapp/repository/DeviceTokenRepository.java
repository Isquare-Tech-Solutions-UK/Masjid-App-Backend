package com.masjidapp.repository;

import com.masjidapp.entity.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, UUID> {

    Optional<DeviceToken> findByFcmToken(String fcmToken);

    List<DeviceToken> findAllByActiveTrue();

    @Modifying
    @Query("UPDATE DeviceToken d SET d.active = false WHERE d.fcmToken IN :tokens")
    void deactivateTokens(@Param("tokens") List<String> tokens);
}
