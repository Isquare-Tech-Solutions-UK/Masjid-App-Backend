package com.masjidapp.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "prayer_times")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrayerTime {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private LocalDate date;

    @Column(name = "hijri_date", length = 50)
    private String hijriDate;

    /**
     * JSONB column storing prayer times for all six prayers.
     * Example: {
     * "fajr": {"athan": "06:15", "jamah": "06:45"},
     * "sunrise": {"athan": "07:45"},
     * "zuhr": {"athan": "12:30", "jamah": "13:00"},
     * "asr": {"athan": "15:00", "jamah": "15:30"},
     * "maghrib": {"athan": "17:15", "jamah": "17:20"},
     * "isha": {"athan": "18:45", "jamah": "19:15"}
     * }
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode prayers;

    /**
     * JSONB column for Friday Jumu'ah prayer times (null for non-Friday days).
     * Example: [
     * {"khutbah": "13:00", "jamah": "13:30"},
     * {"khutbah": "14:00", "jamah": "14:30"}
     * ]
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "jumuah_times", columnDefinition = "jsonb")
    private JsonNode jumuahTimes;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

}
