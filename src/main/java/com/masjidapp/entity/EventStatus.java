package com.masjidapp.entity;

/**
 * Enum representing event status. Maps to PostgreSQL enum type 'event_status'.
 */
public enum EventStatus {
    draft,
    published,
    cancelled,
    completed
}


