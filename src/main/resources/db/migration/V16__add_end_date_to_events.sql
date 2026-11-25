-- Optional end of the event, stored like `date` (timestamp without time zone).
-- Nullable so existing events, which have no end time, stay valid.
ALTER TABLE events
    ADD COLUMN end_date TIMESTAMP;
