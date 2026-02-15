package com.masjidapp.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePrayerTimeRequest {

    @NotBlank(message = "Date is required")
    private String date; // YYYY-MM-DD format

    private String hijriDate;

    @NotNull(message = "Prayers data is required")
    private JsonNode prayers;

    private JsonNode jumuahTimes;

}
