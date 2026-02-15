package com.masjidapp.service;

import com.masjidapp.dto.request.BulkPrayerTimeRequest;
import com.masjidapp.dto.request.CreatePrayerTimeRequest;
import com.masjidapp.dto.request.UpdatePrayerTimeRequest;
import com.masjidapp.dto.response.BulkPrayerTimeResult;
import com.masjidapp.dto.response.PrayerTimeResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.UUID;

public interface PrayerTimeService {

    /**
     * Get paginated prayer times with optional date range filter
     */
    Page<PrayerTimeResponse> getPrayerTimes(LocalDate startDate, LocalDate endDate, Pageable pageable);

    /**
     * Get a single prayer time entry by ID
     */
    PrayerTimeResponse getPrayerTimeById(UUID id);

    /**
     * Create a new prayer time entry
     */
    PrayerTimeResponse createPrayerTime(CreatePrayerTimeRequest request);

    /**
     * Bulk create or update prayer times (upsert by date)
     */
    BulkPrayerTimeResult bulkCreatePrayerTimes(BulkPrayerTimeRequest request);

    /**
     * Update an existing prayer time entry
     */
    PrayerTimeResponse updatePrayerTime(UUID id, UpdatePrayerTimeRequest request);

    /**
     * Delete a prayer time entry
     */
    void deletePrayerTime(UUID id);

}
