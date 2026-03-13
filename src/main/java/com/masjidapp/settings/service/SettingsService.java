package com.masjidapp.settings.service;

import com.masjidapp.settings.dto.MasjidSettingsResponse;
import com.masjidapp.settings.dto.UpdateMasjidSettingsRequest;
import com.masjidapp.settings.dto.UpdatePaymentSettingsRequest;

public interface SettingsService {

    MasjidSettingsResponse getSettings();

    MasjidSettingsResponse updateSettings(UpdateMasjidSettingsRequest request);

    MasjidSettingsResponse updatePaymentSettings(UpdatePaymentSettingsRequest request);
}
