const express = require('express');
const router = express.Router();
const { query } = require('../config/database');

// GET /api/blood-banks - Get all blood banks with inventory
router.get('/', async (req, res) => {
    try {
        const result = await query(`
            SELECT 
                bb.*,
                COALESCE(json_object_agg(bi.blood_type, bi.units_available) FILTER (WHERE bi.blood_type IS NOT NULL), '{}') as available_units,
                COALESCE(array_agg(bi.blood_type) FILTER (WHERE bi.blood_type IS NOT NULL), ARRAY[]::varchar[]) as blood_types
            FROM blood_banks bb
            LEFT JOIN blood_inventory bi ON bb.id = bi.blood_bank_id
            GROUP BY bb.id
            ORDER BY bb.id
        `);

        const bloodBanks = result.rows.map(row => ({
            id: row.id,
            name: row.name,
            address: row.address,
            city: row.city,
            phone: row.phone,
            email: row.email,
            rating: parseFloat(row.rating) || 0,
            isOpen: row.is_open,
            hasAccount: !!row.password_hash, // true if already registered
            bloodTypes: (row.blood_types || []).filter(t => t !== null),
            availableUnits: row.available_units || {},
            location: {
                lat: parseFloat(row.latitude) || 0,
                lng: parseFloat(row.longitude) || 0
            }
        }));

        res.json({
            success: true,
            count: bloodBanks.length,
            data: bloodBanks
        });
    } catch (err) {
        console.error('Error fetching blood banks:', err);
        res.status(500).json({ success: false, error: 'Failed to fetch blood banks' });
    }
});

// GET /api/blood-banks/search - Search by blood type
router.get('/search', async (req, res) => {
    const { bloodType } = req.query;

    if (!bloodType) {
        return res.status(400).json({
            success: false,
            error: 'Blood type is required. Use ?bloodType=A+'
        });
    }

    try {
        const result = await query(`
            SELECT 
                bb.*,
                bi.units_available,
                json_object_agg(bi2.blood_type, bi2.units_available) as available_units,
                array_agg(DISTINCT bi2.blood_type) as blood_types
            FROM blood_banks bb
            INNER JOIN blood_inventory bi ON bb.id = bi.blood_bank_id AND bi.blood_type = $1
            LEFT JOIN blood_inventory bi2 ON bb.id = bi2.blood_bank_id
            GROUP BY bb.id, bi.units_available
            ORDER BY bi.units_available DESC
        `, [bloodType]);

        const bloodBanks = result.rows.map(row => ({
            id: row.id,
            name: row.name,
            address: row.address,
            city: row.city,
            phone: row.phone,
            email: row.email,
            rating: parseFloat(row.rating),
            isOpen: row.is_open,
            bloodTypes: row.blood_types.filter(t => t !== null),
            availableUnits: row.available_units || {},
            unitsAvailable: row.units_available,
            distance: `${(Math.random() * 10 + 1).toFixed(1)} km`, // Placeholder - calculate from location
            location: {
                lat: parseFloat(row.latitude),
                lng: parseFloat(row.longitude)
            }
        }));

        res.json({
            success: true,
            bloodType,
            count: bloodBanks.length,
            data: bloodBanks
        });
    } catch (err) {
        console.error('Error searching blood banks:', err);
        res.status(500).json({ success: false, error: 'Failed to search blood banks' });
    }
});

// GET /api/blood-banks/types - Get all blood types
router.get('/types', async (req, res) => {
    res.json({
        success: true,
        data: ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-']
    });
});

// GET /api/blood-banks/:id - Get single blood bank
router.get('/:id', async (req, res) => {
    try {
        const result = await query(`
            SELECT 
                bb.*,
                json_object_agg(bi.blood_type, bi.units_available) as available_units,
                array_agg(bi.blood_type) as blood_types
            FROM blood_banks bb
            LEFT JOIN blood_inventory bi ON bb.id = bi.blood_bank_id
            WHERE bb.id = $1
            GROUP BY bb.id
        `, [req.params.id]);

        if (result.rows.length === 0) {
            return res.status(404).json({
                success: false,
                error: 'Blood bank not found'
            });
        }

        const row = result.rows[0];
        const bloodBank = {
            id: row.id,
            name: row.name,
            address: row.address,
            city: row.city,
            phone: row.phone,
            email: row.email,
            rating: parseFloat(row.rating),
            isOpen: row.is_open,
            bloodTypes: row.blood_types.filter(t => t !== null),
            availableUnits: row.available_units || {},
            location: {
                lat: parseFloat(row.latitude),
                lng: parseFloat(row.longitude)
            }
        };

        res.json({
            success: true,
            data: bloodBank
        });
    } catch (err) {
        console.error('Error fetching blood bank:', err);
        res.status(500).json({ success: false, error: 'Failed to fetch blood bank' });
    }
});

