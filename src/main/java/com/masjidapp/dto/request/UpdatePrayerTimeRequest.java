package com.masjidapp.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePrayerTimeRequest {

    private String hijriDate;

    private JsonNode prayers;

    private JsonNode jumuahTimes;

}
