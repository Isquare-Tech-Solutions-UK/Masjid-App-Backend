package com.masjidapp.repository;

import com.masjidapp.entity.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdminUserRepository extends JpaRepository<AdminUser, UUID> {

    /**
     * Find admin user by email
     */
    Optional<AdminUser> findByEmail(String email);

    /**
     * Find admin user by username
     */
    Optional<AdminUser> findByUsername(String username);

    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);

    /**
     * Check if username exists
     */
    boolean existsByUsername(String username);

    /**
     * Update last login timestamp
     */
    @Modifying
    @Query("UPDATE AdminUser u SET u.lastLoginAt = :loginTime, u.failedLoginAttempts = 0 WHERE u.id = :userId")
    void updateLastLoginAt(@Param("userId") UUID userId, @Param("loginTime") LocalDateTime loginTime);

    /**
     * Increment failed login attempts
     */
    @Modifying
    @Query("UPDATE AdminUser u SET u.failedLoginAttempts = u.failedLoginAttempts + 1 WHERE u.id = :userId")
    void incrementFailedLoginAttempts(@Param("userId") UUID userId);

    /**
     * Reset failed login attempts
     */
    @Modifying
    @Query("UPDATE AdminUser u SET u.failedLoginAttempts = 0 WHERE u.id = :userId")
    void resetFailedLoginAttempts(@Param("userId") UUID userId);

}
