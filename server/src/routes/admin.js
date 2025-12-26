/**
 * Admin Routes
 * Provides dashboard statistics and admin operations
 */
const express = require('express');
const router = express.Router();
const { query } = require('../config/database');
const { getStatus } = require('../services/whatsapp');
const { checkAllLowStock, DEFAULT_THRESHOLD } = require('../services/lowStock');

// GET /api/admin/stats - Get dashboard statistics
router.get('/stats', async (req, res) => {
    try {
        // Get reservation stats
        const reservationStats = await query(`
            SELECT 
                COUNT(*) as total,
                COUNT(*) FILTER (WHERE status = 'pending') as pending,
                COUNT(*) FILTER (WHERE status = 'confirmed') as confirmed,
                COUNT(*) FILTER (WHERE status = 'completed') as completed,
                COUNT(*) FILTER (WHERE status = 'cancelled') as cancelled
            FROM reservations
        `);

        // Get blood bank count
        const bloodBankCount = await query(`
            SELECT COUNT(*) as total FROM blood_banks
        `);

        // Get total blood units
        const bloodUnits = await query(`
            SELECT COALESCE(SUM(units_available), 0) as total FROM blood_inventory
        `);

        // Get recent reservations (last 5)
        const recentReservations = await query(`
            SELECT r.*, bb.name as blood_bank_name
            FROM reservations r
            LEFT JOIN blood_banks bb ON r.blood_bank_id = bb.id
            ORDER BY r.created_at DESC
            LIMIT 5
        `);

        // Get WhatsApp status
        const whatsappStatus = getStatus();

        // Get blood type distribution
        const bloodTypeStats = await query(`
            SELECT blood_type, SUM(units_available) as units
            FROM blood_inventory
            GROUP BY blood_type
            ORDER BY blood_type
        `);

        // Get low stock alerts
        const lowStockAlerts = await checkAllLowStock();

        res.json({
            success: true,
            data: {
                reservations: {
                    total: parseInt(reservationStats.rows[0].total),
                    pending: parseInt(reservationStats.rows[0].pending),
                    confirmed: parseInt(reservationStats.rows[0].confirmed),
                    completed: parseInt(reservationStats.rows[0].completed),
                    cancelled: parseInt(reservationStats.rows[0].cancelled)
                },
                bloodBanks: {
                    total: parseInt(bloodBankCount.rows[0].total)
                },
                bloodUnits: {
                    total: parseInt(bloodUnits.rows[0].total),
                    byType: bloodTypeStats.rows.map(row => ({
                        type: row.blood_type,
                        units: parseInt(row.units)
                    }))
                },
                whatsapp: {
                    connected: whatsappStatus.isReady,
                    hasQR: whatsappStatus.hasQR
                },
                lowStockAlerts: lowStockAlerts,
                lowStockThreshold: DEFAULT_THRESHOLD,
                recentActivity: recentReservations.rows.map(row => ({
                    id: row.id,
                    type: 'reservation',
                    patientName: row.patient_name,
                    bloodType: row.blood_type,
                    status: row.status,
                    bloodBankName: row.blood_bank_name,
                    createdAt: row.created_at
                }))
            }
        });
    } catch (err) {
        console.error('Error fetching admin stats:', err);
        res.status(500).json({ success: false, error: 'Failed to fetch statistics' });
    }
});

// GET /api/admin/low-stock - Get all low stock alerts
router.get('/low-stock', async (req, res) => {
    try {
        const alerts = await checkAllLowStock();
        res.json({
            success: true,
            threshold: DEFAULT_THRESHOLD,
            count: alerts.length,
            data: alerts
        });
    } catch (err) {
        console.error('Error fetching low stock alerts:', err);
        res.status(500).json({ success: false, error: 'Failed to fetch low stock alerts' });
    }
});

// GET /api/admin/inventory - Get all inventory grouped by blood bank
router.get('/inventory', async (req, res) => {
    try {
        const result = await query(`
            SELECT 
                bb.id as blood_bank_id,
                bb.name as blood_bank_name,
                bi.blood_type,
                bi.units_available
            FROM blood_banks bb
            LEFT JOIN blood_inventory bi ON bb.id = bi.blood_bank_id
            ORDER BY bb.name, bi.blood_type
        `);

        // Group by blood bank
        const grouped = {};
        result.rows.forEach(row => {
            if (!grouped[row.blood_bank_id]) {
                grouped[row.blood_bank_id] = {
                    id: row.blood_bank_id,
                    name: row.blood_bank_name,
                    inventory: []
                };
            }
            if (row.blood_type) {
                grouped[row.blood_bank_id].inventory.push({
                    bloodType: row.blood_type,
                    units: row.units_available
                });
            }
        });

        res.json({
            success: true,
            data: Object.values(grouped)
        });
    } catch (err) {
        console.error('Error fetching inventory:', err);
        res.status(500).json({ success: false, error: 'Failed to fetch inventory' });
    }
});

// PUT /api/admin/inventory - Update inventory for a blood bank
router.put('/inventory', async (req, res) => {
    const { bloodBankId, bloodType, units } = req.body;

    if (!bloodBankId || !bloodType || units === undefined) {
        return res.status(400).json({
            success: false,
            error: 'bloodBankId, bloodType, and units are required'
        });
    }

    try {
        const result = await query(`
            INSERT INTO blood_inventory (blood_bank_id, blood_type, units_available)
            VALUES ($1, $2, $3)
            ON CONFLICT (blood_bank_id, blood_type) 
            DO UPDATE SET units_available = $3, last_updated = CURRENT_TIMESTAMP
            RETURNING *
        `, [bloodBankId, bloodType, units]);

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

module.exports = router;


