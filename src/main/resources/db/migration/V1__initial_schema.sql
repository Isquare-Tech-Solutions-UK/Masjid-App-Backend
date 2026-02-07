-- ============================================
-- MASJID APP - DATABASE SCHEMA
-- Version: 1.0
-- Database: PostgreSQL 15+
-- ============================================

-- ============================================
-- EXTENSIONS
-- ============================================
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================
-- ENUM TYPES
-- ============================================

-- Event status enum
CREATE TYPE event_status AS ENUM (
    'draft',
    'published',
    'cancelled',
    'completed'
);

-- Announcement status enum
CREATE TYPE announcement_status AS ENUM (
    'draft',
    'scheduled',
    'sent',
    'failed'
);

-- Campaign status enum
CREATE TYPE campaign_status AS ENUM (
    'draft',
    'active',
    'paused',
    'completed',
    'cancelled'
);

-- Donation status enum
CREATE TYPE donation_status AS ENUM (
    'pending',
    'completed',
    'failed',
    'refunded'
);

-- Admin role enum
CREATE TYPE admin_role AS ENUM (
    'super_admin',
    'admin'
);

-- ============================================
-- TABLE: admin_users
-- Purpose: Authentication for masjid administrators
-- ============================================
CREATE TABLE admin_users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role admin_role DEFAULT 'admin',
    is_active BOOLEAN DEFAULT true,
    
    -- Security
    failed_login_attempts INT DEFAULT 0,
    locked_until TIMESTAMP,
    last_login_at TIMESTAMP,
    
    -- Metadata
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Indexes for admin_users
CREATE INDEX idx_admin_users_username ON admin_users(username);
CREATE INDEX idx_admin_users_email ON admin_users(email);
CREATE INDEX idx_admin_users_active ON admin_users(is_active) WHERE is_active = true;

COMMENT ON TABLE admin_users IS 'Stores masjid administrator accounts for web portal access';
COMMENT ON COLUMN admin_users.failed_login_attempts IS 'Counter for rate limiting, resets on successful login';
COMMENT ON COLUMN admin_users.locked_until IS 'Account locked until this timestamp after 5 failed attempts';

