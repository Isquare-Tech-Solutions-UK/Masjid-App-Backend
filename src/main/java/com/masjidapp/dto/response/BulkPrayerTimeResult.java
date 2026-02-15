package com.masjidapp.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkPrayerTimeResult {

    @JsonProperty("created")
    private int created;

    @JsonProperty("updated")
    private int updated;

    @JsonProperty("failed")
    private int failed;

}
