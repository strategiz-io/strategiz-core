import express from 'express';
import axios from 'axios';
import { RateLimiterMemory } from 'rate-limiter-flexible';

const router = express.Router();

// Create a more restrictive rate limiter for Binance US API calls
const binanceUsRateLimiter = new RateLimiterMemory({
  points: 5, // 5 requests
  duration: 60, // per 60 seconds
});

// Cache for Binance US data to reduce API calls
const dataCache: { [userId: string]: { data: any; timestamp: number } } = {};
const CACHE_TTL = 60 * 1000; // 1 minute cache TTL

// Middleware to check cache and apply rate limiting
const checkCacheAndRateLimit = async (req: express.Request, res: express.Response, next: express.NextFunction) => {
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
    // Get user's Binance US API credentials from database
    // This is a placeholder - implement your actual authentication logic
    const apiKey = process.env.BINANCE_US_API_KEY || 'your-api-key';
    const apiSecret = process.env.BINANCE_US_API_SECRET || 'your-api-secret';
    
    // Make request to Binance US API
    const response = await axios.get('https://api.binance.us/api/v3/account', {
      headers: {
        'X-MBX-APIKEY': apiKey
      },
      // Add necessary authentication parameters
      params: {
        timestamp: Date.now(),
        // Add signature here based on apiSecret
      }
    });
    
    // Cache the response
    dataCache[userId] = {
      data: response.data,
      timestamp: Date.now()
    };
    
    // Return the data
    return res.json(response.data);
  } catch (error) {
    console.error('Error fetching Binance US balance:', error.message);
    
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

export default router;
