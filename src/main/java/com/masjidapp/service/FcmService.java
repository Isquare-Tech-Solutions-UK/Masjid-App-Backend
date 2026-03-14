package com.masjidapp.service;

import java.util.Map;

public interface FcmService {

    /**
     * Send a push notification to all active registered devices.
     *
     * @param title notification title
     * @param body  notification body text
     * @param data  optional key-value data payload (e.g. type, id for deep linking)
     * @return number of devices successfully notified
     */
    int sendToAll(String title, String body, Map<String, String> data);
}
