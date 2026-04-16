-- ============================================
-- MIGRATION V2
-- Add 'cancelled' value to announcement_status enum
-- ============================================
-- IMPORTANT: PostgreSQL does NOT allow ALTER TYPE ... ADD VALUE inside
-- a transaction block. Run this script manually (outside BEGIN/COMMIT)
-- directly on the PostgreSQL instance BEFORE deploying the code changes.
-- ============================================

ALTER TYPE announcement_status ADD VALUE IF NOT EXISTS 'cancelled';
