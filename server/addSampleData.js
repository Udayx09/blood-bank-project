const { Pool } = require('pg');
const bcrypt = require('bcrypt');

const pool = new Pool({
    user: 'postgres',
    host: 'localhost',
    database: 'bloodbank',
    password: '1234',
    port: 5432
});

async function addSampleData() {
    try {
        // Hash the password 'test123'
        const passwordHash = await bcrypt.hash('test123', 10);

        // Insert sample blood banks with pre-registered accounts
        const banks = [
            { name: 'City Blood Bank', address: '123 Main Street', city: 'Mumbai', phone: '+91 9876543210', email: 'city@test.com' },
            { name: 'LifeSaver Blood Center', address: '456 Health Avenue', city: 'Delhi', phone: '+91 9876543211', email: 'lifesaver@test.com' },
            { name: 'RedCross Blood Bank', address: '789 Care Road', city: 'Bangalore', phone: '+91 9876543212', email: 'redcross@test.com' }
        ];

        for (const bank of banks) {
            const result = await pool.query(
                'INSERT INTO blood_banks (name, address, city, phone, email, password_hash, is_open, rating) VALUES ($1, $2, $3, $4, $5, $6, $7, $8) RETURNING id',
                [bank.name, bank.address, bank.city, bank.phone, bank.email, passwordHash, true, 4.5]
            );
            const bankId = result.rows[0].id;

            // Add inventory for each blood type
            const bloodTypes = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'];
            for (const type of bloodTypes) {
                const units = Math.floor(Math.random() * 20) + 5;
                await pool.query(
                    'INSERT INTO blood_inventory (blood_bank_id, blood_type, units_available) VALUES ($1, $2, $3)',
                    [bankId, type, units]
                );
            }
            console.log('Created: ' + bank.name + ' (Email: ' + bank.email + ')');
        }

        console.log('\n✅ Sample data added successfully!');
        console.log('\n📋 Login Credentials (Password for all: test123):');
        console.log('   1. Email: city@test.com');
        console.log('   2. Email: lifesaver@test.com');
        console.log('   3. Email: redcross@test.com');
    } catch (err) {
        console.error('Error:', err.message);
    } finally {
        pool.end();
    }
}

addSampleData();
