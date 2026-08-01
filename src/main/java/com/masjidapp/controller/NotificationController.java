package com.masjidapp.controller;

import com.masjidapp.dto.response.ApiResponse;
import com.masjidapp.dto.response.NotificationItemResponse;
import com.masjidapp.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notifications", description = "Endpoints for fetching notification feeds")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Get Notifications", description = "Fetch notification history for sent announcements and events.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationItemResponse>>> getNotifications() {
        log.info("Received request to get notifications");
        List<NotificationItemResponse> notifications = notificationService.getNotifications();
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }
}
