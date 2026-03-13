-- V6__masjid_settings_table.sql
-- Recreate masjid_settings with JSONB columns (replaces V1 flat-column version)
DROP TABLE IF EXISTS masjid_settings;

CREATE TABLE masjid_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    name VARCHAR(100) NOT NULL,
    about TEXT,
    logo_url VARCHAR(255),
    logo_key VARCHAR(255),

    address JSONB DEFAULT '{}',
    contact JSONB DEFAULT '{}',
    location JSONB DEFAULT '{}',
    capacity JSONB DEFAULT '{}',
    services JSONB DEFAULT '{}',
    facilities JSONB DEFAULT '{}',
    payment JSONB DEFAULT '{}'
);

INSERT INTO masjid_settings (id, name) VALUES (
    '550e8400-e29b-41d4-a716-446655440000',
    'NWK Muslim Association'
);
