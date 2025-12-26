// Test script to lower inventory and trigger low stock alert
const { query } = require('./src/config/database');

async function testLowStock() {
    try {
        console.log('🔍 Checking current inventory...');

        // Get current inventory
        const inventory = await query('SELECT * FROM blood_inventory ORDER BY blood_bank_id, blood_type');
        console.log('Current inventory:', inventory.rows);

        if (inventory.rows.length === 0) {
            console.log('⚠️ No inventory found. Adding some test data...');
            // Add low inventory for testing
            await query(`
                INSERT INTO blood_inventory (blood_bank_id, blood_type, units_available)
                VALUES (1, 'O-', 3), (1, 'AB-', 2)
                ON CONFLICT (blood_bank_id, blood_type) 
                DO UPDATE SET units_available = EXCLUDED.units_available
            `);
            console.log('✅ Added low stock test data');
        } else {
            // Update first blood type to low stock
            const firstItem = inventory.rows[0];
            console.log(`📉 Lowering ${firstItem.blood_type} to 3 units...`);
            await query(
                'UPDATE blood_inventory SET units_available = 3 WHERE id = $1',
                [firstItem.id]
            );
            console.log('✅ Inventory lowered!');
        }

        // Check low stock
        const lowStock = await query(`
            SELECT bi.*, bb.name as bank_name
            FROM blood_inventory bi
            JOIN blood_banks bb ON bi.blood_bank_id = bb.id
            WHERE bi.units_available <= 5
        `);
        console.log('🚨 Low stock items:', lowStock.rows);

        console.log('\n✅ Test complete! Refresh the admin dashboard to see alerts.');
        process.exit(0);
    } catch (err) {
        console.error('Error:', err);
        process.exit(1);
    }
}

testLowStock();
