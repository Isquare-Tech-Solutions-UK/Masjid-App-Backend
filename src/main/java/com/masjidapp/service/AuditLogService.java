package com.masjidapp.service;

import com.masjidapp.entity.AdminUser;
import com.masjidapp.entity.Event;

public interface AuditLogService {

    void logEventUpdate(AdminUser user,
                        Event oldEvent,
                        Event newEvent,
                        String ipAddress,
                        String userAgent);
}


