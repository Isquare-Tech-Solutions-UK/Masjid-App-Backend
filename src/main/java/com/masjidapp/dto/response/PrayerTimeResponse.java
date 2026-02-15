package com.masjidapp.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.masjidapp.entity.PrayerTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrayerTimeResponse {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("date")
    private LocalDate date;

    @JsonProperty("hijriDate")
    private String hijriDate;

    @JsonProperty("prayers")
    private JsonNode prayers;

    @JsonProperty("jumuahTimes")
    private JsonNode jumuahTimes;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt;

    /**
     * Convert PrayerTime entity to PrayerTimeResponse DTO
     */
    public static PrayerTimeResponse fromEntity(PrayerTime prayerTime) {
        return PrayerTimeResponse.builder()
                .id(prayerTime.getId())
                .date(prayerTime.getDate())
                .hijriDate(prayerTime.getHijriDate())
                .prayers(prayerTime.getPrayers())
                .jumuahTimes(prayerTime.getJumuahTimes())
                .createdAt(prayerTime.getCreatedAt())
                .updatedAt(prayerTime.getUpdatedAt())
                .build();
    }

}
