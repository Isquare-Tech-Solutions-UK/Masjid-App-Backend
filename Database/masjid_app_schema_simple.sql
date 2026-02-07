--
-- Masjid App Database Schema (Simplified for visualization tools)
--

-- TABLE: admin_users
CREATE TABLE admin_users (
    id UUID PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(20) DEFAULT 'admin',
    is_active BOOLEAN DEFAULT true,
    failed_login_attempts INTEGER DEFAULT 0,
    locked_until TIMESTAMP,
    last_login_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- TABLE: prayer_times
CREATE TABLE prayer_times (
    id UUID PRIMARY KEY,
    date DATE NOT NULL UNIQUE,
    hijri_date VARCHAR(50),
    prayers JSONB NOT NULL,
    jumuah_times JSONB,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- TABLE: events
CREATE TABLE events (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    category VARCHAR(50) NOT NULL,
    status VARCHAR(20) DEFAULT 'draft',
    event_date DATE NOT NULL,
    timing JSONB NOT NULL,
    location JSONB NOT NULL,
    speaker JSONB,
    media JSONB,
    registration_link VARCHAR(500),
    notification_sent BOOLEAN DEFAULT false,
    notification_sent_at TIMESTAMP,
    created_by UUID REFERENCES admin_users(id),
    published_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- TABLE: announcements
CREATE TABLE announcements (
    id UUID PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    message TEXT NOT NULL,
    status VARCHAR(20) DEFAULT 'draft',
    is_scheduled BOOLEAN DEFAULT false,
    scheduled_for TIMESTAMP,
    sent_at TIMESTAMP,
    fcm_message_id VARCHAR(255),
    created_by UUID REFERENCES admin_users(id),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- TABLE: campaigns
CREATE TABLE campaigns (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    category VARCHAR(50) NOT NULL,
    status VARCHAR(20) DEFAULT 'draft',
    goal_amount DECIMAL(10,2) NOT NULL,
    raised_amount DECIMAL(10,2) DEFAULT 0.00,
    donor_count INTEGER DEFAULT 0,
    start_date DATE NOT NULL,
    end_date DATE,
    media JSONB,
    created_by UUID REFERENCES admin_users(id),
    published_at TIMESTAMP,
    ended_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- TABLE: donations
CREATE TABLE donations (
    id UUID PRIMARY KEY,
    campaign_id UUID NOT NULL REFERENCES campaigns(id),
    donor_name VARCHAR(100),
    donor_email VARCHAR(255),
    is_anonymous BOOLEAN DEFAULT false,
    amount DECIMAL(10,2) NOT NULL,
    processing_fee DECIMAL(10,2) DEFAULT 0.00,
    cover_fee BOOLEAN DEFAULT false,
    total_charged DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'GBP',
    stripe_payment_intent_id VARCHAR(255) UNIQUE,
    stripe_checkout_session_id VARCHAR(255),
    payment_method VARCHAR(20),
    status VARCHAR(20) DEFAULT 'pending',
    receipt_sent BOOLEAN DEFAULT false,
    receipt_sent_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- TABLE: masjid_settings
CREATE TABLE masjid_settings (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    about TEXT,
    logo_url VARCHAR(500),
    logo_key VARCHAR(255),
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(100),
    postcode VARCHAR(20),
    country VARCHAR(100) DEFAULT 'United Kingdom',
    phone VARCHAR(20),
    email VARCHAR(255),
    website VARCHAR(500),
    latitude DECIMAL(10,8),
    longitude DECIMAL(11,8),
    mens_capacity INTEGER,
    womens_capacity INTEGER,
    services JSONB,
    facilities JSONB,
    stripe_account_id VARCHAR(255),
    stripe_account_status VARCHAR(50),
    bank_account_name VARCHAR(255),
    bank_account_number_last4 VARCHAR(4),
    bank_sort_code VARCHAR(10),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- TABLE: audit_logs
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES admin_users(id),
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID NOT NULL,
    old_values JSONB,
    new_values JSONB,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);
