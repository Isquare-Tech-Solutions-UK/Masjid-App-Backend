package com.masjidapp.repository;

import com.masjidapp.entity.PrayerTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PrayerTimeRepository extends JpaRepository<PrayerTime, UUID> {

    /**
     * Find prayer time entry by specific date
     */
    Optional<PrayerTime> findByDate(LocalDate date);

    /**
     * Check if prayer time exists for a specific date
     */
    boolean existsByDate(LocalDate date);

    /**
     * Find prayer times within a date range, ordered by date ascending (paginated)
     */
    @Query("SELECT pt FROM PrayerTime pt WHERE pt.date >= :startDate AND pt.date <= :endDate ORDER BY pt.date ASC")
    Page<PrayerTime> findByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);

    /**
     * Find prayer times within a date range, ordered by date ascending (non-paginated).
     * Used by member APIs for week/month views.
     */
    List<PrayerTime> findByDateBetweenOrderByDateAsc(LocalDate start, LocalDate end);

    /**
     * Find all prayer times ordered by date descending (paginated)
     */
    Page<PrayerTime> findAllByOrderByDateDesc(Pageable pageable);

}
