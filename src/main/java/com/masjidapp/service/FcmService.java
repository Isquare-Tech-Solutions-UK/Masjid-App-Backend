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
}
