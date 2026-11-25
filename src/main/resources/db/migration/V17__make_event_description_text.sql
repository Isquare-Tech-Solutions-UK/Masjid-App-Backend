-- Make events.description unlimited text everywhere.
-- V4 creates it as TEXT, but some deployed databases have it as varchar(255),
-- so descriptions over 255 characters failed to save. On databases where the
-- column is already TEXT this is a no-op. Length is enforced by the API (5000).
ALTER TABLE events ALTER COLUMN description TYPE TEXT;
