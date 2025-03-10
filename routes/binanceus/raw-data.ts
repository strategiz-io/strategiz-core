import express from 'express';
import axios from 'axios';
import { RateLimiterMemory } from 'rate-limiter-flexible';

const router = express.Router();

// Create a specific rate limiter for raw data admin endpoints
// More restrictive to prevent 429 errors
const rawDataRateLimiter = new RateLimiterMemory({
  points: 3, // 3 requests
  duration: 60, // per 60 seconds
});

// Cache for raw data to reduce API calls
const rawDataCache: { [userId: string]: { data: any; timestamp: number } } = {};
const CACHE_TTL = 30 * 1000; // 30 seconds cache TTL for admin data

// Middleware to check cache and apply rate limiting
const checkCacheAndRateLimit = async (req: express.Request, res: express.Response, next: express.NextFunction) => {
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
    console.error('Rate limit exceeded for Binance US raw data API:', req.ip);
    return res.status(429).json({ 
      error: 'Too many requests to Binance US raw data API. Please try again later.',
      retryAfter: Math.ceil(error.msBeforeNext / 1000) || 60
    });
  }
};

// GET /api/binanceus/raw-data/:userId
router.get('/:userId', checkCacheAndRateLimit, async (req, res) => {
  const userId = req.params.userId;
  
  try {
    // Get user's Binance US API credentials from database
    // This is a placeholder - implement your actual authentication logic
    const apiKey = process.env.BINANCE_US_API_KEY || 'your-api-key';
    const apiSecret = process.env.BINANCE_US_API_SECRET || 'your-api-secret';
    
    // Make request to Binance US API to get completely unmodified raw data
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
    
    // Cache the raw response
    rawDataCache[userId] = {
      data: response.data,
      timestamp: Date.now()
    };
    
    // Return the completely unmodified raw data
    return res.json(response.data);
  } catch (error) {
    console.error('Error fetching Binance US raw data:', error.message);
    
    // Handle rate limiting errors specifically
    if (error.response && error.response.status === 429) {
      return res.status(429).json({ 
        error: 'Rate limit exceeded on Binance US API',
        retryAfter: parseInt(error.response.headers['retry-after'] || '60')
      });
    }
    
    return res.status(500).json({ error: 'Failed to fetch Binance US raw data' });
  }
});

export default router;
