-- V14__stripe_direct_keys.sql
-- Replace Stripe Connect (connected-account) model with the charity's own
-- standalone Stripe API keys stored directly on masjid_settings.
--
-- The secret key and webhook signing secret are encrypted at rest by the
-- application (AES-256-GCM), so they are stored as base64 TEXT ciphertext.
-- The publishable key is public and stored in plain text.

ALTER TABLE masjid_settings
    DROP COLUMN IF EXISTS stripe_account_id,
    DROP COLUMN IF EXISTS stripe_connected_at,
    DROP COLUMN IF EXISTS stripe_account_status,
    DROP COLUMN IF EXISTS stripe_accepting_donations,
    DROP COLUMN IF EXISTS stripe_onboarding_complete,
    DROP COLUMN IF EXISTS stripe_payouts_enabled;

ALTER TABLE masjid_settings
    ADD COLUMN IF NOT EXISTS stripe_publishable_key     VARCHAR(255),
    ADD COLUMN IF NOT EXISTS stripe_secret_key_enc      TEXT,
    ADD COLUMN IF NOT EXISTS stripe_webhook_secret_enc  TEXT,
    ADD COLUMN IF NOT EXISTS stripe_key_mode            VARCHAR(10),
    ADD COLUMN IF NOT EXISTS stripe_keys_updated_at     TIMESTAMP WITH TIME ZONE;