const express = require('express');
const router = express.Router();
const admin = require('firebase-admin');
const axios = require('axios');
const crypto = require('crypto');
const { RateLimiterMemory } = require('rate-limiter-flexible');

// Create a specific rate limiter for raw data admin endpoints
// More restrictive to prevent 429 errors
const rawDataRateLimiter = new RateLimiterMemory({
  points: 3, // 3 requests
  duration: 60, // per 60 seconds
});

// Cache for raw data to reduce API calls
const rawDataCache = {};
const CACHE_TTL = 30 * 1000; // 30 seconds cache TTL for admin data

// Middleware to check cache and apply rate limiting
const checkCacheAndRateLimit = async (req, res, next) => {
  const userId = req.params.userId;
  
  // Check if we have fresh cached data
  if (rawDataCache[userId] && Date.now() - rawDataCache[userId].timestamp < CACHE_TTL) {
    return res.json(rawDataCache[userId].data);
  }
  
  // Apply rate limiting
  try {
    await rawDataRateLimiter.consume(req.ip);
    next();
  } catch (error) {
    console.error('Rate limit exceeded for Coinbase raw data API:', req.ip);
    return res.status(429).json({ 
      error: 'Too many requests to Coinbase raw data API. Please try again later.',
      retryAfter: Math.ceil(error.msBeforeNext / 1000) || 60
    });
  }
};

// GET /api/coinbase/raw-data/:userId
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
    
    // Make direct request to Coinbase API to get completely unmodified raw data
    const timestamp = Math.floor(Date.now() / 1000).toString();
    const method = 'GET';
    const requestPath = '/v2/accounts';
    
    // Create signature
    const message = timestamp + method + requestPath;
    const hmac = crypto.createHmac('sha256', credentials.apiSecret);
    const signature = hmac.update(message).digest('base64');
    
    // Make the API request
    const response = await axios({
      method,
      url: `https://api.coinbase.com${requestPath}`,
      headers: {
        'CB-ACCESS-KEY': credentials.apiKey,
        'CB-ACCESS-SIGN': signature,
        'CB-ACCESS-TIMESTAMP': timestamp,
        'CB-VERSION': '2021-04-29',
        'Content-Type': 'application/json'
      }
    });
    
    // Cache the completely unmodified raw data
    rawDataCache[userId] = {
      data: response.data,
      timestamp: Date.now()
    };
    
    // Return the completely unmodified raw data
    return res.json(response.data);
  } catch (error) {
    console.error('Error fetching Coinbase raw data:', error);
    
    // Handle rate limiting errors specifically
    if (error.response && error.response.status === 429) {
      return res.status(429).json({ 
        error: 'Rate limit exceeded on Coinbase API',
        retryAfter: parseInt(error.response.headers['retry-after'] || '60')
      });
    }
    
    return res.status(500).json({ error: 'Failed to fetch Coinbase raw data' });
  }
});

module.exports = router;
