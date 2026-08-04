package com.masjidapp.service.impl;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.TopicManagementResponse;
import com.masjidapp.exception.MARequestException;
import com.masjidapp.service.FcmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class FcmServiceImpl implements FcmService {

    private static final String PRAYER_UPDATES_TOPIC = "prayer-updates";

    @Override
    public void sendPrayerUpdate(String updatedMonth) {
        String title = "Prayer Timings Updated";
        String body = "Prayer timings have been updated for " + updatedMonth + ".";

        Map<String, String> data = new HashMap<>();
        data.put("isUpdate", "true");
        data.put("updatedMonth", updatedMonth);

        sendToTopic(PRAYER_UPDATES_TOPIC, title, body, data);
    }

    @Override
    public void sendToTopic(String topic, String title, String body, Map<String, String> data) {
        if (!isFirebaseInitialized()) {
            log.warn("FCM send skipped: Firebase is not initialized.");
            return;
        }

        log.info("FCM topic send initiated: topic='{}', title='{}'", topic, title);

        Message.Builder messageBuilder = Message.builder()
                .setTopic(topic)
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
                        .build());

        if (data != null && !data.isEmpty()) {
            messageBuilder.putAllData(data);
        }

        try {
            String messageId = FirebaseMessaging.getInstance().send(messageBuilder.build());
            log.info("FCM topic message sent successfully: topic='{}', messageId='{}'", topic, messageId);
        } catch (Exception e) {
            log.error("FCM topic send failed: topic='{}', error={}", topic, e.getMessage(), e);
        }
    }

    @Override
    public void subscribeToTopic(String token, String topic) {
        manageTopicSubscription(token, topic, true);
    }

    @Override
    public void unsubscribeFromTopic(String token, String topic) {
        manageTopicSubscription(token, topic, false);
    }

    private void manageTopicSubscription(String token, String topic, boolean subscribe) {
        String action = subscribe ? "subscribe" : "unsubscribe";
        String maskedToken = maskToken(token);

        if (!isFirebaseInitialized()) {
            log.warn("FCM topic {} skipped: Firebase is not initialized. topic='{}', token='{}'",
                    action, topic, maskedToken);
            throw new MARequestException("Firebase is not initialized; cannot manage topic subscriptions.");
        }

        log.info("FCM topic {} initiated: topic='{}', token='{}'", action, topic, maskedToken);

        try {
            TopicManagementResponse response = subscribe
                    ? FirebaseMessaging.getInstance().subscribeToTopic(List.of(token), topic)
                    : FirebaseMessaging.getInstance().unsubscribeFromTopic(List.of(token), topic);

            log.info("FCM topic {} completed: topic='{}', token='{}', successCount={}, failureCount={}",
                    action, topic, maskedToken, response.getSuccessCount(), response.getFailureCount());

            if (response.getFailureCount() > 0) {
                response.getErrors().forEach(error -> log.error(
                        "FCM topic {} error: topic='{}', token='{}', index={}, reason={}",
                        action, topic, maskedToken, error.getIndex(), error.getReason()));

                throw new MARequestException(String.format(
                        "Failed to %s token to topic '%s': %s",
                        action, topic, response.getErrors().get(0).getReason()));
            }
        } catch (FirebaseMessagingException e) {
            log.error("FCM topic {} failed: topic='{}', token='{}', error={}",
                    action, topic, maskedToken, e.getMessage(), e);
            throw new MARequestException(
                    String.format("Failed to %s token to topic '%s'", action, topic), e);
        }
    }

    /**
     * Masks all but the first 6 and last 4 characters of a token before logging,
     * so full FCM registration tokens never appear in application logs.
     */
    private String maskToken(String token) {
        if (token == null || token.length() <= 10) {
            return "****";
        }
        return token.substring(0, 6) + "..." + token.substring(token.length() - 4);
    }

    private boolean isFirebaseInitialized() {
        return !FirebaseApp.getApps().isEmpty();
    }
}
