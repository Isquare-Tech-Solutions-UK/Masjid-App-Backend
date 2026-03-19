package com.masjidapp.service;

import com.masjidapp.dto.request.UpdateMasjidSettingsRequest;
import com.masjidapp.dto.request.UpdatePaymentSettingsRequest;
import com.masjidapp.dto.response.MasjidSettingsResponse;
import com.masjidapp.dto.response.MemberMasjidInfoResponse;

public interface SettingsService {

    MasjidSettingsResponse getSettings();

    MasjidSettingsResponse updateSettings(UpdateMasjidSettingsRequest request);

    MasjidSettingsResponse updatePaymentSettings(UpdatePaymentSettingsRequest request);

    MemberMasjidInfoResponse getMasjidInfo();
}
