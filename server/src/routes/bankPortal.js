const express = require('express');
const router = express.Router();
const { query } = require('../config/database');
const { authenticateBank } = require('../middleware/authMiddleware');
const { sendStatusUpdate } = require('../services/whatsapp');

// All routes require authentication
router.use(authenticateBank);

// GET /api/bank/stats - Get dashboard stats for this blood bank only
router.get('/stats', async (req, res) => {
    const bankId = req.bank.id;

    try {
        // Reservation stats for this bank
        const reservationStats = await query(`
            SELECT 
                COUNT(*) as total,
                COUNT(*) FILTER (WHERE status = 'pending') as pending,
                COUNT(*) FILTER (WHERE status = 'confirmed') as confirmed,
                COUNT(*) FILTER (WHERE status = 'completed') as completed,
                COUNT(*) FILTER (WHERE status = 'cancelled') as cancelled
            FROM reservations
            WHERE blood_bank_id = $1
        `, [bankId]);

        // Blood units for this bank
        const inventoryStats = await query(`
            SELECT blood_type, units_available
            FROM blood_inventory
            WHERE blood_bank_id = $1
        `, [bankId]);

        const totalUnits = inventoryStats.rows.reduce((sum, row) => sum + row.units_available, 0);

        // Recent reservations
        const recentReservations = await query(`
            SELECT id, patient_name, blood_type, units_needed, status, created_at
            FROM reservations
            WHERE blood_bank_id = $1
            ORDER BY created_at DESC
            LIMIT 5
        `, [bankId]);

        res.json({
            success: true,
            data: {
                reservations: reservationStats.rows[0],
                inventory: {
                    total: totalUnits,
                    byType: inventoryStats.rows.map(r => ({
                        type: r.blood_type,
                        units: r.units_available
                    }))
                },
                recentActivity: recentReservations.rows.map(r => ({
                    id: r.id,
                    patientName: r.patient_name,
                    bloodType: r.blood_type,
                    unitsNeeded: r.units_needed,
                    status: r.status,
                    createdAt: r.created_at
                }))
            }
        });
    } catch (err) {
        console.error('Error fetching bank stats:', err);
        res.status(500).json({ success: false, error: 'Failed to fetch stats' });
    }
});

// GET /api/bank/reservations - Get reservations for this blood bank
router.get('/reservations', async (req, res) => {
    const bankId = req.bank.id;

    try {
        const result = await query(`
            SELECT 
                id, patient_name, whatsapp_number, blood_type, 
                units_needed, urgency_level, status, additional_notes, 
                created_at, updated_at
            FROM reservations
            WHERE blood_bank_id = $1
            ORDER BY created_at DESC
        `, [bankId]);

        const reservations = result.rows.map(r => ({
            id: r.id,
            patientName: r.patient_name,
            whatsappNumber: r.whatsapp_number,
            bloodType: r.blood_type,
            unitsNeeded: r.units_needed,
            urgencyLevel: r.urgency_level,
            status: r.status,
            additionalNotes: r.additional_notes,
            createdAt: r.created_at,
            updatedAt: r.updated_at
        }));

        res.json({ success: true, count: reservations.length, data: reservations });
    } catch (err) {
        console.error('Error fetching reservations:', err);
        res.status(500).json({ success: false, error: 'Failed to fetch reservations' });
    }
});

// PUT /api/bank/reservations/:id/status - Update reservation status
router.put('/reservations/:id/status', async (req, res) => {
    const bankId = req.bank.id;
    const { status } = req.body;
    const reservationId = req.params.id;
    const validStatuses = ['pending', 'confirmed', 'completed', 'cancelled'];

    if (!validStatuses.includes(status)) {
        return res.status(400).json({
            success: false,
            error: `Invalid status. Must be one of: ${validStatuses.join(', ')}`
        });
    }

    try {
        // Verify this reservation belongs to this bank
        const checkResult = await query(
            'SELECT * FROM reservations WHERE id = $1 AND blood_bank_id = $2',
            [reservationId, bankId]
        );

        if (checkResult.rows.length === 0) {
            return res.status(404).json({
                success: false,
                error: 'Reservation not found or not authorized'
            });
        }

        // Update status
        const result = await query(`
            UPDATE reservations 
            SET status = $1, updated_at = CURRENT_TIMESTAMP 
            WHERE id = $2 AND blood_bank_id = $3
            RETURNING *
        `, [status, reservationId, bankId]);

        const reservation = result.rows[0];

        // Auto-deduct inventory when completed
        if (status === 'completed') {
            await query(`
                UPDATE blood_inventory 
                SET units_available = GREATEST(0, units_available - $1),
                    last_updated = CURRENT_TIMESTAMP
                WHERE blood_bank_id = $2 AND blood_type = $3
            `, [reservation.units_needed, bankId, reservation.blood_type]);
        }

        // Get bank name for WhatsApp
        const bankInfo = await query('SELECT name FROM blood_banks WHERE id = $1', [bankId]);

        // Send WhatsApp notification
        let whatsappStatus = 'not_sent';
        try {
            await sendStatusUpdate(
                reservation.whatsapp_number,
                reservation.patient_name,
                status,
                bankInfo.rows[0].name
            );
            whatsappStatus = 'sent';
        } catch (err) {
            console.log('WhatsApp notification not sent:', err.message);
        }

        res.json({
            success: true,
            message: `Status updated to ${status}`,
            whatsappNotification: whatsappStatus,
            data: result.rows[0]
        });
    } catch (err) {
        console.error('Error updating status:', err);
        res.status(500).json({ success: false, error: 'Failed to update status' });
    }
});

