const { query } = require('../config/database');
const { sendMessage, getStatus } = require('./whatsapp');

// Default low stock threshold
const DEFAULT_THRESHOLD = 5;

// Check low stock for a specific blood bank
const checkLowStock = async (bloodBankId) => {
    try {
        const result = await query(`
            SELECT 
                bi.blood_type,
                bi.units_available,
                bb.name as blood_bank_name,
                bb.email as blood_bank_email
            FROM blood_inventory bi
            JOIN blood_banks bb ON bi.blood_bank_id = bb.id
            WHERE bi.blood_bank_id = $1 AND bi.units_available <= $2
        `, [bloodBankId, DEFAULT_THRESHOLD]);

        return result.rows.map(row => ({
            bloodType: row.blood_type,
            unitsAvailable: row.units_available,
            bloodBankName: row.blood_bank_name,
            threshold: DEFAULT_THRESHOLD
        }));
    } catch (err) {
        console.error('Error checking low stock:', err);
        return [];
    }
};

// Check low stock for all blood banks
const checkAllLowStock = async () => {
    try {
        const result = await query(`
            SELECT 
                bi.blood_type,
                bi.units_available,
                bb.id as blood_bank_id,
                bb.name as blood_bank_name,
                bb.email as blood_bank_email
            FROM blood_inventory bi
            JOIN blood_banks bb ON bi.blood_bank_id = bb.id
            WHERE bi.units_available <= $1
            ORDER BY bi.units_available ASC
        `, [DEFAULT_THRESHOLD]);

        return result.rows.map(row => ({
            bloodBankId: row.blood_bank_id,
            bloodBankName: row.blood_bank_name,
            bloodType: row.blood_type,
            unitsAvailable: row.units_available,
            threshold: DEFAULT_THRESHOLD,
            isCritical: row.units_available <= 2
        }));
    } catch (err) {
        console.error('Error checking all low stock:', err);
        return [];
    }
};

// Send low stock alert via WhatsApp
const sendLowStockAlert = async (phoneNumber, bloodBankName, lowStockItems) => {
    const status = getStatus();
    if (!status.connected) {
        console.log('WhatsApp not connected, cannot send low stock alert');
        return false;
    }

    const itemsList = lowStockItems.map(item =>
        `• ${item.bloodType}: ${item.unitsAvailable} units`
    ).join('\n');

    const message = `🚨 *LOW STOCK ALERT* 🚨

Dear ${bloodBankName},

The following blood types are running low:

${itemsList}

Please restock soon to avoid shortages.

- Blood Bank Management System`;

    try {
        await sendMessage(phoneNumber, message);
        console.log(`Low stock alert sent to ${bloodBankName}`);
        return true;
    } catch (err) {
        console.error('Failed to send low stock alert:', err);
        return false;
    }
};

module.exports = {
    checkLowStock,
    checkAllLowStock,
    sendLowStockAlert,
    DEFAULT_THRESHOLD
};
