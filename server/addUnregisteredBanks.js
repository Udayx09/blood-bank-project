const { Pool } = require('pg');

const pool = new Pool({
    user: 'postgres',
    host: 'localhost',
    database: 'bloodbank',
    password: '1234',
    port: 5432
});

async function addUnregisteredBanks() {
    try {
        const banks = [
            { name: 'Hope Blood Bank', address: '100 New Street', city: 'Chennai', phone: '+91 9876500001' },
            { name: 'Sunrise Blood Center', address: '200 East Road', city: 'Pune', phone: '+91 9876500002' }
        ];

        for (const bank of banks) {
            const result = await pool.query(
                'INSERT INTO blood_banks (name, address, city, phone, is_open, rating) VALUES ($1, $2, $3, $4, true, 4.5) RETURNING id',
                [bank.name, bank.address, bank.city, bank.phone]
            );
            console.log('Created (unregistered): ' + bank.name + ' - ID: ' + result.rows[0].id);
        }

        console.log('\n✅ These banks are now available for registration!');
        console.log('Go to Bank Portal > Register and select one of these banks.');
    } catch (err) {
        console.error('Error:', err.message);
    } finally {
        pool.end();
    }
}

addUnregisteredBanks();
