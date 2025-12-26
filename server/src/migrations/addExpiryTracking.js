/**
 * Migration: Add expiry tracking to blood_inventory
 * Run this to add collection_date and expiry_date columns
 */
const { Client } = require('pg');
require('dotenv').config({ path: require('path').join(__dirname, '../.env') });

const migrate = async () => {
    const client = new Client({
        host: process.env.DB_HOST || 'localhost',
        port: process.env.DB_PORT || 5432,
        user: process.env.DB_USER || 'postgres',
        password: process.env.DB_PASSWORD || '1234',
        database: process.env.DB_NAME || 'bloodbank'
    });

    try {
        await client.connect();
        console.log('🔌 Connected to database');

        // Check if columns already exist
        const checkColumns = await client.query(`
            SELECT column_name 
            FROM information_schema.columns 
            WHERE table_name = 'blood_inventory' 
            AND column_name IN ('collection_date', 'expiry_date')
        `);

        if (checkColumns.rows.length >= 2) {
            console.log('✅ Expiry columns already exist, skipping migration');
            return;
        }

        // Add collection_date column
        console.log('Adding collection_date column...');
        await client.query(`
            ALTER TABLE blood_inventory 
            ADD COLUMN IF NOT EXISTS collection_date DATE DEFAULT CURRENT_DATE
        `);

        // Add expiry_date column (42 days after collection for whole blood)
        console.log('Adding expiry_date column...');
        await client.query(`
            ALTER TABLE blood_inventory 
            ADD COLUMN IF NOT EXISTS expiry_date DATE DEFAULT (CURRENT_DATE + INTERVAL '42 days')
        `);

        // Update existing records to have expiry dates
        console.log('Updating existing records with sample expiry dates...');

        // Set varied expiry dates for demo purposes
        await client.query(`
            UPDATE blood_inventory SET
                collection_date = CURRENT_DATE - INTERVAL '35 days',
                expiry_date = CURRENT_DATE + INTERVAL '7 days'
            WHERE id = 1
        `);

        await client.query(`
            UPDATE blood_inventory SET
                collection_date = CURRENT_DATE - INTERVAL '40 days',
                expiry_date = CURRENT_DATE + INTERVAL '2 days'
            WHERE id = 2
        `);

        await client.query(`
            UPDATE blood_inventory SET
                collection_date = CURRENT_DATE - INTERVAL '43 days',
                expiry_date = CURRENT_DATE - INTERVAL '1 day'
            WHERE id = 3
        `);

        // Rest have normal expiry dates
        await client.query(`
            UPDATE blood_inventory SET
                collection_date = CURRENT_DATE - INTERVAL '10 days',
                expiry_date = CURRENT_DATE + INTERVAL '32 days'
            WHERE collection_date IS NULL OR id > 3
        `);

        console.log('✅ Migration completed successfully!');
        console.log('');
        console.log('Sample data created:');
        console.log('  - 1 unit expiring in 7 days (warning)');
        console.log('  - 1 unit expiring in 2 days (critical)');
        console.log('  - 1 unit already expired (for demo)');
        console.log('  - Rest have normal 32 days left');

    } catch (err) {
        console.error('❌ Migration failed:', err.message);
    } finally {
        await client.end();
    }
};

// Run migration
migrate();
