package com.masjidapp.service.impl;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import com.masjidapp.entity.DeviceToken;
import com.masjidapp.repository.DeviceTokenRepository;
import com.masjidapp.service.FcmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FcmServiceImpl implements FcmService {

    // FCM multicast limit per request
    private static final int FCM_BATCH_SIZE = 500;

    private final DeviceTokenRepository deviceTokenRepository;

    @Override
    @Transactional
    public int sendToAll(String title, String body, Map<String, String> data) {
        if (!isFirebaseInitialized()) {
            log.warn("FCM send skipped: Firebase is not initialized.");
            return 0;
        }

        List<DeviceToken> activeTokens = deviceTokenRepository.findAllByActiveTrue();
        if (activeTokens.isEmpty()) {
            log.info("FCM send skipped: No active device tokens registered.");
            return 0;
        }

        log.info("FCM Multicast Initiated: title='{}', totalDevices={}", title, activeTokens.size());

        List<String> tokenStrings = activeTokens.stream()
                .map(DeviceToken::getFcmToken)
                .collect(Collectors.toList());

        int totalSuccess = 0;
        List<String> invalidTokens = new ArrayList<>();

        // Send in batches of 500 (FCM limit)
        for (int i = 0; i < tokenStrings.size(); i += FCM_BATCH_SIZE) {
            List<String> batch = tokenStrings.subList(i, Math.min(i + FCM_BATCH_SIZE, tokenStrings.size()));
            BatchResult result = sendBatch(batch, title, body, data);
            totalSuccess += result.successCount();
            invalidTokens.addAll(result.invalidTokens());
        }

        // Deactivate invalid tokens in DB
        if (!invalidTokens.isEmpty()) {
            log.info("FCM Deactivating {} invalid/expired tokens.", invalidTokens.size());
            deviceTokenRepository.deactivateTokens(invalidTokens);
        }

        log.info("FCM Multicast Completed: successCount={}, failedTokens={}",
                totalSuccess, invalidTokens.size());

        return totalSuccess;
    }

    private BatchResult sendBatch(List<String> tokens, String title, String body, Map<String, String> data) {
        MulticastMessage.Builder messageBuilder = MulticastMessage.builder()
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .setNotification(AndroidNotification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .setSound("default")
                                .build())
                        .build())
                .setApnsConfig(ApnsConfig.builder()
                        .setAps(Aps.builder()
                                .setSound("default")
                                .setBadge(1)
                                .build())
                        .build())
                .addAllTokens(tokens);

        if (data != null && !data.isEmpty()) {
            messageBuilder.putAllData(data);
        }

        try {
            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(messageBuilder.build());

            List<String> failedTokens = new ArrayList<>();
            List<SendResponse> responses = response.getResponses();

            for (int i = 0; i < responses.size(); i++) {
                SendResponse sendResponse = responses.get(i);
                if (!sendResponse.isSuccessful()) {
                    String errorCode = sendResponse.getException() != null
                            ? sendResponse.getException().getMessagingErrorCode() != null
                            ? sendResponse.getException().getMessagingErrorCode().name()
                            : "UNKNOWN"
                            : "UNKNOWN";

                    // Mark token as invalid if FCM says it's unregistered or invalid
                    if (isInvalidTokenError(errorCode)) {
                        failedTokens.add(tokens.get(i));
                    }

                    log.warn("FCM send failed for token index={}, error={}", i, errorCode);
                }
            }

            return new BatchResult(response.getSuccessCount(), failedTokens);

        } catch (Exception e) {
            log.error("FCM batch send failed: {}", e.getMessage(), e);
            return new BatchResult(0, List.of());
        }
    }

    private boolean isInvalidTokenError(String errorCode) {
        return "UNREGISTERED".equals(errorCode)
                || "INVALID_ARGUMENT".equals(errorCode)
                || "SENDER_ID_MISMATCH".equals(errorCode);
    }

    private boolean isFirebaseInitialized() {
        return !FirebaseApp.getApps().isEmpty();
    }

    private record BatchResult(int successCount, List<String> invalidTokens) {}
}
