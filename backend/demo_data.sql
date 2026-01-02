-- ========================================
-- DEMO BLOOD BANK DATA FOR NEON SQL EDITOR
-- Copy and paste this into Neon SQL Editor
-- ========================================

-- Step 1: Create Demo Blood Bank
-- Password: demo123 (BCrypt hash)
-- Login Phone: 9999999999
INSERT INTO blood_banks (name, address, city, phone, email, rating, is_open, latitude, longitude, password_hash, created_at, updated_at)
VALUES (
    'Demo City Blood Bank',
    '123 Demo Street, Medical Complex',
    'Mumbai',
    '9999999999',
    'demo@bloodbank.com',
    4.5,
    true,
    19.0760,
    72.8777,
    '$2a$10$N9qo8uLOickgx2ZMRZoMye/0d4.u0Xz/2v5/0PZj3Z7FCGA.VnSOi',
    NOW(),
    NOW()
)
ON CONFLICT DO NOTHING;

-- Step 2: Get the demo bank ID (run this to see the ID)
SELECT id, name, phone FROM blood_banks WHERE phone = '9999999999';
