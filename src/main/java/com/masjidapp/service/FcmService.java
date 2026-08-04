package com.masjidapp.service;

import java.util.Map;

public interface FcmService {

    /**
     * Send a push notification to all devices subscribed to the given Firebase topic.
     *
     * @param topic Firebase topic name (e.g. "events", "announcements")
     * @param title notification title
     * @param body  notification body text
     * @param data  optional key-value data payload (e.g. type, id for deep linking)
     */
    void sendToTopic(String topic, String title, String body, Map<String, String> data);

    /**
     * Notify devices subscribed to the "prayer-updates" topic that prayer
     * timings have changed for the given month.
     *
     * @param updatedMonth human-readable month label, e.g. "August 2026"
     */
    void sendPrayerUpdate(String updatedMonth);

    /**
     * Subscribe an FCM registration token (mobile or web) to a topic so it
     * receives future {@link #sendToTopic} / {@link #sendPrayerUpdate} messages.
     *
     * @param token FCM registration token from the client
     * @param topic Firebase topic name (e.g. "prayer-updates")
     */
    void subscribeToTopic(String token, String topic);

    /**
     * Unsubscribe an FCM registration token from a topic.
     *
     * @param token FCM registration token supplied by the client (browser/mobile)
     * @param topic Firebase topic name (e.g. "prayer-updates")
     */
    void unsubscribeFromTopic(String token, String topic);
}
