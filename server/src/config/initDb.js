/**
 * Database initialization script
 * Run this to create the bloodbank database and tables
 */
const { Client } = require('pg');

const DB_NAME = 'bloodbank';

// Connect to default postgres database first to create our database
const createDatabase = async () => {
    const client = new Client({
        host: process.env.DB_HOST || 'localhost',
        port: process.env.DB_PORT || 5432,
        user: process.env.DB_USER || 'postgres',
        password: process.env.DB_PASSWORD || '1234',
        database: 'postgres' // Connect to default database
    });

    try {
        await client.connect();
        console.log('Connected to PostgreSQL');

        // Check if database exists
        const checkDb = await client.query(
            `SELECT 1 FROM pg_database WHERE datname = $1`, [DB_NAME]
        );

        if (checkDb.rows.length === 0) {
            // Create database
            await client.query(`CREATE DATABASE ${DB_NAME}`);
            console.log(`✅ Database '${DB_NAME}' created successfully`);
        } else {
            console.log(`📦 Database '${DB_NAME}' already exists`);
        }

    } catch (err) {
        console.error('Error creating database:', err.message);
    } finally {
        await client.end();
    }
};

// Create tables in bloodbank database
const createTables = async () => {
    const client = new Client({
        host: process.env.DB_HOST || 'localhost',
        port: process.env.DB_PORT || 5432,
        user: process.env.DB_USER || 'postgres',
        password: process.env.DB_PASSWORD || '1234',
        database: DB_NAME
    });

    try {
        await client.connect();
        console.log(`Connected to '${DB_NAME}' database`);

        // Create blood_banks table
        await client.query(`
            CREATE TABLE IF NOT EXISTS blood_banks (
                id SERIAL PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                address VARCHAR(500) NOT NULL,
                city VARCHAR(100) NOT NULL,
                phone VARCHAR(20) NOT NULL,
                email VARCHAR(255),
                rating DECIMAL(2,1) DEFAULT 4.5,
                is_open BOOLEAN DEFAULT true,
                latitude DECIMAL(10, 8),
                longitude DECIMAL(11, 8),
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        `);
        console.log('✅ Table blood_banks created');

        // Create blood_inventory table
        await client.query(`
            CREATE TABLE IF NOT EXISTS blood_inventory (
                id SERIAL PRIMARY KEY,
                blood_bank_id INTEGER REFERENCES blood_banks(id) ON DELETE CASCADE,
                blood_type VARCHAR(5) NOT NULL,
                units_available INTEGER DEFAULT 0,
                collection_date DATE DEFAULT CURRENT_DATE,
                expiry_date DATE DEFAULT (CURRENT_DATE + INTERVAL '42 days'),
                last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                UNIQUE(blood_bank_id, blood_type)
            )
        `);
        console.log('✅ Table blood_inventory created');

        // Create reservations table
        await client.query(`
            CREATE TABLE IF NOT EXISTS reservations (
                id SERIAL PRIMARY KEY,
                patient_name VARCHAR(255) NOT NULL,
                whatsapp_number VARCHAR(20) NOT NULL,
                blood_type VARCHAR(5) NOT NULL,
                units_needed INTEGER NOT NULL,
                urgency_level VARCHAR(20) DEFAULT 'normal',
                additional_notes TEXT,
                blood_bank_id INTEGER REFERENCES blood_banks(id),
                status VARCHAR(20) DEFAULT 'pending',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                expires_at TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        `);
        console.log('✅ Table reservations created');

        console.log('\n🎉 All tables created successfully!');

    } catch (err) {
        console.error('Error creating tables:', err.message);
    } finally {
        await client.end();
    }
};

// Insert sample data
const insertSampleData = async () => {
    const client = new Client({
        host: process.env.DB_HOST || 'localhost',
        port: process.env.DB_PORT || 5432,
        user: process.env.DB_USER || 'postgres',
        password: process.env.DB_PASSWORD || '1234',
        database: DB_NAME
    });

    try {
        await client.connect();

        // Check if data already exists
        const existing = await client.query('SELECT COUNT(*) FROM blood_banks');
        if (parseInt(existing.rows[0].count) > 0) {
            console.log('📦 Sample data already exists, skipping...');
            return;
        }

        // Insert blood banks
        const bloodBanks = [
            ['City General Hospital Blood Bank', '123 Healthcare Ave', 'Bloodville', '+1-234-567-8901', 'bloodbank@citygeneral.com', 4.8, true, 12.9716, 77.5946],
            ['Red Cross Blood Center', '456 Donation Street', 'Healthtown', '+1-234-567-8902', 'contact@redcross-bloodcenter.com', 4.9, true, 12.9816, 77.6046],
            ['LifeSaver Blood Bank', '789 Medical Center Blvd', 'Careville', '+1-234-567-8903', 'info@lifesaverblood.org', 4.7, true, 12.9616, 77.5846],
            ['Community Hospital Blood Services', '321 Wellness Road', 'Hopedale', '+1-234-567-8904', 'blood@communityhospital.com', 4.5, false, 12.9516, 77.5746],
            ['Metro Blood Donation Center', '555 Central Plaza', 'Metropolis', '+1-234-567-8905', 'donate@metroblood.org', 4.9, true, 12.9916, 77.6146]
        ];

        for (const bank of bloodBanks) {
            await client.query(`
                INSERT INTO blood_banks (name, address, city, phone, email, rating, is_open, latitude, longitude)
                VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
            `, bank);
        }
        console.log('✅ Blood banks inserted');

        // Insert blood inventory
        const bloodTypes = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'];
        const inventoryData = [
            [1, { 'A+': 25, 'A-': 10, 'B+': 15, 'O+': 30, 'O-': 8 }],
            [2, { 'A+': 40, 'B+': 20, 'B-': 5, 'AB+': 12, 'AB-': 3, 'O+': 35 }],
            [3, { 'A+': 18, 'A-': 7, 'B+': 22, 'B-': 4, 'AB+': 9, 'O+': 28, 'O-': 12 }],
            [4, { 'A+': 15, 'B+': 8, 'O+': 20, 'O-': 6 }],
            [5, { 'A+': 50, 'A-': 15, 'B+': 35, 'B-': 10, 'AB+': 20, 'AB-': 8, 'O+': 45, 'O-': 18 }]
        ];

        for (const [bankId, inventory] of inventoryData) {
            for (const [bloodType, units] of Object.entries(inventory)) {
                await client.query(`
                    INSERT INTO blood_inventory (blood_bank_id, blood_type, units_available)
                    VALUES ($1, $2, $3)
                `, [bankId, bloodType, units]);
            }
        }
        console.log('✅ Blood inventory inserted');

        console.log('\n🎉 Sample data inserted successfully!');

    } catch (err) {
        console.error('Error inserting sample data:', err.message);
    } finally {
        await client.end();
    }
};

// Run initialization
const init = async () => {
    console.log('\n🚀 Starting database initialization...\n');
    await createDatabase();
    await createTables();
    await insertSampleData();
    console.log('\n✅ Database initialization complete!\n');
};

// Run if called directly
if (require.main === module) {
    require('dotenv').config({ path: require('path').join(__dirname, '../../.env') });
    init().catch(console.error);
}

module.exports = { init, createDatabase, createTables, insertSampleData };
