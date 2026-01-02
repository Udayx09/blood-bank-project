-- ========================================
-- DEMO BLOOD BANK DATA
-- Run this in your PostgreSQL database
-- ========================================

-- Demo Blood Bank
-- Password: demo123 (BCrypt hash)
-- Login with Phone: 9999999999
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

-- Get the demo blood bank ID and create related data
DO $$
DECLARE
    demo_bank_id BIGINT;
BEGIN
    SELECT id INTO demo_bank_id FROM blood_banks WHERE phone = '9999999999';
    
    IF demo_bank_id IS NULL THEN
        RAISE NOTICE 'Demo blood bank not found. Please check if it was created.';
        RETURN;
    END IF;

    RAISE NOTICE 'Creating demo data for blood bank ID: %', demo_bank_id;

    -- ========================================
    -- INVENTORY DATA (8 blood types)
    -- ========================================
    INSERT INTO blood_inventory (blood_bank_id, blood_type, units_available, updated_at)
    VALUES 
        (demo_bank_id, 'A+', 25, NOW()),
        (demo_bank_id, 'A-', 8, NOW()),
        (demo_bank_id, 'B+', 30, NOW()),
        (demo_bank_id, 'B-', 5, NOW()),
        (demo_bank_id, 'AB+', 12, NOW()),
        (demo_bank_id, 'AB-', 3, NOW()),
        (demo_bank_id, 'O+', 45, NOW()),
        (demo_bank_id, 'O-', 10, NOW())
    ON CONFLICT (blood_bank_id, blood_type) DO UPDATE SET units_available = EXCLUDED.units_available;

    -- ========================================
    -- RESERVATIONS (Various statuses)
    -- ========================================
    INSERT INTO reservations (blood_bank_id, patient_name, blood_type, units_needed, urgency_level, status, whatsapp_number, referring_doctor, hospital_name, created_at)
    VALUES 
        (demo_bank_id, 'Rahul Sharma', 'A+', 2, 'normal', 'pending', '9876543001', 'Dr. Patel', 'City Hospital', NOW() - INTERVAL '1 hour'),
        (demo_bank_id, 'Priya Gupta', 'O-', 3, 'urgent', 'pending', '9876543002', 'Dr. Mehra', 'Apollo Hospital', NOW() - INTERVAL '2 hours'),
        (demo_bank_id, 'Amit Kumar', 'B+', 1, 'emergency', 'confirmed', '9876543003', 'Dr. Singh', 'Max Hospital', NOW() - INTERVAL '5 hours'),
        (demo_bank_id, 'Sneha Reddy', 'AB+', 2, 'normal', 'completed', '9876543004', 'Dr. Rao', 'Fortis Hospital', NOW() - INTERVAL '1 day'),
        (demo_bank_id, 'Vikram Joshi', 'O+', 4, 'urgent', 'completed', '9876543005', 'Dr. Kapoor', 'Lilavati Hospital', NOW() - INTERVAL '2 days'),
        (demo_bank_id, 'Anjali Nair', 'A-', 1, 'normal', 'cancelled', '9876543006', 'Dr. Iyer', 'Breach Candy Hospital', NOW() - INTERVAL '3 days'),
        (demo_bank_id, 'Rajesh Menon', 'B-', 2, 'emergency', 'completed', '9876543007', 'Dr. Pillai', 'Hinduja Hospital', NOW() - INTERVAL '4 days'),
        (demo_bank_id, 'Kavitha Prasad', 'AB-', 1, 'normal', 'completed', '9876543008', 'Dr. Venkat', 'Kokilaben Hospital', NOW() - INTERVAL '5 days')
    ON CONFLICT DO NOTHING;

    -- ========================================
    -- BLOOD UNITS (With varying expiry dates)
    -- ========================================
    INSERT INTO blood_units (blood_bank_id, unit_number, blood_type, component, component_name, collection_date, expiry_date, status, donation_id, created_at)
    VALUES 
        -- Whole Blood units (35-42 days shelf life)
        (demo_bank_id, 'WB-DEMO-001', 'A+', 'WB', 'Whole Blood', CURRENT_DATE - 30, CURRENT_DATE + 5, 'AVAILABLE', NULL, NOW()),
        (demo_bank_id, 'WB-DEMO-002', 'O+', 'WB', 'Whole Blood', CURRENT_DATE - 28, CURRENT_DATE + 7, 'AVAILABLE', NULL, NOW()),
        (demo_bank_id, 'WB-DEMO-003', 'B+', 'WB', 'Whole Blood', CURRENT_DATE - 35, CURRENT_DATE, 'AVAILABLE', NULL, NOW()),
        (demo_bank_id, 'WB-DEMO-004', 'AB+', 'WB', 'Whole Blood', CURRENT_DATE - 10, CURRENT_DATE + 25, 'AVAILABLE', NULL, NOW()),
        
        -- Packed RBC (42 days)
        (demo_bank_id, 'RBC-DEMO-001', 'A+', 'PRBC', 'Packed Red Blood Cells', CURRENT_DATE - 20, CURRENT_DATE + 22, 'AVAILABLE', NULL, NOW()),
        (demo_bank_id, 'RBC-DEMO-002', 'O-', 'PRBC', 'Packed Red Blood Cells', CURRENT_DATE - 38, CURRENT_DATE + 4, 'AVAILABLE', NULL, NOW()),
        (demo_bank_id, 'RBC-DEMO-003', 'B-', 'PRBC', 'Packed Red Blood Cells', CURRENT_DATE - 5, CURRENT_DATE + 37, 'RESERVED', NULL, NOW()),
        
        -- Platelets (5-7 days - critical!)
        (demo_bank_id, 'PLT-DEMO-001', 'A+', 'PLT', 'Platelets', CURRENT_DATE - 3, CURRENT_DATE + 2, 'AVAILABLE', NULL, NOW()),
        (demo_bank_id, 'PLT-DEMO-002', 'O+', 'PLT', 'Platelets', CURRENT_DATE - 4, CURRENT_DATE + 1, 'AVAILABLE', NULL, NOW()),
        (demo_bank_id, 'PLT-DEMO-003', 'B+', 'PLT', 'Platelets', CURRENT_DATE - 1, CURRENT_DATE + 4, 'AVAILABLE', NULL, NOW()),
        
        -- Fresh Frozen Plasma (1 year)
        (demo_bank_id, 'FFP-DEMO-001', 'A+', 'FFP', 'Fresh Frozen Plasma', CURRENT_DATE - 60, CURRENT_DATE + 305, 'AVAILABLE', NULL, NOW()),
        (demo_bank_id, 'FFP-DEMO-002', 'AB+', 'FFP', 'Fresh Frozen Plasma', CURRENT_DATE - 90, CURRENT_DATE + 275, 'AVAILABLE', NULL, NOW()),
        
        -- Some used/expired units
        (demo_bank_id, 'WB-DEMO-OLD', 'O+', 'WB', 'Whole Blood', CURRENT_DATE - 50, CURRENT_DATE - 8, 'EXPIRED', NULL, NOW()),
        (demo_bank_id, 'RBC-DEMO-USED', 'A+', 'PRBC', 'Packed Red Blood Cells', CURRENT_DATE - 30, CURRENT_DATE + 12, 'USED', NULL, NOW())
    ON CONFLICT (unit_number) DO NOTHING;

    RAISE NOTICE 'Demo data created successfully!';
