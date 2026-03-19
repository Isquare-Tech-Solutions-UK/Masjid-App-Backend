-- V9 - Drop device_tokens table
-- Push notifications are now sent via Firebase topic-based pub/sub.
-- Device tokens are managed entirely by Firebase; no server-side storage needed.

DROP TABLE IF EXISTS public.device_tokens;
