package com.masjidapp.controller.admin;

import com.masjidapp.dto.request.CreateEventRequest;
import com.masjidapp.dto.response.ApiResponse;
import com.masjidapp.dto.response.EventResponse;
import com.masjidapp.entity.AdminUser;
import com.masjidapp.repository.AdminUserRepository;
import com.masjidapp.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/admin/events")
@RequiredArgsConstructor
@Slf4j
public class AdminEventController {

    private final EventService eventService;
    private final AdminUserRepository adminUserRepository;

    /**
     * POST /admin/events
     * Create a new event with optional image uploads.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<EventResponse>> createEvent(
            @Valid @ModelAttribute CreateEventRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            log.warn("Unauthorized attempt to create event. No authenticated user found.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        AdminUser adminUser = adminUserRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> {
                    log.error("Authenticated user not found in database. email={}", userDetails.getUsername());
                    return new IllegalStateException("Authenticated user not found");
                });

        log.info("Received create event request. title={}, speaker={}, adminEmail={}",
                request.getTitle(), request.getSpeaker(), adminUser.getEmail());

        EventResponse eventResponse = eventService.createEvent(request, images, adminUser);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(eventResponse));
    }
}


