-- V8 - Fix events.images column type from varchar(255) to jsonb
-- The original V4 migration was applied with varchar(255); this aligns the DB with the entity.

ALTER TABLE public.events
    ALTER COLUMN images TYPE jsonb
    USING CASE
        WHEN images IS NULL OR images = '' THEN '[]'::jsonb
        ELSE images::jsonb
    END;

ALTER TABLE public.events
    ALTER COLUMN images SET DEFAULT '[]'::jsonb;

ALTER TABLE public.events
    ALTER COLUMN images SET NOT NULL;
