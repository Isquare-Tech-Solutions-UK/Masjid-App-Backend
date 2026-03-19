-- ============================================
-- V5 - Announcements table
-- Structure:
--   id uuid PK
--   title varchar
--   message text
--   scheduled_at timestamp
--   status announcement_status DEFAULT 'draft'
--   created_by (FK -> admin_users.id)
--   created_at, updated_at
-- ============================================

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'announcement_status') THEN
        CREATE TYPE public.announcement_status AS ENUM (
            'draft',
            'scheduled',
            'sent'
        );
    END IF;
END;
$$;

DROP TABLE IF EXISTS public.announcements;

CREATE TABLE public.announcements (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    title varchar(255) NOT NULL,
    message text NOT NULL,
    scheduled_at timestamp,
    status public.announcement_status DEFAULT 'draft'::announcement_status NOT NULL,
    
    -- Notification tracking
    notification_sent boolean DEFAULT false,
    notification_sent_at timestamp,

    -- Metadata
    created_by uuid REFERENCES public.admin_users(id),
    created_at timestamp DEFAULT now(),
    updated_at timestamp DEFAULT now()
);

-- Helpful indexes
CREATE INDEX idx_announcements_status ON public.announcements(status);
CREATE INDEX idx_announcements_scheduled_at ON public.announcements(scheduled_at);
CREATE INDEX idx_announcements_created_by ON public.announcements(created_by);
