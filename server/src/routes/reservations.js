const express = require('express');
const router = express.Router();
const { query } = require('../config/database');
const { sendReservationConfirmation, sendStatusUpdate, getStatus } = require('../services/whatsapp');

// POST /api/reservations - Create new reservation
router.post('/', async (req, res) => {
    const {
        patientName,
        whatsappNumber,
        bloodType,
        unitsNeeded,
        urgencyLevel,
        additionalNotes,
        bloodBankId
    } = req.body;

    // Validation
    if (!patientName || !whatsappNumber || !bloodType || !unitsNeeded || !bloodBankId) {
        return res.status(400).json({
            success: false,
            error: 'Missing required fields: patientName, whatsappNumber, bloodType, unitsNeeded, bloodBankId'
        });
    }

    try {
        // Set expiry to 24 hours from now
        const expiresAt = new Date(Date.now() + 24 * 60 * 60 * 1000);

        const result = await query(`
            INSERT INTO reservations 
            (patient_name, whatsapp_number, blood_type, units_needed, urgency_level, additional_notes, blood_bank_id, expires_at)
            VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
            RETURNING *
        `, [
            patientName,
            `+91${whatsappNumber}`,
            bloodType,
            unitsNeeded,
            urgencyLevel || 'normal',
            additionalNotes || '',
            bloodBankId,
            expiresAt
        ]);

        // Get blood bank name
        const bankResult = await query('SELECT name FROM blood_banks WHERE id = $1', [bloodBankId]);
        const bloodBankName = bankResult.rows[0]?.name || 'Unknown';

        const reservation = {
            id: result.rows[0].id,
            patientName: result.rows[0].patient_name,
            whatsappNumber: result.rows[0].whatsapp_number,
            bloodType: result.rows[0].blood_type,
            unitsNeeded: result.rows[0].units_needed,
            urgencyLevel: result.rows[0].urgency_level,
            additionalNotes: result.rows[0].additional_notes,
            bloodBankId: result.rows[0].blood_bank_id,
            bloodBankName,
            status: result.rows[0].status,
            createdAt: result.rows[0].created_at,
            expiresAt: result.rows[0].expires_at
        };

        console.log('New reservation created:', reservation);

        // Send WhatsApp notification
        let whatsappSent = false;
        if (getStatus().isReady) {
            whatsappSent = await sendReservationConfirmation(reservation);
        } else {
            console.log('⚠️ WhatsApp not connected. Notification not sent.');
        }

        res.status(201).json({
            success: true,
            message: 'Reservation created successfully',
            whatsappNotification: whatsappSent ? 'sent' : 'not_sent',
            data: reservation
        });
    } catch (err) {
        console.error('Error creating reservation:', err);
        res.status(500).json({ success: false, error: 'Failed to create reservation' });
    }
});

// GET /api/reservations - Get all reservations
router.get('/', async (req, res) => {
    try {
        const result = await query(`
            SELECT r.*, bb.name as blood_bank_name
            FROM reservations r
            LEFT JOIN blood_banks bb ON r.blood_bank_id = bb.id
            ORDER BY r.created_at DESC
        `);

        const reservations = result.rows.map(row => ({
            id: row.id,
            patientName: row.patient_name,
            whatsappNumber: row.whatsapp_number,
            bloodType: row.blood_type,
            unitsNeeded: row.units_needed,
            urgencyLevel: row.urgency_level,
            additionalNotes: row.additional_notes,
            bloodBankId: row.blood_bank_id,
            bloodBankName: row.blood_bank_name,
            status: row.status,
            createdAt: row.created_at,
            expiresAt: row.expires_at,
            updatedAt: row.updated_at
        }));

        res.json({
            success: true,
            count: reservations.length,
            data: reservations
        });
    } catch (err) {
        console.error('Error fetching reservations:', err);
        res.status(500).json({ success: false, error: 'Failed to fetch reservations' });
    }
});

// GET /api/reservations/:id - Get single reservation
router.get('/:id', async (req, res) => {
    try {
        const result = await query(`
            SELECT r.*, bb.name as blood_bank_name
            FROM reservations r
            LEFT JOIN blood_banks bb ON r.blood_bank_id = bb.id
            WHERE r.id = $1
        `, [req.params.id]);

        if (result.rows.length === 0) {
            return res.status(404).json({
                success: false,
                error: 'Reservation not found'
            });
        }

        const row = result.rows[0];
        const reservation = {
            id: row.id,
            patientName: row.patient_name,
            whatsappNumber: row.whatsapp_number,
            bloodType: row.blood_type,
            unitsNeeded: row.units_needed,
            urgencyLevel: row.urgency_level,
            additionalNotes: row.additional_notes,
            bloodBankId: row.blood_bank_id,
            bloodBankName: row.blood_bank_name,
            status: row.status,
            createdAt: row.created_at,
            expiresAt: row.expires_at,
            updatedAt: row.updated_at
        };

        res.json({
            success: true,
            data: reservation
        });
    } catch (err) {
        console.error('Error fetching reservation:', err);
        res.status(500).json({ success: false, error: 'Failed to fetch reservation' });
    }
});

// PUT /api/reservations/:id/status - Update reservation status
router.put('/:id/status', async (req, res) => {
    const { status } = req.body;
    const validStatuses = ['pending', 'confirmed', 'completed', 'cancelled'];

    if (!validStatuses.includes(status)) {
        return res.status(400).json({
            success: false,
            error: `Invalid status. Must be one of: ${validStatuses.join(', ')}`
        });
    }

    try {
        const result = await query(`
            UPDATE reservations 
            SET status = $1, updated_at = CURRENT_TIMESTAMP
            WHERE id = $2
            RETURNING *
        `, [status, req.params.id]);

        if (result.rows.length === 0) {
            return res.status(404).json({
                success: false,
                error: 'Reservation not found'
            });
        }

        // Send WhatsApp notification for status update
        let whatsappSent = false;
        if (getStatus().isReady) {
            whatsappSent = await sendStatusUpdate(result.rows[0], status);
        }

        res.json({
            success: true,
            message: `Reservation status updated to ${status}`,
            whatsappNotification: whatsappSent ? 'sent' : 'not_sent',
            data: result.rows[0]
        });
    } catch (err) {
        console.error('Error updating reservation:', err);
        res.status(500).json({ success: false, error: 'Failed to update reservation' });
    }
});

// DELETE /api/reservations/:id - Cancel reservation
router.delete('/:id', async (req, res) => {
    try {
        const result = await query(`
            UPDATE reservations 
            SET status = 'cancelled', updated_at = CURRENT_TIMESTAMP
            WHERE id = $1
            RETURNING *
        `, [req.params.id]);

        if (result.rows.length === 0) {
            return res.status(404).json({
                success: false,
                error: 'Reservation not found'
            });
        }

        res.json({
            success: true,
            message: 'Reservation cancelled',
            data: result.rows[0]
        });
    } catch (err) {
        console.error('Error cancelling reservation:', err);
        res.status(500).json({ success: false, error: 'Failed to cancel reservation' });
    }
});

module.exports = router;
