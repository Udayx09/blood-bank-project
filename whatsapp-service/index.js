/**
 * WhatsApp Cloud API Service
 * Uses official Meta WhatsApp Business API
 * No Puppeteer/browser needed - simple HTTP requests
 */

require('dotenv').config();
const express = require('express');
const cors = require('cors');
const axios = require('axios');

const app = express();
const PORT = process.env.PORT || 3001;

// WhatsApp Cloud API Configuration
const WHATSAPP_API_URL = 'https://graph.facebook.com/v18.0';
const PHONE_NUMBER_ID = process.env.WHATSAPP_PHONE_NUMBER_ID;
const ACCESS_TOKEN = process.env.WHATSAPP_ACCESS_TOKEN;

// Service state
let isConfigured = !!(PHONE_NUMBER_ID && ACCESS_TOKEN);

// Middleware
app.use(cors());
app.use(express.json());

// Request logging
app.use((req, res, next) => {
    console.log(`${new Date().toISOString()} - ${req.method} ${req.path}`);
    next();
});

/**
 * Format phone number for WhatsApp API
 * WhatsApp requires country code without + sign
 */
const formatPhoneNumber = (phone) => {
    let cleaned = phone.replace(/\D/g, '');
    // Add India country code if not present
    if (cleaned.length === 10) {
        cleaned = '91' + cleaned;
    }
    return cleaned;
};

/**
 * Send WhatsApp message via Cloud API
 */
const sendMessage = async (phoneNumber, message) => {
    if (!isConfigured) {
        console.log('⚠️ WhatsApp not configured. Message logged:', message.substring(0, 50) + '...');
        return { success: true, demo: true, message: 'Demo mode - message logged' };
    }

    try {
        const response = await axios.post(
            `${WHATSAPP_API_URL}/${PHONE_NUMBER_ID}/messages`,
            {
                messaging_product: 'whatsapp',
                to: formatPhoneNumber(phoneNumber),
                type: 'text',
                text: { body: message }
            },
            {
                headers: {
                    'Authorization': `Bearer ${ACCESS_TOKEN}`,
                    'Content-Type': 'application/json'
                }
            }
        );

        console.log(`✅ WhatsApp message sent to ${phoneNumber}`);
        return { success: true, messageId: response.data.messages?.[0]?.id };
    } catch (error) {
        console.error('❌ WhatsApp API error:', error.response?.data || error.message);
        return { success: false, error: error.response?.data?.error?.message || error.message };
    }
};

/**
 * Send template message (required for first contact)
 */
const sendTemplateMessage = async (phoneNumber, templateName, languageCode = 'en') => {
    if (!isConfigured) {
        console.log('⚠️ WhatsApp not configured. Template logged:', templateName);
        return { success: true, demo: true };
    }

    try {
        const response = await axios.post(
            `${WHATSAPP_API_URL}/${PHONE_NUMBER_ID}/messages`,
            {
                messaging_product: 'whatsapp',
                to: formatPhoneNumber(phoneNumber),
                type: 'template',
                template: {
                    name: templateName,
                    language: { code: languageCode }
                }
            },
            {
                headers: {
                    'Authorization': `Bearer ${ACCESS_TOKEN}`,
                    'Content-Type': 'application/json'
                }
            }
        );

        console.log(`✅ Template message sent to ${phoneNumber}`);
        return { success: true, messageId: response.data.messages?.[0]?.id };
    } catch (error) {
        console.error('❌ Template error:', error.response?.data || error.message);
        return { success: false, error: error.response?.data?.error?.message || error.message };
    }
};

// ================== API Endpoints ==================

/**
 * GET /api/whatsapp/status - Get service status
 */
app.get('/api/whatsapp/status', (req, res) => {
    res.json({
        success: true,
        isReady: isConfigured,
        hasQR: false,  // No QR needed for Cloud API
        mode: isConfigured ? 'cloud_api' : 'demo',
        message: isConfigured ? 'WhatsApp Cloud API ready' : 'Demo mode - configure credentials to enable'
    });
});

/**
 * GET /api/whatsapp/qr - QR code page (for backward compatibility)
 */
