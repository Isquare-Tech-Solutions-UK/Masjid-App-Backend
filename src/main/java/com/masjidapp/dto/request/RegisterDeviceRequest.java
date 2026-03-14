package com.masjidapp.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterDeviceRequest {

    @NotBlank(message = "FCM token is required")
    private String fcmToken;

    @NotBlank(message = "Platform is required")
    @Pattern(regexp = "^(ios|android)$", message = "Platform must be 'ios' or 'android'")
    private String platform;
}
