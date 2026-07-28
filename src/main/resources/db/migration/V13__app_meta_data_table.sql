-- V13__app_meta_data_table.sql
-- Generic key/value metadata table, scoped by module_name.
CREATE TABLE app_meta_data (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    module_name VARCHAR(100) NOT NULL,
    value VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Seed donation causes for the campaign module (previously hardcoded in the frontend)
INSERT INTO app_meta_data (module_name, value) VALUES
    ('campaign_module', 'General'),
    ('campaign_module', 'Masjid Development'),
    ('campaign_module', 'Education & Dawa''h'),
    ('campaign_module', 'Charity & Welfare');
