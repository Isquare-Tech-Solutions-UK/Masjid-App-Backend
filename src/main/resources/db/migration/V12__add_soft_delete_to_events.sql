ALTER TABLE events
    ADD COLUMN is_deleted BOOLEAN DEFAULT false,
    ADD COLUMN deleted_at TIMESTAMP,
    ADD COLUMN deleted_by UUID;

ALTER TABLE events
    ADD CONSTRAINT fk_events_deleted_by FOREIGN KEY (deleted_by) REFERENCES admin_users (id);
