package com.masjidapp.service;

import com.masjidapp.dto.request.DonationCauseCreateRequest;
import com.masjidapp.dto.response.DonationCauseListResponse;

public interface DonationCauseService {

    DonationCauseListResponse getAllCauses();

    DonationCauseListResponse createCause(DonationCauseCreateRequest request);

    DonationCauseListResponse updateCause(String oldCause, String newCause);

    DonationCauseListResponse deleteCause(String cause);
}