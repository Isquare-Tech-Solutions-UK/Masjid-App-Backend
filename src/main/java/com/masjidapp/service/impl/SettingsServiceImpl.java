package com.masjidapp.service.impl;

import com.masjidapp.dto.request.UpdateMasjidSettingsRequest;
import com.masjidapp.dto.request.UpdatePaymentSettingsRequest;
import com.masjidapp.dto.response.MasjidSettingsResponse;
import com.masjidapp.dto.response.MemberMasjidInfoResponse;
import com.masjidapp.entity.MasjidSettings;
import com.masjidapp.exception.ResourceNotFoundException;
import com.masjidapp.repository.SettingsRepository;
import com.masjidapp.service.SettingsService;
import com.masjidapp.service.StripeKeyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettingsServiceImpl implements SettingsService {

    private final SettingsRepository settingsRepository;
    private final StripeKeyService stripeKeyService;

    private MasjidSettings getSettingsEntity() {
        return settingsRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Masjid settings not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public MasjidSettingsResponse getSettings() {
        MasjidSettings settings = getSettingsEntity();
        log.info("Retrieved masjid settings: id={}", settings.getId());
        MasjidSettingsResponse response = MasjidSettingsResponse.fromEntity(settings);
        response.setStripe(stripeKeyService.getStatus());
        return response;
    }

    @Override
    @Transactional
    public MasjidSettingsResponse updateSettings(UpdateMasjidSettingsRequest request) {
        MasjidSettings settings = getSettingsEntity();

        settings.setName(request.getName());
        settings.setAbout(request.getAbout());

        if (request.getAddress() != null) settings.setAddress(request.getAddress());
        if (request.getContact() != null) settings.setContact(request.getContact());
        if (request.getLocation() != null) settings.setLocation(request.getLocation());
        if (request.getCapacity() != null) settings.setCapacity(request.getCapacity());
        if (request.getServices() != null) settings.setServices(request.getServices());
        if (request.getFacilities() != null) settings.setFacilities(request.getFacilities());

        MasjidSettings saved = settingsRepository.save(settings);
        log.info("Updated masjid settings: id={}", saved.getId());
        return MasjidSettingsResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public MasjidSettingsResponse updatePaymentSettings(UpdatePaymentSettingsRequest request) {
        MasjidSettings settings = getSettingsEntity();

        MasjidSettings.Payment payment = settings.getPayment();
        if (payment == null) {
            payment = new MasjidSettings.Payment();
        }

        payment.setBankAccountName(request.getBankAccountName());
        payment.setBankName(request.getBankName());
        payment.setBankAccountNumber(request.getBankAccountNumber());
        payment.setBankSortCode(request.getBankSortCode());

        settings.setPayment(payment);

        MasjidSettings saved = settingsRepository.save(settings);
        log.info("Updated payment settings: id={}", saved.getId());
        return MasjidSettingsResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public MemberMasjidInfoResponse getMasjidInfo() {
        MasjidSettings settings = getSettingsEntity();
        log.info("Retrieved masjid info for member API: id={}", settings.getId());
        return MemberMasjidInfoResponse.fromEntity(settings);
    }
}
