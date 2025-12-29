/**
 * WhatsApp Microservice
 * Standalone Node.js service for WhatsApp notifications
 * Runs on port 3001 and is called by Spring Boot backend
 * Uses min-instances=1 on Cloud Run for session persistence
 */

require('dotenv').config();
const express = require('express');
const cors = require('cors');
const { Client, LocalAuth } = require('whatsapp-web.js');
const qrcode = require('qrcode-terminal');

const app = express();
const PORT = process.env.PORT || 3001;

// WhatsApp client state
let client = null;
let isReady = false;
let qrCodeData = null;

// Middleware
app.use(cors());
app.use(express.json());

// Request logging
app.use((req, res, next) => {
    console.log(`${new Date().toISOString()} - ${req.method} ${req.path}`);
    next();
});

/**
 * Initialize WhatsApp client with cloud-compatible settings
 */
const initWhatsApp = () => {
    console.log('\n📱 Initializing WhatsApp client...');

    // Check for cloud environment Chromium path
    const executablePath = process.env.PUPPETEER_EXECUTABLE_PATH ||
        process.env.CHROME_BIN ||
        '/usr/bin/chromium';

    console.log('🔧 Using Chromium at:', executablePath);
    console.log('💾 Using LocalAuth for session storage');

    client = new Client({
        authStrategy: new LocalAuth({
            dataPath: './.wwebjs_auth'
        }),
        puppeteer: {
            headless: true,
            executablePath: process.env.PUPPETEER_EXECUTABLE_PATH ? executablePath : undefined,
            args: [
                '--no-sandbox',
                '--disable-setuid-sandbox',
                '--disable-dev-shm-usage',
                '--disable-accelerated-2d-canvas',
                '--no-first-run',
                '--no-zygote',
                '--disable-gpu',
                '--single-process',
                '--disable-extensions',
                '--disable-background-networking',
                '--disable-default-apps',
                '--disable-sync',
                '--disable-translate',
                '--hide-scrollbars',
                '--metrics-recording-only',
                '--mute-audio',
                '--safebrowsing-disable-auto-update',
                '--ignore-certificate-errors',
                '--ignore-ssl-errors',
                '--ignore-certificate-errors-spki-list'
            ],
            timeout: 120000  // 2 minute timeout for initialization
        }
    });

    // QR Code event
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

    // Auth failure - attempt to recover by clearing session
    client.on('auth_failure', async (msg) => {
        console.error('❌ WhatsApp authentication failed:', msg);
        isReady = false;
        console.log('🔄 Will retry on next restart...');
    });

    // Disconnected - attempt to reconnect
    client.on('disconnected', async (reason) => {
        console.log('📱 WhatsApp disconnected:', reason);
        isReady = false;
        console.log('🔄 Attempting to reconnect in 10 seconds...');
        setTimeout(() => {
            console.log('🔄 Reinitializing WhatsApp client...');
            initWhatsApp();
        }, 10000);
    });

    // Loading screen event - useful for debugging initialization
    client.on('loading_screen', (percent, message) => {
        console.log(`⏳ Loading: ${percent}% - ${message}`);
    });

    // Initialize with timeout protection
    const initTimeout = setTimeout(() => {
        if (!isReady) {
            console.log('⚠️ Initialization taking longer than expected...');
            console.log('💡 This is normal for first-time setup or cloud deployments');
        }
    }, 30000);

    client.initialize()
        .then(() => {
            clearTimeout(initTimeout);
            console.log('✅ WhatsApp client initialization started');
        })
        .catch(err => {
            clearTimeout(initTimeout);
            console.error('❌ Failed to initialize WhatsApp client:', err.message);
            console.log('🔄 Will retry in 30 seconds...');
            setTimeout(() => initWhatsApp(), 30000);
        });

    return client;
};

/**
 * Format phone number for WhatsApp
 */
