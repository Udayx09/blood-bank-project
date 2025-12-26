const express = require('express');
const router = express.Router();
const bcrypt = require('bcrypt');
const { query } = require('../config/database');
const { generateToken } = require('../middleware/authMiddleware');

// Run migration to add password_hash column if not exists
const runMigration = async () => {
    try {
        await query(`
            ALTER TABLE blood_banks 
            ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255)
        `);
        console.log('✅ Auth migration: password_hash column ready');
    } catch (err) {
        // Column might already exist or other issue
        console.log('Auth migration note:', err.message);
    }
};
runMigration();

// POST /api/auth/bank/register - Register a blood bank account
router.post('/bank/register', async (req, res) => {
    const { phone, password, bloodBankId } = req.body;

    if (!phone || !password || !bloodBankId) {
        return res.status(400).json({
            success: false,
            error: 'Phone, password, and bloodBankId are required'
        });
    }

    try {
        // Check if blood bank exists
        const bankResult = await query('SELECT * FROM blood_banks WHERE id = $1', [bloodBankId]);
        if (bankResult.rows.length === 0) {
            return res.status(404).json({
                success: false,
                error: 'Blood bank not found'
            });
        }

        const bank = bankResult.rows[0];

        // Check if already registered (has password)
        if (bank.password_hash) {
            return res.status(400).json({
                success: false,
                error: 'This blood bank already has an account'
            });
        }

        // Hash password
        const saltRounds = 10;
        const passwordHash = await bcrypt.hash(password, saltRounds);

        // Update blood bank with phone and password (use the phone field that already exists)
        await query(
            'UPDATE blood_banks SET phone = $1, password_hash = $2 WHERE id = $3',
            [phone, passwordHash, bloodBankId]
        );

        // Get updated bank
        const updatedBank = await query('SELECT id, name, phone FROM blood_banks WHERE id = $1', [bloodBankId]);
        const token = generateToken(updatedBank.rows[0]);

        res.status(201).json({
            success: true,
            message: 'Account created successfully',
            token,
            bank: {
                id: updatedBank.rows[0].id,
                name: updatedBank.rows[0].name,
                phone: updatedBank.rows[0].phone
            }
        });
    } catch (err) {
        console.error('Registration error:', err);
        res.status(500).json({ success: false, error: 'Registration failed' });
    }
});

// POST /api/auth/bank/login - Login with phone and password
router.post('/bank/login', async (req, res) => {
    const { phone, password } = req.body;

    if (!phone || !password) {
        return res.status(400).json({
            success: false,
            error: 'Phone and password are required'
        });
    }

    try {
        // Find blood bank by phone
        const result = await query(
            'SELECT id, name, phone, password_hash FROM blood_banks WHERE phone = $1',
            [phone]
        );

        if (result.rows.length === 0) {
            return res.status(401).json({
                success: false,
                error: 'Invalid phone or password'
            });
        }

        const bank = result.rows[0];

        // Check if account has password set
        if (!bank.password_hash) {
            return res.status(401).json({
                success: false,
                error: 'Account not registered. Please register first.'
            });
        }

        // Compare password
        const validPassword = await bcrypt.compare(password, bank.password_hash);
        if (!validPassword) {
            return res.status(401).json({
                success: false,
                error: 'Invalid phone or password'
            });
        }

        // Generate token
        const token = generateToken(bank);

        res.json({
            success: true,
            message: 'Login successful',
            token,
            bank: {
                id: bank.id,
                name: bank.name,
                phone: bank.phone
            }
        });
    } catch (err) {
        console.error('Login error:', err);
        res.status(500).json({ success: false, error: 'Login failed' });
    }
});

// GET /api/auth/bank/me - Get current logged-in bank info
router.get('/bank/me', async (req, res) => {
    const authHeader = req.headers.authorization;

    if (!authHeader || !authHeader.startsWith('Bearer ')) {
        return res.status(401).json({ success: false, error: 'No token provided' });
    }

    const jwt = require('jsonwebtoken');
    const { JWT_SECRET } = require('../middleware/authMiddleware');

    try {
        const token = authHeader.split(' ')[1];
        const decoded = jwt.verify(token, JWT_SECRET);

        const result = await query(
            'SELECT id, name, email, address, city, phone, is_open FROM blood_banks WHERE id = $1',
            [decoded.id]
        );

        if (result.rows.length === 0) {
            return res.status(404).json({ success: false, error: 'Bank not found' });
        }

        res.json({
            success: true,
            bank: result.rows[0]
        });
    } catch (err) {
        return res.status(401).json({ success: false, error: 'Invalid token' });
    }
});

module.exports = router;
