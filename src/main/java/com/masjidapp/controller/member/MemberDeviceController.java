package com.masjidapp.controller.member;

import com.masjidapp.dto.request.RegisterDeviceRequest;
import com.masjidapp.dto.response.ApiResponse;
import com.masjidapp.dto.response.MessageResponse;
import com.masjidapp.entity.DeviceToken;
import com.masjidapp.repository.DeviceTokenRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/member/devices")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Member Devices", description = "Register and manage device tokens for push notifications")
public class MemberDeviceController {

    private final DeviceTokenRepository deviceTokenRepository;

    /**
     * POST /member/devices/register
     * Called by the Flutter app on launch to register the FCM token.
     * Safe to call repeatedly — upserts by token.
     */
    @Operation(summary = "Register Device Token",
            description = "Register an FCM token for push notifications. Call on app launch. Safe to call multiple times.")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<MessageResponse>> registerDevice(
            @Valid @RequestBody RegisterDeviceRequest request) {

        log.info("Device registration request. platform={}, token={}...",
                request.getPlatform(), request.getFcmToken().substring(0, Math.min(20, request.getFcmToken().length())));

        deviceTokenRepository.findByFcmToken(request.getFcmToken())
                .ifPresentOrElse(
                        existing -> {
                            existing.setActive(true);
                            existing.setPlatform(request.getPlatform());
                            deviceTokenRepository.save(existing);
                            log.info("Device token reactivated. platform={}", request.getPlatform());
                        },
                        () -> {
                            DeviceToken token = DeviceToken.builder()
                                    .fcmToken(request.getFcmToken())
                                    .platform(request.getPlatform())
                                    .active(true)
                                    .build();
                            deviceTokenRepository.save(token);
                            log.info("New device token registered. platform={}", request.getPlatform());
                        });

        return ResponseEntity.ok(ApiResponse.success(new MessageResponse("Device registered successfully.")));
    }

    /**
     * DELETE /member/devices/{token}
     * Called by the Flutter app on logout to stop receiving notifications.
     */
    @Operation(summary = "Unregister Device Token",
            description = "Deactivate a device token on logout. The device will stop receiving push notifications.")
    @DeleteMapping("/{token}")
    public ResponseEntity<ApiResponse<MessageResponse>> unregisterDevice(
            @PathVariable String token) {

        log.info("Device unregister request. token={}...", token.substring(0, Math.min(20, token.length())));

        deviceTokenRepository.findByFcmToken(token).ifPresent(existing -> {
            existing.setActive(false);
            deviceTokenRepository.save(existing);
            log.info("Device token deactivated.");
        });

        return ResponseEntity.ok(ApiResponse.success(new MessageResponse("Device unregistered successfully.")));
    }
}
