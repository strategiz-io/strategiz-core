const express = require('express');
const router = express.Router();
const admin = require('firebase-admin');
const kraken = require('../../kraken');
const { RateLimiterMemory } = require('rate-limiter-flexible');

// Create a more restrictive rate limiter for Kraken API calls
const krakenRateLimiter = new RateLimiterMemory({
  points: 5, // 5 requests
  duration: 60, // per 60 seconds
});

// Cache for Kraken data to reduce API calls
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
    await krakenRateLimiter.consume(req.ip);
    next();
  } catch (error) {
    console.error('Rate limit exceeded for Kraken API:', req.ip);
    return res.status(429).json({ 
      error: 'Too many requests to Kraken API. Please try again later.',
      retryAfter: Math.ceil(error.msBeforeNext / 1000) || 60
    });
  }
};

// GET /api/kraken/balance/:userId
router.get('/:userId', checkCacheAndRateLimit, async (req, res) => {
  const userId = req.params.userId;
  
  try {
    // Get user's Kraken API credentials from Firestore
    const db = admin.firestore();
    const doc = await db.collection('users').doc(userId).collection('credentials').doc('kraken').get();
    
    if (!doc.exists) {
      return res.status(404).json({ error: 'Kraken credentials not found' });
    }
    
    const credentials = doc.data();
    
    // Configure Kraken API with user's credentials
    kraken.configure({
      apiKey: credentials.apiKey,
      apiSecret: credentials.apiSecret
    });
    
    // Get account balance - this returns the completely unmodified raw data
    // This is important for the admin page to see the exact data from the API
    const balance = await kraken.getBalance();
    
    // Cache the response
    dataCache[userId] = {
      data: balance,
      timestamp: Date.now()
    };
    
    // Return the completely unmodified raw data
    return res.json(balance);
  } catch (error) {
    console.error('Error fetching Kraken balance:', error);
    
    // Handle rate limiting errors specifically
    if (error.response && error.response.status === 429) {
      return res.status(429).json({ 
        error: 'Rate limit exceeded on Kraken API',
        retryAfter: parseInt(error.response.headers['retry-after'] || '60')
      });
    }
    
    return res.status(500).json({ error: 'Failed to fetch Kraken balance' });
  }
});

module.exports = router;
