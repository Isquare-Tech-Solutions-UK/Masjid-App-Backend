-- ============================================
-- Remove the development test admins created by V3__test_admin_user.sql.
--
-- V3 is left untouched on purpose: Flyway checksums applied migrations and
-- refuses to start on any database where V3 has already run if its contents
-- change. This migration undoes it instead, on every database, old and new.
--
-- Both accounts have passwords published in V3's comments, so they must not
-- exist anywhere the app is reachable.
-- ============================================

-- Content and audit rows these accounts created keep existing; they just lose
-- the link to the deleted user. Without this the DELETE below fails on the
-- foreign keys (none of them cascade), Flyway aborts, and the app will not
-- start. Every reader of these columns already treats null as "unknown".
UPDATE events        SET created_by = NULL WHERE created_by IN (SELECT id FROM admin_users WHERE email IN ('admin@masjid-app.com', 'manager@masjid-app.com'));
UPDATE events        SET deleted_by = NULL WHERE deleted_by IN (SELECT id FROM admin_users WHERE email IN ('admin@masjid-app.com', 'manager@masjid-app.com'));
UPDATE announcements SET created_by = NULL WHERE created_by IN (SELECT id FROM admin_users WHERE email IN ('admin@masjid-app.com', 'manager@masjid-app.com'));
UPDATE campaigns     SET created_by = NULL WHERE created_by IN (SELECT id FROM admin_users WHERE email IN ('admin@masjid-app.com', 'manager@masjid-app.com'));
UPDATE audit_logs    SET user_id    = NULL WHERE user_id    IN (SELECT id FROM admin_users WHERE email IN ('admin@masjid-app.com', 'manager@masjid-app.com'));

-- refresh_tokens.admin_user_id is ON DELETE CASCADE, so their sessions go too.
DELETE FROM admin_users
WHERE email IN ('admin@masjid-app.com', 'manager@masjid-app.com');

COMMENT ON TABLE admin_users IS 'Admin portal users.';
