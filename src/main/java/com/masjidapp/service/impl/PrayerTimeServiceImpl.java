package com.masjidapp.service.impl;

import com.masjidapp.dto.request.BulkPrayerTimeRequest;
import com.masjidapp.dto.request.CreatePrayerTimeRequest;
import com.masjidapp.dto.request.UpdatePrayerTimeRequest;
import com.masjidapp.dto.response.BulkPrayerTimeResult;
import com.masjidapp.dto.response.PrayerTimeResponse;
import com.masjidapp.entity.PrayerTime;
import com.masjidapp.exception.ResourceNotFoundException;
import com.masjidapp.repository.PrayerTimeRepository;
import com.masjidapp.service.PrayerTimeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PrayerTimeServiceImpl implements PrayerTimeService {

    private final PrayerTimeRepository prayerTimeRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<PrayerTimeResponse> getPrayerTimes(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        log.debug("Fetching prayer times - startDate: {}, endDate: {}", startDate, endDate);

        Page<PrayerTime> page;

        if (startDate != null && endDate != null) {
            page = prayerTimeRepository.findByDateRange(startDate, endDate, pageable);
        } else {
            page = prayerTimeRepository.findAllByOrderByDateDesc(pageable);
        }

        return page.map(PrayerTimeResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public PrayerTimeResponse getPrayerTimeById(UUID id) {
        log.debug("Fetching prayer time by ID: {}", id);

        PrayerTime prayerTime = prayerTimeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prayer time not found with id: " + id));

        return PrayerTimeResponse.fromEntity(prayerTime);
    }

    @Override
    @Transactional
    public PrayerTimeResponse createPrayerTime(CreatePrayerTimeRequest request) {
        LocalDate date = parseDate(request.getDate());
        log.debug("Creating prayer time for date: {}", date);

        // Check if entry already exists for this date
        if (prayerTimeRepository.existsByDate(date)) {
            throw new IllegalArgumentException("Prayer time already exists for date: " + date);
        }

        PrayerTime prayerTime = PrayerTime.builder()
                .date(date)
                .hijriDate(request.getHijriDate())
                .prayers(request.getPrayers())
                .jumuahTimes(request.getJumuahTimes())
                .build();

        PrayerTime saved = prayerTimeRepository.save(prayerTime);
        log.info("Created prayer time for date: {}", date);

        return PrayerTimeResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public BulkPrayerTimeResult bulkCreatePrayerTimes(BulkPrayerTimeRequest request) {
        log.debug("Bulk creating/updating {} prayer times", request.getPrayerTimes().size());

        int created = 0;
        int updated = 0;
        int failed = 0;

        for (CreatePrayerTimeRequest entry : request.getPrayerTimes()) {
            try {
                LocalDate date = parseDate(entry.getDate());
                Optional<PrayerTime> existing = prayerTimeRepository.findByDate(date);

                if (existing.isPresent()) {
                    // Update existing entry
                    PrayerTime prayerTime = existing.get();
                    prayerTime.setHijriDate(entry.getHijriDate());
                    prayerTime.setPrayers(entry.getPrayers());
                    prayerTime.setJumuahTimes(entry.getJumuahTimes());
                    prayerTimeRepository.save(prayerTime);
                    updated++;
                } else {
                    // Create new entry
                    PrayerTime prayerTime = PrayerTime.builder()
                            .date(date)
                            .hijriDate(entry.getHijriDate())
                            .prayers(entry.getPrayers())
                            .jumuahTimes(entry.getJumuahTimes())
                            .build();
                    prayerTimeRepository.save(prayerTime);
                    created++;
                }
            } catch (Exception e) {
                log.warn("Failed to process prayer time entry for date: {} — {}", entry.getDate(), e.getMessage());
                failed++;
            }
        }

        log.info("Bulk prayer time result — created: {}, updated: {}, failed: {}", created, updated, failed);

        return BulkPrayerTimeResult.builder()
                .created(created)
                .updated(updated)
                .failed(failed)
                .build();
    }

    @Override
    @Transactional
    public PrayerTimeResponse updatePrayerTime(UUID id, UpdatePrayerTimeRequest request) {
        log.debug("Updating prayer time with ID: {}", id);

        PrayerTime prayerTime = prayerTimeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prayer time not found with id: " + id));

        // Update only provided fields
        if (request.getHijriDate() != null) {
            prayerTime.setHijriDate(request.getHijriDate());
        }
        if (request.getPrayers() != null) {
            prayerTime.setPrayers(request.getPrayers());
        }
        if (request.getJumuahTimes() != null) {
            prayerTime.setJumuahTimes(request.getJumuahTimes());
        }

        PrayerTime saved = prayerTimeRepository.save(prayerTime);
        log.info("Updated prayer time for date: {}", saved.getDate());

        return PrayerTimeResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public void deletePrayerTime(UUID id) {
        log.debug("Deleting prayer time with ID: {}", id);

        if (!prayerTimeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Prayer time not found with id: " + id);
        }

        prayerTimeRepository.deleteById(id);
        log.info("Deleted prayer time with ID: {}", id);
    }

    /**
     * Parse date string to LocalDate
     */
    private LocalDate parseDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format: " + dateStr + ". Expected YYYY-MM-DD");
        }
    }

}
