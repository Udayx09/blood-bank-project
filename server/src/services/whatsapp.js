/**
 * WhatsApp Notification Service
 * Single sender - all messages sent from one WhatsApp number
 */
const { Client, LocalAuth } = require('whatsapp-web.js');
const qrcode = require('qrcode-terminal');

let client = null;
let isReady = false;
let qrCodeData = null;

/**
 * Initialize WhatsApp client
 */
const initWhatsApp = () => {
    console.log('\n📱 Initializing WhatsApp client...');

    client = new Client({
        authStrategy: new LocalAuth({
            dataPath: './.wwebjs_auth'
        }),
        puppeteer: {
            headless: true,
            args: [
                '--no-sandbox',
                '--disable-setuid-sandbox',
                '--disable-dev-shm-usage',
                '--disable-accelerated-2d-canvas',
                '--no-first-run',
                '--no-zygote',
                '--disable-gpu'
            ]
        }
    });

    // QR Code event - display in terminal
    client.on('qr', (qr) => {
        qrCodeData = qr;
        console.log('\n╔════════════════════════════════════════════════════╗');
        console.log('║     📱 SCAN THIS QR CODE WITH YOUR WHATSAPP        ║');
        console.log('║     Settings > Linked Devices > Link a Device      ║');
        console.log('╚════════════════════════════════════════════════════╝\n');
        qrcode.generate(qr, { small: true });
    });

    // Ready event
    client.on('ready', () => {
        isReady = true;
        qrCodeData = null;
        console.log('\n╔════════════════════════════════════════════════════╗');
        console.log('║     ✅ WHATSAPP CONNECTED SUCCESSFULLY!            ║');
        console.log('║     Messages will now be sent automatically        ║');
        console.log('╚════════════════════════════════════════════════════╝\n');
    });

    // Authenticated event
    client.on('authenticated', () => {
        console.log('🔐 WhatsApp authenticated');
    });

    // Auth failure
    client.on('auth_failure', (msg) => {
        console.error('❌ WhatsApp authentication failed:', msg);
        isReady = false;
    });

    // Disconnected
    client.on('disconnected', (reason) => {
        console.log('📱 WhatsApp disconnected:', reason);
        isReady = false;
    });

    // Initialize
    client.initialize().catch(err => {
        console.error('❌ Failed to initialize WhatsApp client:', err.message);
    });

    return client;
};

/**
 * Format phone number for WhatsApp
 * @param {string} phone - Phone number (with or without country code)
 * @returns {string} - Formatted WhatsApp ID
 */
const formatPhoneNumber = (phone) => {
    // Remove all non-digits
    let cleaned = phone.replace(/\D/g, '');

    // If starts with +91, remove it (we'll add it back)
    if (cleaned.startsWith('91') && cleaned.length > 10) {
        cleaned = cleaned.substring(2);
    }

    // Add India country code
    return `91${cleaned}@c.us`;
};

/**
 * Send WhatsApp message
 * @param {string} phoneNumber - Recipient's phone number
 * @param {string} message - Message to send
 * @returns {Promise<boolean>} - Success status
 */
const sendMessage = async (phoneNumber, message) => {
    if (!isReady || !client) {
        console.log('⚠️ WhatsApp not ready. Message not sent.');
        return false;
    }

    try {
        const chatId = formatPhoneNumber(phoneNumber);
        await client.sendMessage(chatId, message);
        console.log(`✅ WhatsApp message sent to ${phoneNumber}`);
        return true;
    } catch (error) {
        console.error(`❌ Failed to send WhatsApp message:`, error.message);
        return false;
    }
};

/**
 * Send reservation confirmation
 * @param {Object} reservation - Reservation details
 */
const sendReservationConfirmation = async (reservation) => {
    const message = `🏥 *Blood Bank Notification*

✅ *Reservation Confirmed!*

👤 Patient: ${reservation.patientName}
🩸 Blood Type: ${reservation.bloodType}
📦 Units: ${reservation.unitsNeeded}
🏥 Blood Bank: ${reservation.bloodBankName}

🔖 Reservation ID: #${reservation.id}
⏰ Valid for 24 hours

_Thank you for using Blood Bank Service!_`;

    return sendMessage(reservation.whatsappNumber, message);
};

/**
 * Send status update notification
 * @param {Object} reservation - Reservation details
 * @param {string} status - New status
 */
const sendStatusUpdate = async (reservation, status) => {
    let emoji, statusText;

    switch (status) {
        case 'confirmed':
            emoji = '🎉';
            statusText = 'Your reservation has been confirmed by the blood bank!';
            break;
        case 'completed':
            emoji = '✅';
            statusText = 'Your reservation has been completed. Thank you!';
            break;
        case 'cancelled':
            emoji = '❌';
            statusText = 'Your reservation has been cancelled. Please contact the blood bank for assistance.';
            break;
        default:
            emoji = 'ℹ️';
            statusText = `Status updated to: ${status}`;
    }

    const message = `🏥 *Blood Bank Update*

${emoji} *Reservation #${reservation.id}*

${statusText}

👤 Patient: ${reservation.patient_name || reservation.patientName}
🩸 Blood Type: ${reservation.blood_type || reservation.bloodType}`;

    const phone = reservation.whatsapp_number || reservation.whatsappNumber;
    return sendMessage(phone, message);
};

/**
 * Get WhatsApp connection status
 */
const getStatus = () => ({
    isReady,
    hasQR: !!qrCodeData
});

/**
 * Get QR code data (for API endpoint if needed)
 */
const getQRCode = () => qrCodeData;

module.exports = {
    initWhatsApp,
    sendMessage,
    sendReservationConfirmation,
    sendStatusUpdate,
    getStatus,
    getQRCode
};
