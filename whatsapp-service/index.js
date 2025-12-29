/**
 * WhatsApp Microservice - DEMO MODE
 * Simulates WhatsApp notifications without requiring Puppeteer/Chrome
 * Perfect for demos, presentations, and development
 */

require('dotenv').config();
const express = require('express');
const cors = require('cors');

const app = express();
const PORT = process.env.PORT || 3001;

// Demo mode is always ready
const isReady = true;
const DEMO_MODE = true;

// Middleware
app.use(cors());
app.use(express.json());

// Request logging
app.use((req, res, next) => {
    console.log(`${new Date().toISOString()} - ${req.method} ${req.path}`);
    next();
});

/**
 * Log a simulated WhatsApp message
 */
const logMessage = (phoneNumber, messageType, details) => {
    console.log('\n╔════════════════════════════════════════════════════════════╗');
    console.log('║  📱 DEMO MODE - WhatsApp Message Simulated                 ║');
    console.log('╠════════════════════════════════════════════════════════════╣');
    console.log(`║  📞 To: ${phoneNumber.padEnd(49)}║`);
    console.log(`║  📝 Type: ${messageType.padEnd(47)}║`);
    console.log('╠════════════════════════════════════════════════════════════╣');
    Object.entries(details).forEach(([key, value]) => {
        const line = `${key}: ${value}`.substring(0, 56);
        console.log(`║  ${line.padEnd(58)}║`);
    });
    console.log('╚════════════════════════════════════════════════════════════╝\n');
};

// ================== API Endpoints ==================

/**
 * GET /api/whatsapp/status - Get connection status
 */
app.get('/api/whatsapp/status', (req, res) => {
    res.json({
        success: true,
        isReady: true,
        hasQR: false,
        demoMode: true,
        message: 'WhatsApp Demo Mode - All messages will be simulated'
    });
});

/**
 * GET /api/whatsapp/qr - QR code page (Demo Mode)
 */
app.get('/api/whatsapp/qr', (req, res) => {
    const html = `
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>WhatsApp Demo Mode - Blood Bank</title>
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
        .status-demo {
            background: linear-gradient(135deg, #667eea, #764ba2);
            padding: 20px 40px;
            border-radius: 12px;
            font-size: 20px;
            font-weight: bold;
        }
        .features {
            text-align: left;
            margin-top: 30px;
            padding: 20px;
            background: rgba(255,255,255,0.05);
            border-radius: 12px;
        }
        .feature { padding: 8px 0; border-bottom: 1px solid rgba(255,255,255,0.1); }
        .feature:last-child { border-bottom: none; }
        .check { color: #00b894; margin-right: 10px; }
    </style>
</head>
<body>
    <div class="container">
        <div class="status-icon">🎭</div>
        <h1 class="title">Demo Mode Active</h1>
        <p class="subtitle">No QR code needed - Messages are simulated</p>
        <div class="status-demo">
            ✨ Ready for Demonstration
        </div>
        <div class="features">
            <div class="feature"><span class="check">✓</span> Reservation confirmations logged</div>
            <div class="feature"><span class="check">✓</span> Status updates logged</div>
            <div class="feature"><span class="check">✓</span> OTP messages logged</div>
            <div class="feature"><span class="check">✓</span> All API endpoints working</div>
            <div class="feature"><span class="check">✓</span> No WhatsApp account required</div>
        </div>
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

    logMessage(phoneNumber, 'Reservation Confirmation', {
        'Patient': patientName,
        'Blood Type': bloodType,
        'Units': unitsNeeded,
        'Blood Bank': bloodBankName,
        'Reservation ID': reservationId
    });

    res.json({
        success: true,
        message: 'Confirmation sent (Demo Mode)',
        demoMode: true
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

    logMessage(phoneNumber, 'Status Update', {
        'Patient': patientName,
        'New Status': status,
        'Blood Bank': bloodBankName
    });

    res.json({
        success: true,
        message: 'Status update sent (Demo Mode)',
        demoMode: true
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

    logMessage(phoneNumber, 'OTP Verification', {
        'OTP Code': otp,
        'Valid For': '10 minutes'
    });

    res.json({
        success: true,
        message: 'OTP sent (Demo Mode)',
        demoMode: true
    });
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

    logMessage(phoneNumber, 'Welcome Message', {
        'Donor Name': donorName,
        'Message': 'Welcome to Blood Bank!'
    });

    res.json({
        success: true,
        message: 'Welcome message sent (Demo Mode)',
        demoMode: true
    });
});

/**
 * POST /api/whatsapp/send-eligibility-reminder
 */
app.post('/api/whatsapp/send-eligibility-reminder', async (req, res) => {
    const { phoneNumber, donorName } = req.body;

    logMessage(phoneNumber || 'unknown', 'Eligibility Reminder', {
        'Donor': donorName || 'unknown',
        'Message': 'You are now eligible to donate!'
    });

    res.json({ success: true, message: 'Reminder sent (Demo Mode)', demoMode: true });
});

/**
 * POST /api/whatsapp/send-blood-shortage-alert
 */
app.post('/api/whatsapp/send-blood-shortage-alert', async (req, res) => {
    const { phoneNumber, donorName, bloodType, city, bloodBankName } = req.body;

    logMessage(phoneNumber || 'unknown', 'Blood Shortage Alert', {
        'Blood Type': bloodType,
        'City': city,
        'Blood Bank': bloodBankName
    });

    res.json({ success: true, message: 'Alert sent (Demo Mode)', demoMode: true });
});

/**
 * POST /api/whatsapp/send-donation-request
 */
app.post('/api/whatsapp/send-donation-request', async (req, res) => {
    const { phoneNumber, donorName, bloodBankName, city } = req.body;

    logMessage(phoneNumber || 'unknown', 'Donation Request', {
        'Donor': donorName,
        'From Bank': bloodBankName,
        'City': city
    });

    res.json({ success: true, message: 'Request sent (Demo Mode)', demoMode: true });
});

/**
 * POST /api/whatsapp/send - Generic message
 */
app.post('/api/whatsapp/send', async (req, res) => {
    const { phoneNumber, message } = req.body;

    if (!phoneNumber || !message) {
        return res.status(400).json({
            success: false,
            error: 'phoneNumber and message are required'
        });
    }

    logMessage(phoneNumber, 'Custom Message', {
        'Message': message.substring(0, 50) + (message.length > 50 ? '...' : '')
    });

    res.json({
        success: true,
        message: 'Message sent (Demo Mode)',
        demoMode: true
    });
});

// Health check
app.get('/api/health', (req, res) => {
    res.json({
        status: 'ok',
        service: 'WhatsApp Microservice (Demo Mode)',
        demoMode: true,
        timestamp: new Date().toISOString()
    });
});

// Start server
app.listen(PORT, () => {
    console.log(`
╔════════════════════════════════════════════════════════════╗
║     🎭 WhatsApp Microservice - DEMO MODE                   ║
╠════════════════════════════════════════════════════════════╣
║  🚀 Server running on port ${String(PORT).padEnd(29)}║
║  📍 http://localhost:${String(PORT).padEnd(36)}║
║  ✨ All messages will be logged (not actually sent)       ║
║  📱 No WhatsApp connection required                        ║
╚════════════════════════════════════════════════════════════╝
    `);
});
