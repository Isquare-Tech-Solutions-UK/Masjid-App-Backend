-- V8 - Fix events.images column type from varchar(255) to jsonb
-- Existing rows may have truncated/invalid data so we reset them to []
-- before altering the column type.

-- Step 1: Reset any existing (likely corrupted) image data
UPDATE public.events SET images = '[]';

-- Step 2: Alter column type to jsonb
ALTER TABLE public.events
    ALTER COLUMN images TYPE jsonb
    USING images::jsonb;

-- Step 3: Ensure default and not-null are set correctly
ALTER TABLE public.events
    ALTER COLUMN images SET DEFAULT '[]'::jsonb;

ALTER TABLE public.events
    ALTER COLUMN images SET NOT NULL;