const formatPhoneNumber = (phone) => {
    let cleaned = phone.replace(/\D/g, '');
    if (cleaned.startsWith('91') && cleaned.length > 10) {
        cleaned = cleaned.substring(2);
    }
    return `91${cleaned}@c.us`;
};

/**
 * Send WhatsApp message
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

// ================== API Endpoints ==================

/**
 * GET /api/whatsapp/status - Get connection status
 */
app.get('/api/whatsapp/status', (req, res) => {
    res.json({
        success: true,
        isReady,
        hasQR: !!qrCodeData
    });
});

/**
 * GET /api/whatsapp/qr - Get QR code page (HTML)
 */
app.get('/api/whatsapp/qr', (req, res) => {
    // Return HTML page with status
    const html = `
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>WhatsApp Status - Blood Bank</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            color: #fff;
        }
        .container {
            background: rgba(255,255,255,0.05);
            border-radius: 20px;
            padding: 40px;
            text-align: center;
            max-width: 500px;
            backdrop-filter: blur(10px);
            border: 1px solid rgba(255,255,255,0.1);
        }
        .status-icon {
            font-size: 80px;
            margin-bottom: 20px;
        }
        .title {
            font-size: 28px;
            margin-bottom: 10px;
        }
        .subtitle {
            color: #aaa;
            margin-bottom: 30px;
        }
        .status-connected {
            background: linear-gradient(135deg, #00b894, #00cec9);
            padding: 20px 40px;
            border-radius: 12px;
            font-size: 20px;
            font-weight: bold;
        }
        .status-waiting {
            background: linear-gradient(135deg, #fdcb6e, #f39c12);
            padding: 20px 40px;
            border-radius: 12px;
            font-size: 20px;
            font-weight: bold;
            color: #333;
        }
        .qr-container {
            background: #fff;
            padding: 20px;
            border-radius: 12px;
            margin: 20px 0;
            display: inline-block;
        }
        .qr-code {
            font-family: monospace;
            font-size: 6px;
            line-height: 1;
            white-space: pre;
            color: #000;
        }
        .instructions {
            color: #aaa;
            font-size: 14px;
            margin-top: 20px;
        }
        .refresh-btn {
            margin-top: 20px;
            padding: 10px 30px;
            background: rgba(255,255,255,0.1);
            border: 1px solid rgba(255,255,255,0.3);
            border-radius: 8px;
            color: #fff;
            cursor: pointer;
            font-size: 16px;
        }
        .refresh-btn:hover {
            background: rgba(255,255,255,0.2);
        }
    </style>
    <meta http-equiv="refresh" content="${isReady ? '30' : '5'}">
</head>
<body>
    <div class="container">
        ${isReady ? `
            <div class="status-icon">✅</div>
            <h1 class="title">WhatsApp Connected</h1>
            <p class="subtitle">Messages will be sent automatically</p>
            <div class="status-connected">
                📱 Ready to Send Messages
            </div>
            <p class="instructions">
                Your WhatsApp is linked and ready.<br>
                Reservation notifications will be sent automatically.
            </p>
        ` : qrCodeData ? `
            <div class="status-icon">📱</div>
            <h1 class="title">Scan QR Code</h1>
            <p class="subtitle">Open WhatsApp → Settings → Linked Devices → Link a Device</p>
            <div class="qr-container">
                <img src="https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=${encodeURIComponent(qrCodeData)}" alt="QR Code" />
            </div>
            <p class="instructions">
                Scan this code with your phone's WhatsApp app<br>
                Page will auto-refresh when connected
            </p>
        ` : `
            <div class="status-icon">⏳</div>
            <h1 class="title">Initializing...</h1>
            <p class="subtitle">Please wait while WhatsApp client starts</p>
            <div class="status-waiting">
                🔄 Loading WhatsApp...
            </div>
            <p class="instructions">
                The QR code will appear here shortly.<br>
                Page refreshes automatically.
            </p>
        `}
        <button class="refresh-btn" onclick="location.reload()">🔄 Refresh</button>
    </div>
</body>
</html>
    `;
    res.type('html').send(html);
});

