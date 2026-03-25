-- V10__stripe_connect.sql
-- Add Stripe Connect fields to masjid_settings

ALTER TABLE masjid_settings
    ADD COLUMN IF NOT EXISTS stripe_account_id    VARCHAR(255),
    ADD COLUMN IF NOT EXISTS stripe_connected_at  TIMESTAMP WITH TIME ZONE;
