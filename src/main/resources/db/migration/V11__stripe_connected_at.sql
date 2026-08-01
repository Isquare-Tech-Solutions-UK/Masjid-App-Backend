ALTER TABLE masjid_settings
    ADD COLUMN IF NOT EXISTS stripe_connected_at TIMESTAMP WITH TIME ZONE;
