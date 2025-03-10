const express = require('express');
const router = express.Router();
const admin = require('firebase-admin');
const uniswap = require('../../uniswap');
const { RateLimiterMemory } = require('rate-limiter-flexible');

// Create a more restrictive rate limiter for Uniswap API calls
const uniswapRateLimiter = new RateLimiterMemory({
  points: 5, // 5 requests
  duration: 60, // per 60 seconds
});

// Cache for Uniswap data to reduce API calls
const dataCache = {};
const CACHE_TTL = 60 * 1000; // 1 minute cache TTL

// Middleware to check cache and apply rate limiting
const checkCacheAndRateLimit = async (req, res, next) => {
  const userId = req.params.userId;
  const walletAddress = req.params.walletAddress;
  const cacheKey = `${userId}-${walletAddress}`;
  
  // Check if we have fresh cached data
  if (dataCache[cacheKey] && Date.now() - dataCache[cacheKey].timestamp < CACHE_TTL) {
    return res.json(dataCache[cacheKey].data);
  }
  
  // Apply rate limiting
  try {
    await uniswapRateLimiter.consume(req.ip);
    next();
  } catch (error) {
    console.error('Rate limit exceeded for Uniswap API:', req.ip);
    return res.status(429).json({ 
      error: 'Too many requests to Uniswap API. Please try again later.',
      retryAfter: Math.ceil(error.msBeforeNext / 1000) || 60
    });
  }
};

// GET /api/uniswap/portfolio/:userId/:walletAddress
router.get('/:userId/:walletAddress', checkCacheAndRateLimit, async (req, res) => {
  const userId = req.params.userId;
  const walletAddress = req.params.walletAddress;
  const cacheKey = `${userId}-${walletAddress}`;
  
  try {
    // Validate Ethereum address format
    if (!/^0x[a-fA-F0-9]{40}$/.test(walletAddress)) {
      return res.status(400).json({ error: 'Invalid Ethereum wallet address' });
    }
    
    // Get user's Ethereum provider settings from Firestore (optional)
    const db = admin.firestore();
    const doc = await db.collection('users').doc(userId).collection('settings').doc('ethereum').get();
    
    if (doc.exists) {
      const settings = doc.data();
      // Configure Uniswap with custom provider if available
      if (settings.providerUrl) {
        uniswap.configure({
          providerUrl: settings.providerUrl
        });
      }
    }
    
    // Get portfolio data
    const portfolio = await uniswap.getPortfolio(walletAddress);
    
    // Cache the response
    dataCache[cacheKey] = {
      data: portfolio,
      timestamp: Date.now()
    };
    
    // Return the data
    return res.json(portfolio);
  } catch (error) {
    console.error('Error fetching Uniswap portfolio:', error);
    return res.status(500).json({ error: 'Failed to fetch Uniswap portfolio' });
  }
});

module.exports = router;
