const express = require('express');
const router = express.Router();
const admin = require('firebase-admin');
const coinbase = require('../../coinbase');
const { RateLimiterMemory } = require('rate-limiter-flexible');

// Create a more restrictive rate limiter for Coinbase API calls
const coinbaseRateLimiter = new RateLimiterMemory({
  points: 5, // 5 requests
  duration: 60, // per 60 seconds
});

// Cache for Coinbase data to reduce API calls
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
    await coinbaseRateLimiter.consume(req.ip);
    next();
  } catch (error) {
    console.error('Rate limit exceeded for Coinbase API:', req.ip);
    return res.status(429).json({ 
      error: 'Too many requests to Coinbase API. Please try again later.',
      retryAfter: Math.ceil(error.msBeforeNext / 1000) || 60
    });
  }
};

// GET /api/coinbase/balance/:userId
router.get('/:userId', checkCacheAndRateLimit, async (req, res) => {
  const userId = req.params.userId;
  
  try {
    // Get user's Coinbase API credentials from Firestore
    const db = admin.firestore();
    const doc = await db.collection('users').doc(userId).collection('credentials').doc('coinbase').get();
    
    if (!doc.exists) {
      return res.status(404).json({ error: 'Coinbase credentials not found' });
    }
    
    const credentials = doc.data();
    
    // Configure Coinbase API with user's credentials
    coinbase.configure({
      standard: {
        apiKey: credentials.apiKey,
        apiSecret: credentials.apiSecret
      },
      cloud: {
        apiKey: credentials.cloudApiKey,
        organizationId: credentials.organizationId,
        privateKey: credentials.privateKey
      }
    });
    
    // Get account balance
    const accounts = await coinbase.getAccounts();
    
    // Cache the response
    dataCache[userId] = {
      data: accounts,
      timestamp: Date.now()
    };
    
    // Return the data
    return res.json(accounts);
  } catch (error) {
    console.error('Error fetching Coinbase balance:', error);
    
    // Handle rate limiting errors specifically
    if (error.response && error.response.status === 429) {
      return res.status(429).json({ 
        error: 'Rate limit exceeded on Coinbase API',
        retryAfter: parseInt(error.response.headers['retry-after'] || '60')
      });
    }
    
    return res.status(500).json({ error: 'Failed to fetch Coinbase balance' });
  }
});

module.exports = router;
