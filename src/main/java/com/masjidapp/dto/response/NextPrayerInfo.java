package com.masjidapp.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing the next upcoming prayer.
 * Included in the /member/prayer-times/today response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NextPrayerInfo {

    @JsonProperty("name")
    private String name;

    @JsonProperty("athan")
    private String athan;

    @JsonProperty("jamah")
    private String jamah;

    @JsonProperty("timeUntil")
    private String timeUntil;

}
