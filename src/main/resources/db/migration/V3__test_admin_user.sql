-- ============================================
-- MASJID APP - TEST ADMIN USER
-- Version: 1.0
-- Purpose: Create test admin user for development
-- ============================================

-- Test Admin User
-- Email: admin@masjid-app.com
-- Password: Admin@123 (BCrypt hashed)
-- Role: super_admin

INSERT INTO admin_users (
    id,
    username,
    email,
    password_hash,
    full_name,
    role,
    is_active,
    failed_login_attempts,
    created_at,
    updated_at
) VALUES (
    gen_random_uuid(),
    'admin',
    'admin@masjid-app.com',
    -- BCrypt hash of 'Admin@123' with strength 10
    '$2b$10$Q4dcH1pN2fvfZdSWQEeEI.yJyVnQqBdNhPROZarw2opv4ozpQyaVm',
    'Super Admin',
    'super_admin',
    true,
    0,
    NOW(),
    NOW()
);

-- Additional test admin (regular admin role)
-- Email: manager@masjid-app.com
-- Password: Manager@123

INSERT INTO admin_users (
    id,
    username,
    email,
    password_hash,
    full_name,
    role,
    is_active,
    failed_login_attempts,
    created_at,
    updated_at
) VALUES (
    gen_random_uuid(),
    'manager',
    'manager@masjid-app.com',
    -- BCrypt hash of 'Manager@123' with strength 10
    '$2b$10$7NuwgtLt0Z.3gxvwyLDh4OGrkeOt19TtAzCAx2028O2ZdGeeFymoi',
    'Masjid Manager',
    'admin',
    true,
    0,
    NOW(),
    NOW()
);

COMMENT ON TABLE admin_users IS 'Test users added for development. Remove in production.';
