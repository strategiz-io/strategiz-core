const express = require('express');
const router = express.Router();
const admin = require('firebase-admin');
const binanceUS = require('../../binanceus');
const { RateLimiterMemory } = require('rate-limiter-flexible');

// Create a more restrictive rate limiter for Binance US API calls
const binanceUsRateLimiter = new RateLimiterMemory({
  points: 5, // 5 requests
  duration: 60, // per 60 seconds
});

// Cache for Binance US data to reduce API calls
const dataCache = {};
const CACHE_TTL = 60 * 1000; // 1 minute cache TTL

// Middleware to check cache and apply rate limiting
const checkCacheAndRateLimit = async (req, res, next) => {
  const userId = req.params.userId;
  
  // Check if we have fresh cached data
  if (dataCache[userId] && Date.now() - dataCache[userId].timestamp < CACHE_TTL) {
    return res.json(dataCache[userId].data);
  }
  
  // Apply rate limiting
  try {
    await binanceUsRateLimiter.consume(req.ip);
    next();
  } catch (error) {
    console.error('Rate limit exceeded for Binance US API:', req.ip);
    return res.status(429).json({ 
      error: 'Too many requests to Binance US API. Please try again later.',
      retryAfter: Math.ceil(error.msBeforeNext / 1000) || 60
    });
  }
};

// GET /api/binanceus/balance/:userId
router.get('/:userId', checkCacheAndRateLimit, async (req, res) => {
  const userId = req.params.userId;
  
  try {
    // Get user's Binance US API credentials from Firestore
    const db = admin.firestore();
    const doc = await db.collection('users').doc(userId).collection('credentials').doc('binanceus').get();
    
    if (!doc.exists) {
      return res.status(404).json({ error: 'Binance US credentials not found' });
    }
    
    const credentials = doc.data();
    
    // Configure Binance US API with user's credentials
    binanceUS.configure({
      apiKey: credentials.apiKey,
      secretKey: credentials.secretKey
    });
    
    // Get account balance
    const balance = await binanceUS.getAccountBalance();
    
    // Cache the response
    dataCache[userId] = {
      data: balance,
      timestamp: Date.now()
    };
    
    // Return the data
    return res.json(balance);
  } catch (error) {
    console.error('Error fetching Binance US balance:', error);
    
    // Handle rate limiting errors specifically
    if (error.response && error.response.status === 429) {
      return res.status(429).json({ 
        error: 'Rate limit exceeded on Binance US API',
        retryAfter: parseInt(error.response.headers['retry-after'] || '60')
      });
    }
    
    return res.status(500).json({ error: 'Failed to fetch Binance US balance' });
  }
});

module.exports = router;
