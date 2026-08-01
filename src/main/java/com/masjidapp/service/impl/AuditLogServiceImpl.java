package com.masjidapp.service.impl;

import com.masjidapp.entity.AdminUser;
import com.masjidapp.entity.AuditLog;
import com.masjidapp.entity.Event;
import com.masjidapp.repository.AuditLogRepository;
import com.masjidapp.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional
    public void logEventUpdate(AdminUser user,
                               Event oldEvent,
                               Event newEvent,
                               String ipAddress,
                               String userAgent) {

        Map<String, Object> oldValues = toEventMap(oldEvent);
        Map<String, Object> newValues = toEventMap(newEvent);

        AuditLog logEntry = AuditLog.builder()
                .user(user)
                .action("UPDATE")
                .entityType("Event")
                .entityId(newEvent.getId())
                .oldValues(oldValues)
                .newValues(newValues)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

        auditLogRepository.save(logEntry);

        log.info("Audit log created for event update. eventId={}, userId={}",
                newEvent.getId(),
                user != null ? user.getId() : null);
    }

    private Map<String, Object> toEventMap(Event event) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", event.getId());
        map.put("title", event.getTitle());
        map.put("speaker", event.getSpeaker());
        map.put("date", event.getDate());
        map.put("link", event.getLink());
        map.put("images", event.getImages());
        map.put("description", event.getDescription());
        map.put("venue", event.getVenue());
        map.put("status", event.getStatus() != null ? event.getStatus().name() : null);
        map.put("publishedAt", event.getPublishedAt());
        map.put("createdAt", event.getCreatedAt());
        map.put("updatedAt", event.getUpdatedAt());
        return map;
    }
}