// POST /api/blood-banks - Create new blood bank
router.post('/', async (req, res) => {
    const { name, address, city, phone, email, rating, isOpen, latitude, longitude } = req.body;

    if (!name || !address || !city || !phone) {
        return res.status(400).json({
            success: false,
            error: 'Missing required fields: name, address, city, phone'
        });
    }

    try {
        const result = await query(`
            INSERT INTO blood_banks (name, address, city, phone, email, rating, is_open, latitude, longitude)
            VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
            RETURNING *
        `, [
            name,
            address,
            city,
            phone,
            email || null,
            rating || 4.5,
            isOpen !== undefined ? isOpen : true,
            latitude || null,
            longitude || null
        ]);

        res.status(201).json({
            success: true,
            message: 'Blood bank created successfully',
            data: result.rows[0]
        });
    } catch (err) {
        console.error('Error creating blood bank:', err);
        res.status(500).json({ success: false, error: 'Failed to create blood bank' });
    }
});

// PUT /api/blood-banks/:id - Update blood bank
router.put('/:id', async (req, res) => {
    const { name, address, city, phone, email, rating, isOpen, latitude, longitude } = req.body;

    try {
        const result = await query(`
            UPDATE blood_banks 
            SET 
                name = COALESCE($1, name),
                address = COALESCE($2, address),
                city = COALESCE($3, city),
                phone = COALESCE($4, phone),
                email = COALESCE($5, email),
                rating = COALESCE($6, rating),
                is_open = COALESCE($7, is_open),
                latitude = COALESCE($8, latitude),
                longitude = COALESCE($9, longitude),
                updated_at = CURRENT_TIMESTAMP
            WHERE id = $10
            RETURNING *
        `, [name, address, city, phone, email, rating, isOpen, latitude, longitude, req.params.id]);

        if (result.rows.length === 0) {
            return res.status(404).json({
                success: false,
                error: 'Blood bank not found'
            });
        }

        res.json({
            success: true,
            message: 'Blood bank updated successfully',
            data: result.rows[0]
        });
    } catch (err) {
        console.error('Error updating blood bank:', err);
        res.status(500).json({ success: false, error: 'Failed to update blood bank' });
    }
});

// PUT /api/blood-banks/:id/toggle - Toggle open/closed status
router.put('/:id/toggle', async (req, res) => {
    try {
        const result = await query(`
            UPDATE blood_banks 
            SET is_open = NOT is_open, updated_at = CURRENT_TIMESTAMP
            WHERE id = $1
            RETURNING *
        `, [req.params.id]);

        if (result.rows.length === 0) {
            return res.status(404).json({
                success: false,
                error: 'Blood bank not found'
            });
        }

        res.json({
            success: true,
            message: `Blood bank is now ${result.rows[0].is_open ? 'open' : 'closed'}`,
            data: result.rows[0]
        });
    } catch (err) {
        console.error('Error toggling blood bank status:', err);
        res.status(500).json({ success: false, error: 'Failed to toggle status' });
    }
});

// DELETE /api/blood-banks/:id - Delete blood bank
router.delete('/:id', async (req, res) => {
    try {
        const bankId = req.params.id;

        // First check if blood bank exists
        const checkResult = await query('SELECT * FROM blood_banks WHERE id = $1', [bankId]);
        if (checkResult.rows.length === 0) {
            return res.status(404).json({
                success: false,
                error: 'Blood bank not found'
            });
        }

        // Delete related records first (cascade delete)
        await query('DELETE FROM reservations WHERE blood_bank_id = $1', [bankId]);
        await query('DELETE FROM blood_inventory WHERE blood_bank_id = $1', [bankId]);

        // Now delete the blood bank
        const result = await query('DELETE FROM blood_banks WHERE id = $1 RETURNING *', [bankId]);

        res.json({
            success: true,
            message: 'Blood bank deleted successfully',
            data: result.rows[0]
        });
    } catch (err) {
        console.error('Error deleting blood bank:', err);
        res.status(500).json({ success: false, error: 'Failed to delete blood bank' });
    }
});

module.exports = router;
