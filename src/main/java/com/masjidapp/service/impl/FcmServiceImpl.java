package com.masjidapp.service.impl;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.masjidapp.service.FcmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class FcmServiceImpl implements FcmService {

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

    private boolean isFirebaseInitialized() {
        return !FirebaseApp.getApps().isEmpty();
    }
}
