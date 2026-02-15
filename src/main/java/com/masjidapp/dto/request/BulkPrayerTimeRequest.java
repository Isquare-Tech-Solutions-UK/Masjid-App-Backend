package com.masjidapp.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkPrayerTimeRequest {

    @NotEmpty(message = "Prayer times list cannot be empty")
    @Valid
    private List<CreatePrayerTimeRequest> prayerTimes;

}
