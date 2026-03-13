package com.masjidapp.settings.dto;

import com.masjidapp.settings.entity.MasjidSettings;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMasjidSettingsRequest {

    @NotBlank(message = "Masjid name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    private String about;

    private MasjidSettings.Address address;

    private MasjidSettings.Contact contact;

    private MasjidSettings.Location location;

    private MasjidSettings.Capacity capacity;

    private MasjidSettings.Services services;

    private MasjidSettings.Facilities facilities;
}
