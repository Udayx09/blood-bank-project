/**
 * WhatsApp Routes
 * Provides QR code display and status endpoints
 */
const express = require('express');
const router = express.Router();
const QRCode = require('qrcode');
const { getStatus, getQRCode } = require('../services/whatsapp');

// GET /api/whatsapp/status - Get WhatsApp connection status
router.get('/status', (req, res) => {
    const status = getStatus();
    res.json({
        success: true,
        connected: status.isReady,
        hasQR: status.hasQR,
        message: status.isReady
            ? 'WhatsApp is connected and ready to send messages'
            : (status.hasQR ? 'Scan the QR code to connect' : 'Initializing...')
    });
});

// GET /api/whatsapp/qr - Get QR code as image
router.get('/qr', async (req, res) => {
    const qrData = getQRCode();

    if (!qrData) {
        const status = getStatus();
        if (status.isReady) {
            return res.send(`
                <!DOCTYPE html>
                <html>
                <head>
                    <title>WhatsApp Connected</title>
                    <style>
                        body { font-family: Arial, sans-serif; display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0; background: linear-gradient(135deg, #25D366, #128C7E); }
                        .container { text-align: center; background: white; padding: 40px; border-radius: 20px; box-shadow: 0 10px 40px rgba(0,0,0,0.2); }
                        h1 { color: #25D366; margin-bottom: 20px; }
                        p { color: #666; font-size: 18px; }
                        .checkmark { font-size: 80px; margin-bottom: 20px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="checkmark">✅</div>
                        <h1>WhatsApp Connected!</h1>
                        <p>Your WhatsApp is already connected and ready to send messages.</p>
                    </div>
                </body>
                </html>
            `);
        }
        return res.send(`
            <!DOCTYPE html>
            <html>
            <head>
                <title>Waiting for QR Code</title>
                <meta http-equiv="refresh" content="3">
                <style>
                    body { font-family: Arial, sans-serif; display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0; background: linear-gradient(135deg, #667eea, #764ba2); }
                    .container { text-align: center; background: white; padding: 40px; border-radius: 20px; box-shadow: 0 10px 40px rgba(0,0,0,0.2); }
                    h1 { color: #333; margin-bottom: 20px; }
                    p { color: #666; }
                    .loader { border: 4px solid #f3f3f3; border-top: 4px solid #25D366; border-radius: 50%; width: 50px; height: 50px; animation: spin 1s linear infinite; margin: 20px auto; }
                    @keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="loader"></div>
                    <h1>Initializing WhatsApp...</h1>
                    <p>Please wait, the QR code will appear shortly.</p>
                    <p style="font-size: 12px; color: #999;">This page will auto-refresh</p>
                </div>
            </body>
            </html>
        `);
    }

    // Generate QR code as data URL
    try {
        const qrImageUrl = await QRCode.toDataURL(qrData, {
            width: 300,
            margin: 2,
            color: {
                dark: '#000000',
                light: '#ffffff'
            }
        });

        res.send(`
            <!DOCTYPE html>
            <html>
            <head>
                <title>Scan WhatsApp QR Code</title>
                <meta http-equiv="refresh" content="30">
                <style>
                    body { font-family: Arial, sans-serif; display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0; background: linear-gradient(135deg, #25D366, #128C7E); }
                    .container { text-align: center; background: white; padding: 40px; border-radius: 20px; box-shadow: 0 10px 40px rgba(0,0,0,0.2); }
                    h1 { color: #25D366; margin-bottom: 10px; }
                    p { color: #666; margin-bottom: 20px; }
                    img { border-radius: 10px; }
                    .steps { text-align: left; background: #f5f5f5; padding: 15px 20px; border-radius: 10px; margin-top: 20px; }
                    .steps li { margin: 8px 0; color: #444; }
                    .phone-icon { font-size: 40px; margin-bottom: 10px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="phone-icon">📱</div>
                    <h1>Scan to Connect WhatsApp</h1>
                    <p>Use your phone to scan this QR code</p>
                    <img src="${qrImageUrl}" alt="WhatsApp QR Code" />
                    <div class="steps">
                        <strong>Steps:</strong>
                        <ol>
                            <li>Open WhatsApp on your phone</li>
                            <li>Go to Settings → Linked Devices</li>
                            <li>Tap "Link a Device"</li>
                            <li>Point your phone at this QR code</li>
                        </ol>
                    </div>
                </div>
            </body>
            </html>
        `);
    } catch (error) {
        res.status(500).json({ error: 'Failed to generate QR code' });
    }
});

// GET /api/whatsapp/qr-image - Get QR code as PNG image only
router.get('/qr-image', async (req, res) => {
    const qrData = getQRCode();

    if (!qrData) {
        return res.status(404).json({ error: 'No QR code available' });
    }

    try {
        res.setHeader('Content-Type', 'image/png');
        await QRCode.toFileStream(res, qrData, {
            width: 300,
            margin: 2
        });
    } catch (error) {
        res.status(500).json({ error: 'Failed to generate QR code' });
    }
});

module.exports = router;