/**
 * POST /api/whatsapp/send-confirmation - Send reservation confirmation
 */
app.post('/api/whatsapp/send-confirmation', async (req, res) => {
    const { phoneNumber, patientName, bloodType, unitsNeeded, bloodBankName, reservationId } = req.body;

    if (!phoneNumber || !patientName) {
        return res.status(400).json({
            success: false,
            error: 'phoneNumber and patientName are required'
        });
    }

    const message = `🏥 *Blood Bank Notification*

✅ *Reservation Confirmed!*

👤 Patient: ${patientName}
🩸 Blood Type: ${bloodType}
📦 Units: ${unitsNeeded}
🏥 Blood Bank: ${bloodBankName}

🔖 Reservation ID: #${reservationId}
⏰ Valid for 24 hours

_Thank you for using Blood Bank Service!_`;

    const sent = await sendMessage(phoneNumber, message);

    res.json({
        success: sent,
        message: sent ? 'Confirmation sent' : 'Failed to send confirmation'
    });
});

/**
 * POST /api/whatsapp/send-status-update - Send status update
 */
app.post('/api/whatsapp/send-status-update', async (req, res) => {
    const { phoneNumber, patientName, status, bloodBankName } = req.body;

    if (!phoneNumber || !status) {
        return res.status(400).json({
            success: false,
            error: 'phoneNumber and status are required'
        });
    }

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

${emoji} *Status Update*

${statusText}

👤 Patient: ${patientName}
🏥 Blood Bank: ${bloodBankName}`;

    const sent = await sendMessage(phoneNumber, message);

    res.json({
        success: sent,
        message: sent ? 'Status update sent' : 'Failed to send status update'
    });
});

/**
 * POST /api/whatsapp/send-donor-otp - Send OTP to donor
 */
app.post('/api/whatsapp/send-donor-otp', async (req, res) => {
    const { phoneNumber, otp } = req.body;

    if (!phoneNumber || !otp) {
        return res.status(400).json({
            success: false,
            error: 'phoneNumber and otp are required'
        });
    }

    const message = `🩸 *Blood Bank - OTP Verification*

Your OTP is: *${otp}*

This code is valid for 10 minutes.
Do not share this code with anyone.

_If you didn't request this, please ignore._`;

    const sent = await sendMessage(phoneNumber, message);

    res.json({
        success: sent,
        message: sent ? 'OTP sent' : 'Failed to send OTP'
    });
});

/**
 * POST /api/whatsapp/send-donor-welcome - Send welcome message to new donor
 */
app.post('/api/whatsapp/send-donor-welcome', async (req, res) => {
    const { phoneNumber, donorName } = req.body;

    if (!phoneNumber || !donorName) {
        return res.status(400).json({
            success: false,
            error: 'phoneNumber and donorName are required'
        });
    }

    const message = `🎉 *Welcome to Blood Bank, ${donorName}!*

Thank you for registering as a blood donor! 🩸

You're now part of a community that saves lives.

*What happens next:*
• We'll notify you when you're eligible to donate
• You'll receive alerts for blood shortages in your area
• You can track your donation history in the app

_Every drop counts. Thank you for being a hero!_ ❤️`;

    const sent = await sendMessage(phoneNumber, message);

    res.json({
        success: sent,
        message: sent ? 'Welcome message sent' : 'Failed to send welcome message'
    });
});

/**
 * POST /api/whatsapp/send-eligibility-reminder - Remind donor they're eligible
 */
app.post('/api/whatsapp/send-eligibility-reminder', async (req, res) => {
    const { phoneNumber, donorName } = req.body;

    if (!phoneNumber || !donorName) {
        return res.status(400).json({
            success: false,
            error: 'phoneNumber and donorName are required'
        });
    }

    const message = `🩸 *Blood Donation Reminder*

Hi ${donorName}! 👋

Great news - *you're now eligible to donate blood again!* 🎉

It's been 90 days since your last donation, and your body has fully recovered.

*Your donation can save up to 3 lives!*

Ready to donate? Visit your nearest blood bank today.

_Thank you for being a lifesaver!_ ❤️`;

    const sent = await sendMessage(phoneNumber, message);

    res.json({
        success: sent,
        message: sent ? 'Eligibility reminder sent' : 'Failed to send reminder'
    });
});

