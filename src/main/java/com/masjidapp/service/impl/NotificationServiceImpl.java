package com.masjidapp.service.impl;

import com.masjidapp.dto.response.NotificationItemResponse;
import com.masjidapp.entity.Announcement;
import com.masjidapp.entity.Event;
import com.masjidapp.repository.AnnouncementRepository;
import com.masjidapp.repository.EventRepository;
import com.masjidapp.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final AnnouncementRepository announcementRepository;
    private final EventRepository eventRepository;

    @Override
    @Transactional(readOnly = true)
    public List<NotificationItemResponse> getNotifications() {
        log.debug("Fetching notifications dynamically from sent announcements and events");

        List<NotificationItemResponse> notifications = new ArrayList<>();

        // 1. Fetch announcements with notificationSent = true
        List<Announcement> announcements = announcementRepository.findByNotificationSentTrueOrderByNotificationSentAtDesc();
        for (Announcement ann : announcements) {
            LocalDateTime sentAt = ann.getNotificationSentAt() != null ? ann.getNotificationSentAt() : ann.getCreatedAt();
            notifications.add(NotificationItemResponse.builder()
                    .id(ann.getId())
                    .type("announcement_published")
                    .title(ann.getTitle())
                    .message(ann.getMessage())
                    .isRead(false)
                    .createdAt(sentAt)
                    .build());
        }

        // 2. Fetch events with notificationSent = true
        List<Event> events = eventRepository.findByNotificationSentTrueOrderByNotificationSentAtDesc();
        for (Event event : events) {
            LocalDateTime sentAt = event.getNotificationSentAt() != null ? event.getNotificationSentAt() : event.getCreatedAt();
            notifications.add(NotificationItemResponse.builder()
                    .id(event.getId())
                    .type("event_published")
                    .title(event.getTitle())
                    .message(event.getDescription())
                    .isRead(false)
                    .createdAt(sentAt)
                    .build());
        }

        // 3. Sort all notifications by createdAt descending
        notifications.sort(Comparator.comparing(NotificationItemResponse::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));

        return notifications;
    }
}