-- ============================================
-- TABLE: prayer_times
-- Purpose: Daily prayer schedules with JSONB flexibility
-- ============================================
CREATE TABLE prayer_times (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    date DATE NOT NULL UNIQUE,
    hijri_date VARCHAR(50),
    
    -- All prayer times stored as JSONB for flexibility
    prayers JSONB NOT NULL,
    /*
    Example prayers JSON:
    {
        "fajr": {"athan": "05:45", "jamah": "06:15"},
        "sunrise": {"time": "07:30"},
        "zuhr": {"athan": "12:30", "jamah": "13:00"},
        "asr": {"athan": "15:30", "jamah": "16:00"},
        "maghrib": {"athan": "17:45", "jamah": "17:50"},
        "isha": {"athan": "19:15", "jamah": "19:45"}
    }
    */
    
    -- Multiple Jumu'ah times for Fridays (null for other days)
    jumuah_times JSONB,
    /*
    Example: ["13:00", "14:00", "14:45"]
    */
    
    -- Metadata
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Indexes for prayer_times
CREATE INDEX idx_prayer_times_date ON prayer_times(date);
CREATE INDEX idx_prayer_times_date_range ON prayer_times(date DESC);
CREATE INDEX idx_prayer_times_prayers ON prayer_times USING GIN (prayers);

COMMENT ON TABLE prayer_times IS 'Daily prayer times with JSONB for flexible prayer structure';
COMMENT ON COLUMN prayer_times.prayers IS 'JSON object containing athan and jamah times for all 6 prayers';
COMMENT ON COLUMN prayer_times.jumuah_times IS 'Array of Friday Jumu''ah prayer times, replaces Zuhr on Fridays';

-- ============================================
-- TABLE: events
-- Purpose: Community events with JSONB for flexible details
-- ============================================
CREATE TABLE events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    category VARCHAR(50) NOT NULL,  -- e.g., 'community', 'educational', 'fundraiser', 'religious', 'youth', 'other'
    status event_status DEFAULT 'draft',
    
    -- Keep event_date as column for fast calendar queries
    event_date DATE NOT NULL,
    
    -- Event timing details (JSONB for flexibility)
    timing JSONB NOT NULL DEFAULT '{}',
    /*
    Example:
    {
        "start_time": "18:00",
        "end_time": "21:00",
        "doors_open": "17:30",
        "duration_minutes": 180,
        "timezone": "Europe/London"
    }
    */
    
    -- Location & venue details (JSONB)
    location JSONB NOT NULL DEFAULT '{}',
    /*
    Example:
    {
        "venue": "Main Prayer Hall",
        "address": "82-92 Whitechapel Road, London E1 1JQ",
        "room": "Ground Floor",
        "capacity": 500,
        "coordinates": {
            "latitude": 51.5172,
            "longitude": -0.0651
        },
        "parking_info": "Street parking available",
        "accessibility": "Wheelchair accessible"
    }
    */
    
    -- Speaker/presenter details (JSONB - supports single or multiple)
    speaker JSONB DEFAULT '{}',
    /*
    Single speaker example:
    {
        "name": "Sheikh Ahmad Ali",
        "credentials": "Islamic Scholar, PhD in Islamic Studies",
        "bio": "Sheikh Ahmad has been teaching for over 20 years...",
        "photo_url": "https://s3.../speaker.jpg"
    }
    
    Multiple speakers example:
    [
        {"name": "Speaker 1", "credentials": "..."},
        {"name": "Speaker 2", "credentials": "..."}
    ]
    */
    
    -- Media/images (JSONB - supports featured image and gallery)
    media JSONB DEFAULT '{}',
    /*
    Example:
    {
        "featured_image": {
            "url": "https://s3.../event-image.jpg",
            "key": "events/2025/01/event-123.jpg",
            "alt": "Islamic lecture event"
        },
        "gallery": [
            {"url": "...", "key": "...", "alt": "..."}
        ],
        "thumbnail_url": "https://s3.../thumb.jpg"
    }
    */
    
    -- External registration
    registration_link VARCHAR(500),
    
    -- Notification tracking
    notification_sent BOOLEAN DEFAULT false,
    notification_sent_at TIMESTAMP,
    
    -- Metadata
    created_by UUID REFERENCES admin_users(id),
    published_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Indexes for events
CREATE INDEX idx_events_date ON events(event_date);
CREATE INDEX idx_events_status ON events(status);
CREATE INDEX idx_events_category ON events(category);
CREATE INDEX idx_events_published_date ON events(event_date, status) 
    WHERE status = 'published';
CREATE INDEX idx_events_created_by ON events(created_by);

COMMENT ON TABLE events IS 'Community events with JSONB for timing, location, speaker, and media';
COMMENT ON COLUMN events.event_date IS 'Kept as DATE column for fast calendar queries and indexing';
COMMENT ON COLUMN events.timing IS 'Flexible timing details including start/end times, doors open, etc.';
COMMENT ON COLUMN events.location IS 'Venue details, address, capacity, coordinates, accessibility info';
COMMENT ON COLUMN events.speaker IS 'Speaker info - can be single object or array for multiple speakers';
COMMENT ON COLUMN events.media IS 'Images with S3 URLs and keys for featured image and gallery';

-- ============================================
-- TABLE: announcements
-- Purpose: Push notifications (immediate and scheduled)
-- ============================================
CREATE TABLE announcements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(100) NOT NULL,
    message TEXT NOT NULL,
    status announcement_status DEFAULT 'draft',
    
    -- Scheduling
    is_scheduled BOOLEAN DEFAULT false,
    scheduled_for TIMESTAMP,
    
    -- Delivery tracking
    sent_at TIMESTAMP,
    fcm_message_id VARCHAR(255),
    
    -- Metadata
    created_by UUID REFERENCES admin_users(id),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Indexes for announcements
CREATE INDEX idx_announcements_scheduled ON announcements(scheduled_for, status) 
    WHERE status = 'scheduled';
CREATE INDEX idx_announcements_status ON announcements(status, created_at DESC);
CREATE INDEX idx_announcements_created_by ON announcements(created_by);

COMMENT ON TABLE announcements IS 'Push notification announcements - immediate or scheduled';
COMMENT ON COLUMN announcements.scheduled_for IS 'When to send scheduled announcement, NULL for immediate';
COMMENT ON COLUMN announcements.fcm_message_id IS 'Firebase Cloud Messaging message ID for tracking';

-- ============================================
-- TABLE: campaigns
-- Purpose: Fundraising campaigns with goals and progress
-- ============================================
CREATE TABLE campaigns (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    category VARCHAR(50) NOT NULL,  -- e.g., 'masjid_development', 'education', 'charity', 'individual_support', 'general'
    status campaign_status DEFAULT 'draft',
    
    -- Financial targets
    goal_amount DECIMAL(10, 2) NOT NULL,
    raised_amount DECIMAL(10, 2) DEFAULT 0.00,
    donor_count INT DEFAULT 0,
    
    -- Timeline
    start_date DATE NOT NULL,
    end_date DATE,
    
    -- Media (JSONB for flexibility)
    media JSONB DEFAULT '{}',
    /*
    Example:
    {
        "featured_image": {
            "url": "https://s3.../campaign-image.jpg",
            "key": "campaigns/2025/01/campaign-123.jpg"
        }
    }
    */
    
    -- Metadata
    created_by UUID REFERENCES admin_users(id),
    published_at TIMESTAMP,
    ended_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Indexes for campaigns
CREATE INDEX idx_campaigns_status ON campaigns(status);
CREATE INDEX idx_campaigns_active ON campaigns(status, end_date) 
    WHERE status = 'active';
CREATE INDEX idx_campaigns_category ON campaigns(category);
CREATE INDEX idx_campaigns_created_by ON campaigns(created_by);

COMMENT ON TABLE campaigns IS 'Fundraising campaigns with goals, progress tracking, and timeline';
COMMENT ON COLUMN campaigns.raised_amount IS 'Running total updated via triggers or application code';
COMMENT ON COLUMN campaigns.donor_count IS 'Count of unique donations (not unique donors if anonymous)';

-- ============================================
-- TABLE: donations
-- Purpose: Individual donation records linked to campaigns
-- ============================================
CREATE TABLE donations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    campaign_id UUID NOT NULL REFERENCES campaigns(id),
    
    -- Donor info (optional for anonymous donations)
    donor_name VARCHAR(100),
    donor_email VARCHAR(255),
    is_anonymous BOOLEAN DEFAULT false,
    
    -- Payment details
    amount DECIMAL(10, 2) NOT NULL,
    processing_fee DECIMAL(10, 2) DEFAULT 0.00,
    cover_fee BOOLEAN DEFAULT false,
    total_charged DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'GBP',
    
    -- Stripe integration
    stripe_payment_intent_id VARCHAR(255) UNIQUE,
    stripe_checkout_session_id VARCHAR(255),
    payment_method VARCHAR(20),  -- e.g., 'card', 'apple_pay', 'google_pay', 'paypal'
    status donation_status DEFAULT 'pending',
    
    -- Receipt tracking
    receipt_sent BOOLEAN DEFAULT false,
    receipt_sent_at TIMESTAMP,
    
    -- Metadata
    completed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Indexes for donations
CREATE INDEX idx_donations_campaign ON donations(campaign_id);
CREATE INDEX idx_donations_status ON donations(status);
CREATE INDEX idx_donations_stripe_payment ON donations(stripe_payment_intent_id);
CREATE INDEX idx_donations_stripe_session ON donations(stripe_checkout_session_id);
CREATE INDEX idx_donations_completed ON donations(campaign_id, status) 
    WHERE status = 'completed';
CREATE INDEX idx_donations_created ON donations(created_at DESC);

COMMENT ON TABLE donations IS 'Individual donation records with Stripe payment tracking';
COMMENT ON COLUMN donations.stripe_payment_intent_id IS 'Unique Stripe payment intent - prevents duplicate processing';
COMMENT ON COLUMN donations.cover_fee IS 'True if donor chose to cover processing fees';
COMMENT ON COLUMN donations.total_charged IS 'Final amount charged: amount + fee if cover_fee is true';

-- ============================================
-- TABLE: masjid_settings
-- Purpose: Masjid profile, services, facilities, payment config
-- ============================================
CREATE TABLE masjid_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Basic info
    name VARCHAR(255) NOT NULL,
    about TEXT,
    logo_url VARCHAR(500),
    logo_key VARCHAR(255),
    
    -- Contact information
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(100),
    postcode VARCHAR(20),
    country VARCHAR(100) DEFAULT 'United Kingdom',
    phone VARCHAR(20),
    email VARCHAR(255),
    website VARCHAR(500),
    
    -- Location for map display and Qibla calculation
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    
    -- Capacity
    mens_capacity INT,
    womens_capacity INT,
    
    -- Services offered (JSONB)
    services JSONB DEFAULT '{}',
    /*
    Example:
    {
        "marriage": true,
        "hall_rental": true,
        "iftar": false,
        "counseling": true,
        "new_muslim_support": true,
        "funeral": true,
        "quran_classes": true,
        "islamic_school": true
    }
    */
    
    -- Facilities available (JSONB)
    facilities JSONB DEFAULT '{}',
    /*
    Example:
    {
        "parking": true,
        "disability_access": true,
        "wudu_facilities": true,
        "sisters_area": true,
        "childrens_area": false,
        "library": true,
        "shoe_racks": true,
        "cafe": false
    }
    */
    
    -- Stripe Connect configuration
    stripe_account_id VARCHAR(255),
    stripe_account_status VARCHAR(50),
    
    -- Bank details (for display, actual payouts via Stripe)
    bank_account_name VARCHAR(255),
    bank_account_number_last4 VARCHAR(4),
    bank_sort_code VARCHAR(10),
    
    -- Metadata
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

COMMENT ON TABLE masjid_settings IS 'Single row table for masjid profile, services, facilities, and payment config';
COMMENT ON COLUMN masjid_settings.services IS 'JSONB of available services with boolean flags';
COMMENT ON COLUMN masjid_settings.facilities IS 'JSONB of available facilities with boolean flags';
COMMENT ON COLUMN masjid_settings.stripe_account_status IS 'Status: pending, active, restricted, etc.';

-- ============================================
-- TABLE: audit_logs (Optional - for tracking changes)
-- Purpose: Track all admin actions for accountability
-- ============================================
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES admin_users(id),
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID NOT NULL,
    old_values JSONB,
    new_values JSONB,
    ip_address INET,
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Indexes for audit_logs
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_user ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_created ON audit_logs(created_at DESC);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);

COMMENT ON TABLE audit_logs IS 'Audit trail for all admin actions - useful for debugging and compliance';
COMMENT ON COLUMN audit_logs.action IS 'Action type: create, update, delete, login, logout, etc.';
COMMENT ON COLUMN audit_logs.old_values IS 'Previous values before update (for update/delete actions)';
COMMENT ON COLUMN audit_logs.new_values IS 'New values after change (for create/update actions)';

-- ============================================
-- FUNCTIONS & TRIGGERS
-- ============================================

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply updated_at trigger to all tables with updated_at column
CREATE TRIGGER update_admin_users_updated_at
    BEFORE UPDATE ON admin_users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_prayer_times_updated_at
    BEFORE UPDATE ON prayer_times
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_events_updated_at
    BEFORE UPDATE ON events
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_announcements_updated_at
    BEFORE UPDATE ON announcements
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_campaigns_updated_at
    BEFORE UPDATE ON campaigns
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_donations_updated_at
    BEFORE UPDATE ON donations
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_masjid_settings_updated_at
    BEFORE UPDATE ON masjid_settings
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- Function to update campaign totals when donation completes
-- ============================================
CREATE OR REPLACE FUNCTION update_campaign_totals()
RETURNS TRIGGER AS $$
BEGIN
    -- When donation status changes to 'completed'
    IF NEW.status = 'completed' AND (OLD.status IS NULL OR OLD.status != 'completed') THEN
        UPDATE campaigns
        SET 
            raised_amount = raised_amount + NEW.amount,
            donor_count = donor_count + 1,
            updated_at = NOW()
        WHERE id = NEW.campaign_id;
    END IF;
    
    -- When donation is refunded
    IF NEW.status = 'refunded' AND OLD.status = 'completed' THEN
        UPDATE campaigns
        SET 
            raised_amount = raised_amount - OLD.amount,
            donor_count = donor_count - 1,
            updated_at = NOW()
        WHERE id = NEW.campaign_id;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_campaign_totals
    AFTER INSERT OR UPDATE ON donations
    FOR EACH ROW EXECUTE FUNCTION update_campaign_totals();

-- ============================================
-- INITIAL DATA
-- ============================================

-- Insert default masjid settings row (to be updated via admin panel)
INSERT INTO masjid_settings (
    name,
    services,
    facilities
) VALUES (
    'My Masjid',
    '{
        "marriage": false,
        "hall_rental": false,
        "iftar": false,
        "counseling": false,
        "new_muslim_support": false,
        "funeral": false,
        "quran_classes": false
    }',
    '{
        "parking": false,
        "disability_access": false,
        "wudu_facilities": false,
        "sisters_area": false,
        "childrens_area": false,
        "library": false,
        "shoe_racks": false
    }'
);

-- ============================================
-- SCHEMA COMPLETE
-- ============================================