END $$;

-- ========================================
-- DEMO DONORS (For Find Donors feature)
-- ========================================
INSERT INTO donors (name, email, phone, blood_type, city, date_of_birth, password_hash, is_verified, last_donation_date, opt_in_for_requests, created_at)
VALUES 
    ('Arjun Patel', 'arjun.demo@test.com', '9900001001', 'A+', 'Mumbai', '1990-05-15', '$2a$10$N9qo8uLOickgx2ZMRZoMye/0d4.u0Xz/2v5/0PZj3Z7FCGA.VnSOi', true, CURRENT_DATE - 100, true, NOW()),
    ('Meera Singh', 'meera.demo@test.com', '9900001002', 'O+', 'Mumbai', '1988-08-22', '$2a$10$N9qo8uLOickgx2ZMRZoMye/0d4.u0Xz/2v5/0PZj3Z7FCGA.VnSOi', true, CURRENT_DATE - 95, true, NOW()),
    ('Rohan Desai', 'rohan.demo@test.com', '9900001003', 'B+', 'Mumbai', '1995-03-10', '$2a$10$N9qo8uLOickgx2ZMRZoMye/0d4.u0Xz/2v5/0PZj3Z7FCGA.VnSOi', true, CURRENT_DATE - 120, true, NOW()),
    ('Ananya Iyer', 'ananya.demo@test.com', '9900001004', 'AB+', 'Mumbai', '1992-11-28', '$2a$10$N9qo8uLOickgx2ZMRZoMye/0d4.u0Xz/2v5/0PZj3Z7FCGA.VnSOi', true, CURRENT_DATE - 110, true, NOW()),
    ('Karthik Raja', 'karthik.demo@test.com', '9900001005', 'O-', 'Mumbai', '1985-07-07', '$2a$10$N9qo8uLOickgx2ZMRZoMye/0d4.u0Xz/2v5/0PZj3Z7FCGA.VnSOi', true, CURRENT_DATE - 200, true, NOW()),
    ('Divya Nair', 'divya.demo@test.com', '9900001006', 'A-', 'Mumbai', '1993-02-14', '$2a$10$N9qo8uLOickgx2ZMRZoMye/0d4.u0Xz/2v5/0PZj3Z7FCGA.VnSOi', true, CURRENT_DATE - 150, true, NOW()),
    ('Sanjay Kumar', 'sanjay.demo@test.com', '9900001007', 'B-', 'Mumbai', '1987-09-03', '$2a$10$N9qo8uLOickgx2ZMRZoMye/0d4.u0Xz/2v5/0PZj3Z7FCGA.VnSOi', true, CURRENT_DATE - 180, true, NOW()),
    ('Pooja Sharma', 'pooja.demo@test.com', '9900001008', 'AB-', 'Mumbai', '1991-12-25', '$2a$10$N9qo8uLOickgx2ZMRZoMye/0d4.u0Xz/2v5/0PZj3Z7FCGA.VnSOi', true, CURRENT_DATE - 90, true, NOW())
ON CONFLICT (email) DO NOTHING;

-- ========================================
-- VERIFICATION
-- ========================================
SELECT '✅ Demo Blood Bank:' AS status, phone, name FROM blood_banks WHERE phone = '9999999999';
SELECT '📊 Inventory count:' AS status, COUNT(*) AS count FROM blood_inventory WHERE blood_bank_id = (SELECT id FROM blood_banks WHERE phone = '9999999999');
SELECT '📋 Reservations:' AS status, COUNT(*) AS count FROM reservations WHERE blood_bank_id = (SELECT id FROM blood_banks WHERE phone = '9999999999');
SELECT '🩸 Blood units:' AS status, COUNT(*) AS count FROM blood_units WHERE blood_bank_id = (SELECT id FROM blood_banks WHERE phone = '9999999999');
SELECT '👥 Donors:' AS status, COUNT(*) AS count FROM donors;

-- ========================================
-- 🔐 LOGIN CREDENTIALS FOR DEMO
-- ========================================
-- 
-- Bank Portal Login:
--   Phone: 9999999999
--   Password: demo123
--
-- ⚠️ Admin Panel: DO NOT SHARE (keep admin credentials private)
-- 
-- ========================================
