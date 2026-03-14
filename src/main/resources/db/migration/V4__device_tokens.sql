-- ============================================
-- V4: Device Tokens for Push Notifications
-- ============================================

CREATE TABLE device_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    fcm_token   TEXT        NOT NULL UNIQUE,
    platform    VARCHAR(10) NOT NULL CHECK (platform IN ('ios', 'android')),
    is_active   BOOLEAN     NOT NULL DEFAULT true,
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_device_tokens_active ON device_tokens (is_active) WHERE is_active = true;

COMMENT ON TABLE device_tokens IS 'FCM device tokens registered by mobile app users for push notifications';
COMMENT ON COLUMN device_tokens.fcm_token IS 'Firebase Cloud Messaging registration token from the mobile device';
COMMENT ON COLUMN device_tokens.platform IS 'Mobile platform: ios or android';
COMMENT ON COLUMN device_tokens.is_active IS 'False when FCM reports token as invalid/expired';