app.get('/api/whatsapp/qr', (req, res) => {
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
        .status-icon { font-size: 80px; margin-bottom: 20px; }
        .title { font-size: 28px; margin-bottom: 10px; }
        .subtitle { color: #aaa; margin-bottom: 30px; }
        .status-box {
            background: linear-gradient(135deg, ${isConfigured ? '#00b894, #00cec9' : '#fdcb6e, #f39c12'});
            padding: 20px 40px;
            border-radius: 12px;
            font-size: 18px;
            font-weight: bold;
            ${isConfigured ? '' : 'color: #333;'}
        }
        .info { color: #aaa; font-size: 14px; margin-top: 20px; line-height: 1.6; }
    </style>
</head>
<body>
    <div class="container">
        <div class="status-icon">${isConfigured ? '✅' : '🔧'}</div>
        <h1 class="title">${isConfigured ? 'WhatsApp Cloud API Ready' : 'Demo Mode Active'}</h1>
        <p class="subtitle">${isConfigured ? 'Using official Meta WhatsApp Business API' : 'Messages are being logged for demo'}</p>
        <div class="status-box">
            ${isConfigured ? '📱 Ready to Send Messages' : '⚡ Demo Mode - No Setup Required'}
        </div>
        <p class="info">
            ${isConfigured
            ? 'WhatsApp Cloud API is configured and ready.<br>Messages will be sent to real numbers.'
            : 'No QR code needed!<br>For production, set WHATSAPP_PHONE_NUMBER_ID and WHATSAPP_ACCESS_TOKEN.'}
        </p>
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

    const result = await sendMessage(phoneNumber, message);
    res.json(result);
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
            statusText = 'Your reservation has been cancelled.';
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

    const result = await sendMessage(phoneNumber, message);
    res.json(result);
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

    const result = await sendMessage(phoneNumber, message);
    res.json(result);
});

/**
 * POST /api/whatsapp/send-donor-welcome - Send welcome message
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

    const result = await sendMessage(phoneNumber, message);
    res.json(result);
});

/**
 * POST /api/whatsapp/send-donation-request - Send donation request
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

*${bloodBankName}* kindly requests your help with a blood donation.

━━━━━━━━━━━━━━━
📍 *Location:* ${city || 'Your area'}
📞 *Phone:* ${bankPhone || 'Contact us'}
🏥 *Address:* ${bankAddress || 'Visit us'}
━━━━━━━━━━━━━━━

Your generous donation can save up to 3 precious lives!

Thank you for considering this request. ❤️

_${bloodBankName}_`;

    const result = await sendMessage(phoneNumber, message);
    res.json(result);
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

    const result = await sendMessage(phoneNumber, message);
    res.json(result);
});

/**
 * GET /api/health - Health check
 */
app.get('/api/health', (req, res) => {
    res.json({
        status: 'ok',
        service: 'WhatsApp Cloud API Service',
        mode: isConfigured ? 'production' : 'demo',
        timestamp: new Date().toISOString()
    });
});

// Webhook for receiving messages (optional)
app.get('/webhook', (req, res) => {
    const mode = req.query['hub.mode'];
    const token = req.query['hub.verify_token'];
    const challenge = req.query['hub.challenge'];

    if (mode === 'subscribe' && token === process.env.WEBHOOK_VERIFY_TOKEN) {
        console.log('✅ Webhook verified');
        res.status(200).send(challenge);
    } else {
        res.sendStatus(403);
    }
});

app.post('/webhook', (req, res) => {
    console.log('📨 Webhook received:', JSON.stringify(req.body, null, 2));
    res.sendStatus(200);
});

// Start server
app.listen(PORT, () => {
    console.log(`
╔════════════════════════════════════════════════════╗
║     WhatsApp Cloud API Service Started             ║
╠════════════════════════════════════════════════════╣
║  🚀 Server running on port ${PORT}                      ║
║  📍 http://localhost:${PORT}                           ║
║  ${isConfigured ? '✅ Cloud API configured - Ready!' : '⚡ Demo mode - Messages will be logged'}        ║
╚════════════════════════════════════════════════════╝
    `);
});
