package com.masjidapp.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request object for subscribing an FCM registration token to a topic")
public class SubscribeRequest {

    @Schema(description = "FCM registration token from the client (browser/mobile)", example = "dQw4w9WgXcQ:APA91bF...")
    @NotBlank(message = "Token is required")
    private String token;

    @Schema(description = "Firebase topic name to subscribe to", example = "prayer-updates")
    @NotBlank(message = "Topic is required")
    private String topic;
}
