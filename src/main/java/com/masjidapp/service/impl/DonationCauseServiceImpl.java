package com.masjidapp.service.impl;

import com.masjidapp.dto.request.DonationCauseCreateRequest;
import com.masjidapp.dto.response.DonationCauseListResponse;
import com.masjidapp.entity.AppMetaData;
import com.masjidapp.repository.AppMetaDataRepository;
import com.masjidapp.service.DonationCauseService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DonationCauseServiceImpl implements DonationCauseService {

    private static final String CAMPAIGN_MODULE = "campaign_module";

    private final AppMetaDataRepository appMetaDataRepository;

    @Override
    @Transactional(readOnly = true)
    public DonationCauseListResponse getAllCauses() {
        List<String> causes = appMetaDataRepository.findByModuleName(CAMPAIGN_MODULE).stream()
                .map(AppMetaData::getValue)
                .collect(Collectors.toList());
        return DonationCauseListResponse.builder().cause(causes).build();
    }

    @Override
    @Transactional
    public DonationCauseListResponse createCause(DonationCauseCreateRequest request) {
        String cause = request.getData().getCause().trim();

        if (appMetaDataRepository.existsByModuleNameIgnoreCaseAndValueIgnoreCase(CAMPAIGN_MODULE, cause)) {
            throw new IllegalArgumentException("Donation cause already exists: " + cause);
        }

        AppMetaData saved = appMetaDataRepository.save(
                AppMetaData.builder().moduleName(CAMPAIGN_MODULE).value(cause).build());
        log.info("Created donation cause: {}", saved.getValue());

        return getAllCauses();
    }

    @Override
    @Transactional
    public DonationCauseListResponse updateCause(String oldCause, String newCause) {
        String trimmedOld = oldCause.trim();
        String trimmedNew = newCause.trim();

        if (trimmedOld.equalsIgnoreCase(trimmedNew)) {
            return getAllCauses();
        }

        AppMetaData existing = appMetaDataRepository.findByModuleNameAndValue(CAMPAIGN_MODULE, trimmedOld)
                .orElseThrow(() -> new IllegalArgumentException("Donation cause not found: " + trimmedOld));

        if (appMetaDataRepository.existsByModuleNameIgnoreCaseAndValueIgnoreCase(CAMPAIGN_MODULE, trimmedNew)) {
            throw new IllegalArgumentException("Donation cause already exists: " + trimmedNew);
        }

        existing.setValue(trimmedNew);
        appMetaDataRepository.save(existing);
        log.info("Updated donation cause from '{}' to '{}'", trimmedOld, trimmedNew);

        return getAllCauses();
    }

    @Override
    @Transactional
    public DonationCauseListResponse deleteCause(String cause) {
        String trimmed = cause.trim();
        appMetaDataRepository.deleteByModuleNameAndValue(CAMPAIGN_MODULE, trimmed);
        log.info("Deleted donation cause: {}", trimmed);
        return getAllCauses();
    }
}
