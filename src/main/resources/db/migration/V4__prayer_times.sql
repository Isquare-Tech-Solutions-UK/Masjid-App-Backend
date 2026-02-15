-- Prayer Times table
CREATE TABLE prayer_times (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    date DATE NOT NULL UNIQUE,
    hijri_date VARCHAR(50),
    prayers JSONB NOT NULL,
    jumuah_times JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Index for date-range queries
CREATE INDEX idx_prayer_times_date ON prayer_times (date);