// GET /api/bank/inventory - Get inventory for this blood bank
router.get('/inventory', async (req, res) => {
    const bankId = req.bank.id;

    try {
        const result = await query(`
            SELECT 
                blood_type, 
                units_available,
                collection_date,
                expiry_date,
                (expiry_date - CURRENT_DATE) as days_left
            FROM blood_inventory
            WHERE blood_bank_id = $1
            ORDER BY blood_type
        `, [bankId]);

        const inventory = result.rows.map(row => ({
            blood_type: row.blood_type,
            units_available: row.units_available,
            collection_date: row.collection_date,
            expiry_date: row.expiry_date,
            days_left: row.days_left,
            expiry_status: row.days_left <= 0 ? 'expired' :
                row.days_left <= 3 ? 'critical' :
                    row.days_left <= 7 ? 'warning' : 'normal'
        }));

        res.json({ success: true, data: inventory });
    } catch (err) {
        console.error('Error fetching inventory:', err);
        res.status(500).json({ success: false, error: 'Failed to fetch inventory' });
    }
});

// PUT /api/bank/inventory - Update inventory for this blood bank
router.put('/inventory', async (req, res) => {
    const bankId = req.bank.id;
    const { bloodType, units, collectionDate } = req.body;

    if (!bloodType || units === undefined) {
        return res.status(400).json({
            success: false,
            error: 'bloodType and units are required'
        });
    }

    try {
        // Calculate expiry date (42 days from collection)
        const collection = collectionDate ? new Date(collectionDate) : new Date();
        const expiry = new Date(collection);
        expiry.setDate(expiry.getDate() + 42);

        // Upsert inventory with expiry tracking
        const result = await query(`
            INSERT INTO blood_inventory (blood_bank_id, blood_type, units_available, collection_date, expiry_date)
            VALUES ($1, $2, $3, $4, $5)
            ON CONFLICT (blood_bank_id, blood_type) 
            DO UPDATE SET 
                units_available = $3, 
                collection_date = $4,
                expiry_date = $5,
                last_updated = CURRENT_TIMESTAMP
            RETURNING *
        `, [bankId, bloodType, units, collection.toISOString().split('T')[0], expiry.toISOString().split('T')[0]]);

        res.json({
            success: true,
            message: 'Inventory updated',
            data: result.rows[0]
        });
    } catch (err) {
        console.error('Error updating inventory:', err);
        res.status(500).json({ success: false, error: 'Failed to update inventory' });
    }
});

// GET /api/bank/expiring - Get expiring blood alerts
router.get('/expiring', async (req, res) => {
    const bankId = req.bank.id;

    try {
        // Get blood expiring within 7 days
        const result = await query(`
            SELECT 
                blood_type,
                units_available,
                expiry_date,
                (expiry_date - CURRENT_DATE) as days_left
            FROM blood_inventory
            WHERE blood_bank_id = $1 
            AND expiry_date <= CURRENT_DATE + INTERVAL '7 days'
            AND units_available > 0
            ORDER BY expiry_date ASC
        `, [bankId]);

        const expiring = result.rows.map(row => ({
            bloodType: row.blood_type,
            units: row.units_available,
            expiryDate: row.expiry_date,
            daysLeft: row.days_left,
            status: row.days_left <= 0 ? 'expired' : row.days_left <= 3 ? 'critical' : 'warning'
        }));

        // Separate by status
        const expired = expiring.filter(e => e.status === 'expired');
        const critical = expiring.filter(e => e.status === 'critical');
        const warning = expiring.filter(e => e.status === 'warning');

        res.json({
            success: true,
            data: {
                expired,
                critical,
                warning,
                total: expiring.length
            }
        });
    } catch (err) {
        console.error('Error fetching expiring blood:', err);
        res.status(500).json({ success: false, error: 'Failed to fetch expiring blood' });
    }
});

// PUT /api/bank/profile - Update blood bank profile
router.put('/profile', async (req, res) => {
    const bankId = req.bank.id;
    const { name, address, city, phone, isOpen } = req.body;

    try {
        const result = await query(`
            UPDATE blood_banks
            SET 
                name = COALESCE($1, name),
                address = COALESCE($2, address),
                city = COALESCE($3, city),
                phone = COALESCE($4, phone),
                is_open = COALESCE($5, is_open),
                updated_at = CURRENT_TIMESTAMP
            WHERE id = $6
            RETURNING id, name, address, city, phone, email, is_open
        `, [name, address, city, phone, isOpen, bankId]);

        res.json({
            success: true,
            message: 'Profile updated',
            data: result.rows[0]
        });
    } catch (err) {
        console.error('Error updating profile:', err);
        res.status(500).json({ success: false, error: 'Failed to update profile' });
    }
});

module.exports = router;
