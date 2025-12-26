const { Client } = require('pg');

const client = new Client({
    host: 'localhost',
    port: 5432,
    database: 'bloodbank',
    user: 'postgres',
    password: '1234'
});

async function addUnregisteredBanks() {
    try {
        await client.connect();
        console.log('Connected to database');

        // Add blood banks WITHOUT password_hash (so they can be registered)
        const result = await client.query(`
            INSERT INTO blood_banks (name, address, city, phone, is_open) VALUES 
            ('New Hope Blood Bank', 'MG Road', 'Mumbai', '9111222333', true),
            ('City Care Blood Center', 'Station Road', 'Delhi', '9444555666', true),
            ('Life Line Blood Bank', 'Park Street', 'Kolkata', '9777888999', true)
            RETURNING id, name, city
        `);

        console.log('Added unregistered blood banks:');
        result.rows.forEach(row => {
            console.log(`  - ${row.name} (${row.city}) - ID: ${row.id}`);
        });

        await client.end();
        console.log('\nDone! These blood banks are now available for registration.');
    } catch (err) {
        console.error('Error:', err);
        await client.end();
    }
}

addUnregisteredBanks();
