require('dotenv').config();
const express = require('express');
const cors = require('cors');

// Import routes
const bloodBankRoutes = require('./routes/bloodBanks');
const reservationRoutes = require('./routes/reservations');
const whatsappRoutes = require('./routes/whatsapp');
const adminRoutes = require('./routes/admin');
const authRoutes = require('./routes/auth');
const bankPortalRoutes = require('./routes/bankPortal');

// Import WhatsApp service
const { initWhatsApp } = require('./services/whatsapp');

const app = express();
const PORT = process.env.PORT || 3000;

// Initialize WhatsApp client
initWhatsApp();

// Middleware
app.use(cors({
    origin: process.env.FRONTEND_URL || 'http://localhost:4200',
    methods: ['GET', 'POST', 'PUT', 'DELETE'],
    allowedHeaders: ['Content-Type', 'Authorization']
}));
app.use(express.json());

// Request logging (development)
app.use((req, res, next) => {
    console.log(`${new Date().toISOString()} - ${req.method} ${req.path}`);
    next();
});

// Routes
app.use('/api/blood-banks', bloodBankRoutes);
app.use('/api/reservations', reservationRoutes);
app.use('/api/whatsapp', whatsappRoutes);
app.use('/api/admin', adminRoutes);
app.use('/api/auth', authRoutes);
app.use('/api/bank', bankPortalRoutes);

// Health check endpoint
app.get('/api/health', (req, res) => {
    res.json({
        status: 'ok',
        message: 'Blood Bank API is running',
        timestamp: new Date().toISOString()
    });
});

// Root endpoint
app.get('/', (req, res) => {
    res.json({
        name: 'Blood Bank API',
        version: '1.0.0',
        endpoints: {
            health: '/api/health',
            bloodBanks: '/api/blood-banks',
            search: '/api/blood-banks/search?bloodType=A+',
            reservations: '/api/reservations'
        }
    });
});

// 404 handler
app.use((req, res) => {
    res.status(404).json({ error: 'Endpoint not found' });
});

// Error handler
app.use((err, req, res, next) => {
    console.error('Error:', err.message);
    res.status(500).json({ error: 'Internal server error' });
});

// Start server
app.listen(PORT, () => {
    console.log(`
╔════════════════════════════════════════╗
║     Blood Bank API Server Started      ║
╠════════════════════════════════════════╣
║  🚀 Server running on port ${PORT}         ║
║  📍 http://localhost:${PORT}              ║
║  🏥 API: http://localhost:${PORT}/api     ║
╚════════════════════════════════════════╝
    `);
});
