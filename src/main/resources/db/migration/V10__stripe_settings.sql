-- V10 - Add Stripe Connect fields to masjid_settings
-- Stores the connected Express account ID and live status synced from Stripe webhooks.

ALTER TABLE public.masjid_settings
    ADD COLUMN IF NOT EXISTS stripe_account_id        VARCHAR(255),
    ADD COLUMN IF NOT EXISTS stripe_onboarding_complete BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS stripe_accepting_donations BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS stripe_payouts_enabled    BOOLEAN NOT NULL DEFAULT FALSE;
