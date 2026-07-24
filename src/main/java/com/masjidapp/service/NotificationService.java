package com.masjidapp.service;

import com.masjidapp.dto.response.NotificationItemResponse;
import java.util.List;

public interface NotificationService {
    List<NotificationItemResponse> getNotifications();
}
