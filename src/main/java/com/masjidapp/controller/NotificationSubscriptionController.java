package com.masjidapp.controller;

import com.masjidapp.dto.request.SubscribeRequest;
import com.masjidapp.dto.request.UnsubscribeRequest;
import com.masjidapp.dto.response.ApiResponse;
import com.masjidapp.service.FcmService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * FCM topic subscription endpoints, used by an FCM receiver (e.g. Masjid-Notify-Web)
 * to join/leave topics such as "prayer-updates" that {@code FcmService#sendToTopic}/
 * {@code sendPrayerUpdate} publish to.
 *
 * TODO (PRODUCTION SECURITY): These endpoints are intentionally left public — see the matching
 * TODO in SecurityConfig — ONLY to support development/testing of a browser-based FCM receiver.
 * Before production, do at least one of the following, and do not allow arbitrary topic
 * subscriptions in production:
 *   1. Require authentication (JWT and/or API key) on these endpoints, and/or
 *   2. Validate "topic" against an allow-list (e.g. "prayer-updates") instead of
 *      accepting any client-supplied topic string, and/or
 *   3. Protect with a dedicated API key or other security mechanism.
 */
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notification Subscriptions", description = "Endpoints for subscribing/unsubscribing FCM tokens to topics (development/testing)")
public class NotificationSubscriptionController {

    private final FcmService fcmService;

    /**
     * POST /notifications/subscribe
     * Subscribe an FCM registration token to a topic.
     */
    @Operation(summary = "Subscribe to Topic",
            description = "Subscribe an FCM registration token to a topic so the owning device receives future notifications for it.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Token subscribed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or subscription failure",
                    content = @Content(schema = @Schema(implementation = com.masjidapp.exception.GlobalExceptionHandler.ErrorResponse.class)))
    })
    @PostMapping("/subscribe")
    public ResponseEntity<ApiResponse<Map<String, Object>>> subscribe(@Valid @RequestBody SubscribeRequest request) {
        log.info("Received request to subscribe token to topic. topic={}", request.getTopic());

        fcmService.subscribeToTopic(request.getToken(), request.getTopic());

        Map<String, Object> result = new HashMap<>();
        result.put("topic", request.getTopic());
        result.put("subscribed", true);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * POST /notifications/unsubscribe
     * Unsubscribe an FCM registration token from a topic.
     */
    @Operation(summary = "Unsubscribe from Topic",
            description = "Unsubscribe an FCM registration token from a topic.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Token unsubscribed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or unsubscription failure",
                    content = @Content(schema = @Schema(implementation = com.masjidapp.exception.GlobalExceptionHandler.ErrorResponse.class)))
    })
    @PostMapping("/unsubscribe")
    public ResponseEntity<ApiResponse<Map<String, Object>>> unsubscribe(@Valid @RequestBody UnsubscribeRequest request) {
        log.info("Received request to unsubscribe token from topic. topic={}", request.getTopic());

        fcmService.unsubscribeFromTopic(request.getToken(), request.getTopic());

        Map<String, Object> result = new HashMap<>();
        result.put("topic", request.getTopic());
        result.put("subscribed", false);

        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
