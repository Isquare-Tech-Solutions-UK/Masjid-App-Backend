-- ============================================
-- V4 - Events table (simplified)
-- Structure:
--   id uuid PK
--   title varchar
--   speaker varchar
--   date timestamp
--   link varchar
--   images jsonb (multiple URLs)
--   description text
--   venue varchar
--   status event_status DEFAULT 'draft'
--   notification_sent, notification_sent_at
--   created_by (FK -> admin_users.id)
--   published_at, created_at, updated_at
-- ============================================

-- Ensure event_status enum exists (created in V1, but guard just in case)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'event_status') THEN
        CREATE TYPE public.event_status AS ENUM (
            'draft',
            'published',
            'cancelled',
            'completed'
        );
    END IF;
END;
$$;

-- Drop table if it somehow exists already (fresh start for events)
DROP TABLE IF EXISTS public.events;

CREATE TABLE public.events (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,

    -- Core fields
    title varchar(255) NOT NULL,
    speaker varchar(255) NOT NULL,
    date timestamp NOT NULL,
    link varchar(500),

    -- Images JSONB (multiple URLs)
    -- Recommended structure in this column:
    --   ["https://.../image1.jpg", "https://.../image2.png"]
    images jsonb NOT NULL DEFAULT '[]'::jsonb,

    description text NOT NULL,
    venue varchar(255) NOT NULL,

    -- Status
    status public.event_status DEFAULT 'draft'::event_status NOT NULL,

    -- Notification tracking
    notification_sent boolean DEFAULT false,
    notification_sent_at timestamp,

    -- Metadata
    created_by uuid REFERENCES public.admin_users(id),
    published_at timestamp,
    created_at timestamp DEFAULT now(),
    updated_at timestamp DEFAULT now()
);

-- Helpful indexes
CREATE INDEX idx_events_status ON public.events(status);
CREATE INDEX idx_events_date ON public.events(date);
CREATE INDEX idx_events_created_by ON public.events(created_by);
