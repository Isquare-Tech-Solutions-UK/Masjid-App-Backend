package com.masjidapp.service;

import com.masjidapp.dto.request.CreateEventRequest;
import com.masjidapp.dto.response.EventResponse;
import com.masjidapp.entity.AdminUser;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface EventService {

    /**
     * Create a new event with optional image uploads.
     */
    EventResponse createEvent(CreateEventRequest request, List<MultipartFile> images, AdminUser createdBy);
}


