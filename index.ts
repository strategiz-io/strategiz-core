const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const morgan = require('morgan');
const dotenv = require('dotenv');
const rateLimit = require('express-rate-limit');
const admin = require('firebase-admin');
const path = require('path');

// Load environment variables
dotenv.config();

// Initialize Firebase Admin
try {
  const serviceAccount = require('./serviceAccountKey.json');
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
  });
  console.log('Firebase Admin initialized successfully');
} catch (error) {
  console.error('Error initializing Firebase Admin:', error);
}

// Create Express server
const app = express();
const PORT = process.env.PORT || 3001;

// Middleware
app.use(cors());
app.use(helmet());
app.use(morgan('dev'));
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Global rate limiter
const globalLimiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 100, // limit each IP to 100 requests per windowMs
  standardHeaders: true,
  legacyHeaders: false,
  message: 'Too many requests from this IP, please try again after 15 minutes'
});

// Apply global rate limiter to all requests
app.use(globalLimiter);

// Exchange API specific rate limiters
const exchangeApiLimiter = rateLimit({
  windowMs: 60 * 1000, // 1 minute
  max: 10, // limit each IP to 10 requests per minute
  standardHeaders: true,
  legacyHeaders: false,
  message: 'Too many exchange API requests, please try again after a minute'
});

// Admin API specific rate limiters - more lenient for raw data endpoints
const adminApiLimiter = rateLimit({
  windowMs: 60 * 1000, // 1 minute
  max: 5, // limit each IP to 5 requests per minute for admin endpoints
  standardHeaders: true,
  legacyHeaders: false,
  message: 'Too many admin API requests, please try again after a minute'
});

// Routes
// User routes
app.use('/api/user', require('./routes/user'));

// Kraken routes with rate limiting
app.use('/api/kraken/balance', exchangeApiLimiter, require('./routes/kraken/balance'));
app.use('/api/kraken/raw-data', adminApiLimiter, require('./routes/kraken/raw-data'));

// Binance US routes with rate limiting
app.use('/api/binanceus/balance', exchangeApiLimiter, require('./routes/binanceus/balance'));
app.use('/api/binanceus/raw-data', adminApiLimiter, require('./routes/binanceus/raw-data'));

// Coinbase routes with rate limiting
app.use('/api/coinbase/balance', exchangeApiLimiter, require('./routes/coinbase/balance'));
app.use('/api/coinbase/raw-data', adminApiLimiter, require('./routes/coinbase/raw-data'));

// Alpaca routes
app.use('/api/alpaca', exchangeApiLimiter, require('./routes/alpaca'));

// TD Ameritrade routes
app.use('/api/tdameritrade', exchangeApiLimiter, require('./routes/tdameritrade'));

// Uniswap routes with rate limiting
app.use('/api/uniswap/portfolio', exchangeApiLimiter, require('./routes/uniswap/portfolio'));
app.use('/api/uniswap/raw-data', adminApiLimiter, require('./routes/uniswap/raw-data'));

// Health check endpoint
app.get('/health', (req, res) => {
  res.status(200).json({ status: 'ok', timestamp: new Date().toISOString() });
});

// Start server
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});

module.exports = app;
