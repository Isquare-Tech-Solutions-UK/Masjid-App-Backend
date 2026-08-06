package com.masjidapp.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.masjidapp.dto.request.BulkPrayerTimeRequest;
import com.masjidapp.dto.request.CreatePrayerTimeRequest;
import com.masjidapp.dto.request.UpdatePrayerTimeRequest;
import com.masjidapp.dto.response.BulkPrayerTimeResult;
import com.masjidapp.dto.response.MemberPrayerTimeResponse;
import com.masjidapp.dto.response.NextPrayerInfo;
import com.masjidapp.dto.response.PrayerTimeResponse;
import com.masjidapp.entity.PrayerTime;
import com.masjidapp.exception.ResourceNotFoundException;
import com.masjidapp.repository.PrayerTimeRepository;
import com.masjidapp.service.FcmService;
import com.masjidapp.service.PrayerTimeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PrayerTimeServiceImpl implements PrayerTimeService {

    private final PrayerTimeRepository prayerTimeRepository;
    private final FcmService fcmService;

    /**
     * Ordered list of prayer names to iterate when computing nextPrayer.
     * "sunrise" is excluded because it has no jamah time.
     */
    private static final String[] PRAYER_ORDER = {"fajr", "sunrise", "zuhr", "asr", "maghrib", "isha"};

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    // ============================================
    // Admin methods
    // ============================================

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

        notifyPrayerUpdate(YearMonth.from(saved.getDate()));

        return PrayerTimeResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public BulkPrayerTimeResult bulkCreatePrayerTimes(BulkPrayerTimeRequest request) {
        log.debug("Bulk creating/updating {} prayer times", request.getPrayerTimes().size());

        int created = 0;
        int updated = 0;
        int failed = 0;
        LocalDate firstSavedDate = null;

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

                if (firstSavedDate == null) {
                    firstSavedDate = date;
                }
            } catch (Exception e) {
                log.warn("Failed to process prayer time entry for date: {} — {}", entry.getDate(), e.getMessage());
                failed++;
            }
        }

        log.info("Bulk prayer time result — created: {}, updated: {}, failed: {}", created, updated, failed);

        // Send a single notification for the whole bulk operation, not one per record
        if (firstSavedDate != null) {
            notifyPrayerUpdate(YearMonth.from(firstSavedDate));
        }

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

        notifyPrayerUpdate(YearMonth.from(saved.getDate()));

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

    // ============================================
    // Member methods
    // ============================================

    @Override
    @Transactional(readOnly = true)
    public MemberPrayerTimeResponse getTodayPrayerTimes() {
        LocalDate today = LocalDate.now();
        log.debug("Fetching today's prayer times for date: {}", today);

        PrayerTime prayerTime = prayerTimeRepository.findByDate(today)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Prayer times not found for today: " + today));

        MemberPrayerTimeResponse response = MemberPrayerTimeResponse.fromEntity(prayerTime);
        response.setNextPrayer(computeNextPrayer(prayerTime.getPrayers()));

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberPrayerTimeResponse> getWeekPrayerTimes() {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        log.debug("Fetching weekly prayer times: {} to {}", weekStart, weekEnd);

        List<PrayerTime> prayerTimes = prayerTimeRepository
                .findByDateBetweenOrderByDateAsc(weekStart, weekEnd);

        return prayerTimes.stream()
                .map(MemberPrayerTimeResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberPrayerTimeResponse> getMonthPrayerTimes(Integer month, Integer year) {
        LocalDate today = LocalDate.now();
        int targetMonth = (month != null) ? month : today.getMonthValue();
        int targetYear = (year != null) ? year : today.getYear();

        YearMonth yearMonth = YearMonth.of(targetYear, targetMonth);
        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();

        log.debug("Fetching monthly prayer times: {} to {}", monthStart, monthEnd);

        List<PrayerTime> prayerTimes = prayerTimeRepository
                .findByDateBetweenOrderByDateAsc(monthStart, monthEnd);

        return prayerTimes.stream()
                .map(MemberPrayerTimeResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // ============================================
    // Private helpers
    // ============================================

    /**
     * Send a push notification for a prayer time create/update to the "prayer-updates" topic.
     * Mirrors the notify pattern used by EventServiceImpl / AnnouncementServiceImpl.
     */
    private void notifyPrayerUpdate(YearMonth yearMonth) {
        try {
            String updatedMonth = yearMonth.toString();

            Map<String, String> data = new HashMap<>();
            data.put("isUpdate", "true");
            data.put("updatedMonth", updatedMonth);

            String body = "Prayer times for " + updatedMonth + " have been updated.";
            fcmService.sendToTopic("prayer-updates", "Prayer Times Updated", body, data);

            log.info("Prayer time notification sent via topic. updatedMonth={}", updatedMonth);
        } catch (Exception e) {
            log.error("Failed to send prayer time notification. updatedMonth={}, error={}", yearMonth, e.getMessage(), e);
        }
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

    /**
     * Compute the next upcoming prayer based on current time.
     * Returns null if all prayers for today have passed.
     */
    private NextPrayerInfo computeNextPrayer(JsonNode prayers) {
        if (prayers == null) {
            return null;
        }

        LocalTime now = LocalTime.now();

        for (String prayerName : PRAYER_ORDER) {
            JsonNode prayerNode = prayers.get(prayerName);
            if (prayerNode == null) {
                continue;
            }

            JsonNode athanNode = prayerNode.get("athan");
            if (athanNode == null || athanNode.isNull()) {
                continue;
            }

            try {
                LocalTime athanTime = LocalTime.parse(athanNode.asText(), TIME_FORMAT);

                if (athanTime.isAfter(now)) {
                    // This is the next prayer
                    String jamah = null;
                    JsonNode jamahNode = prayerNode.get("jamah");
                    if (jamahNode != null && !jamahNode.isNull()) {
                        jamah = jamahNode.asText();
                    }

                    return NextPrayerInfo.builder()
                            .name(prayerName)
                            .athan(athanNode.asText())
                            .jamah(jamah)
                            .timeUntil(formatTimeUntil(now, athanTime))
                            .build();
                }
            } catch (DateTimeParseException e) {
                log.warn("Failed to parse athan time for prayer '{}': {}", prayerName, athanNode.asText());
            }
        }

        // All prayers have passed for today
        return null;
    }

    /**
     * Format the duration between now and a target time as "Xh Ym".
     */
    private String formatTimeUntil(LocalTime now, LocalTime target) {
        long totalMinutes = java.time.Duration.between(now, target).toMinutes();
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;

        if (hours > 0 && minutes > 0) {
            return hours + "h " + minutes + "m";
        } else if (hours > 0) {
            return hours + "h";
        } else {
            return minutes + "m";
        }
    }

}

