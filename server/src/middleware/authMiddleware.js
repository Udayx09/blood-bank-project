const jwt = require('jsonwebtoken');

const JWT_SECRET = process.env.JWT_SECRET || 'blood-bank-secret-key-2024';

// Middleware to verify JWT token for blood bank routes
const authenticateBank = (req, res, next) => {
    const authHeader = req.headers.authorization;

    if (!authHeader || !authHeader.startsWith('Bearer ')) {
        return res.status(401).json({
            success: false,
            error: 'Access denied. No token provided.'
        });
    }

    const token = authHeader.split(' ')[1];

    try {
        const decoded = jwt.verify(token, JWT_SECRET);
        req.bank = decoded; // Contains { id, name, email }
        next();
    } catch (err) {
        return res.status(401).json({
            success: false,
            error: 'Invalid or expired token'
        });
    }
};

// Generate JWT token for blood bank
const generateToken = (bank) => {
    return jwt.sign(
        { id: bank.id, name: bank.name, email: bank.email },
        JWT_SECRET,
        { expiresIn: '7d' }
    );
};

module.exports = { authenticateBank, generateToken, JWT_SECRET };
