package com.masjidapp.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.masjidapp.entity.PrayerTime;

import java.time.DayOfWeek;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Member-facing prayer time response DTO.
 * Differs from the admin PrayerTimeResponse:
 * - Includes 'dayName' (for weekly view)
 * - Includes 'nextPrayer' (for today view)
 * - Excludes admin-only fields like 'id', 'createdAt', 'updatedAt'
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MemberPrayerTimeResponse {

    @JsonProperty("date")
    private LocalDate date;

    @JsonProperty("dayName")
    private String dayName;

    @JsonProperty("hijriDate")
    private String hijriDate;

    @JsonProperty("hijriDay")
    private String hijriDay;

    @JsonProperty("prayers")
    private JsonNode prayers;

    @JsonProperty("jumuahTimes")
    private JsonNode jumuahTimes;

    @JsonProperty("nextPrayer")
    private NextPrayerInfo nextPrayer;

    /**
     * Convert PrayerTime entity to member response DTO (without nextPrayer).
     */
    public static MemberPrayerTimeResponse fromEntity(PrayerTime prayerTime) {
        boolean isFriday = prayerTime.getDate().getDayOfWeek() == DayOfWeek.FRIDAY;

        String rawHijri = prayerTime.getHijriDate();
        String hijriDateVal = rawHijri;
        String hijriDayVal = rawHijri;

        if (rawHijri != null && rawHijri.contains(" ")) {
            hijriDateVal = rawHijri.split(" ")[0];
            hijriDayVal = rawHijri;
        }

        return MemberPrayerTimeResponse.builder()
                .date(prayerTime.getDate())
                .dayName(prayerTime.getDate()
                        .getDayOfWeek()
                        .getDisplayName(TextStyle.FULL, Locale.ENGLISH))
                .hijriDate(hijriDateVal)
                .hijriDay(hijriDayVal)
                .prayers(prayerTime.getPrayers())
                .jumuahTimes(isFriday ? prayerTime.getJumuahTimes() : null)
                .build();
    }

}