/**
 * POST /api/whatsapp/send-blood-shortage-alert - Alert donors about blood shortage
 */
app.post('/api/whatsapp/send-blood-shortage-alert', async (req, res) => {
    const { phoneNumber, donorName, bloodType, city, bloodBankName } = req.body;

    if (!phoneNumber || !bloodType) {
        return res.status(400).json({
            success: false,
            error: 'phoneNumber and bloodType are required'
        });
    }

    const name = donorName || 'Donor';
    const location = city || 'your area';
    const bank = bloodBankName || 'local blood banks';

    const message = `🚨 *URGENT: Blood Shortage Alert*

Hi ${name},

*${bloodType} blood is critically needed* in ${location}!

${bank} urgently needs donors.

If you're available and eligible to donate, please visit the blood bank as soon as possible.

*Your donation can save a life today!* 🩸

_Reply STOP to unsubscribe from alerts._`;

    const sent = await sendMessage(phoneNumber, message);

    res.json({
        success: sent,
        message: sent ? 'Blood shortage alert sent' : 'Failed to send alert'
    });
});

/**
 * POST /api/whatsapp/send - Send custom message
 */
app.post('/api/whatsapp/send', async (req, res) => {
    const { phoneNumber, message } = req.body;

    if (!phoneNumber || !message) {
        return res.status(400).json({
            success: false,
            error: 'phoneNumber and message are required'
        });
    }

    const sent = await sendMessage(phoneNumber, message);

    res.json({
        success: sent,
        message: sent ? 'Message sent' : 'Failed to send message'
    });
});

// Health check
app.get('/api/health', (req, res) => {
    res.json({
        status: 'ok',
        service: 'WhatsApp Microservice',
        whatsappConnected: isReady,
        timestamp: new Date().toISOString()
    });
});

/**
 * POST /api/whatsapp/send-donation-request - Send donation request from bank to donor
 */
app.post('/api/whatsapp/send-donation-request', async (req, res) => {
    const { phoneNumber, donorName, bloodBankName, city, bankPhone, bankAddress } = req.body;

    if (!phoneNumber || !donorName || !bloodBankName) {
        return res.status(400).json({
            success: false,
            error: 'phoneNumber, donorName, and bloodBankName are required'
        });
    }

    const message = `🩸 *Blood Donation Request*

Dear *${donorName}*,

We hope this message finds you well! 🙏

*${bloodBankName}* kindly requests your help with a blood donation.

━━━━━━━━━━━━━━━
📍 *Location:* ${city || 'Your area'}
📞 *Phone:* ${bankPhone || 'Contact us'}
🏥 *Address:* ${bankAddress || 'Visit us'}
━━━━━━━━━━━━━━━

Your generous donation can save up to 3 precious lives!

If you are available and willing to donate, please reply with *INTERESTED* and we will get in touch with you.

Thank you so much for considering this request. You are a true lifesaver! ❤️

_With gratitude,_
_${bloodBankName}_`;

    const sent = await sendMessage(phoneNumber, message);

    res.json({
        success: sent,
        message: sent ? 'Donation request sent' : 'Failed to send request'
    });
});

// Initialize WhatsApp and start server
initWhatsApp();

app.listen(PORT, () => {
    console.log(`
╔════════════════════════════════════════╗
║   WhatsApp Microservice Started        ║
╠════════════════════════════════════════╣
║  🚀 Server running on port ${PORT}         ║
║  📍 http://localhost:${PORT}              ║
║  📱 Waiting for WhatsApp connection... ║
╚════════════════════════════════════════╝
    `);
});

