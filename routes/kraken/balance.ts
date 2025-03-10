import express from 'express';
import axios from 'axios';
import { RateLimiterMemory } from 'rate-limiter-flexible';
import crypto from 'crypto';
import querystring from 'querystring';

const router = express.Router();

// Create a more restrictive rate limiter for Kraken API calls
const krakenRateLimiter = new RateLimiterMemory({
  points: 5, // 5 requests
  duration: 60, // per 60 seconds
});

// Cache for Kraken data to reduce API calls
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
    // Get user's Kraken API credentials from database
    // This is a placeholder - implement your actual authentication logic
    const apiKey = process.env.KRAKEN_API_KEY || 'your-api-key';
    const apiSecret = process.env.KRAKEN_API_SECRET || 'your-api-secret';
    
    // Kraken API endpoint
    const endpoint = '/0/private/Balance';
    const url = 'https://api.kraken.com' + endpoint;
    
    // Prepare request data
    const nonce = Date.now().toString();
    const postData = {
      nonce: nonce
    };
    
    // Create signature
    const signature = createKrakenSignature(endpoint, postData, apiSecret);
    
    // Make request to Kraken API
    const response = await axios.post(url, querystring.stringify(postData), {
      headers: {
        'API-Key': apiKey,
        'API-Sign': signature,
        'Content-Type': 'application/x-www-form-urlencoded'
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
    console.error('Error fetching Kraken balance:', error.message);
    
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

// Helper function to create Kraken API signature
function createKrakenSignature(endpoint: string, postData: any, secret: string): string {
  const message = querystring.stringify(postData);
  const secretBuffer = Buffer.from(secret, 'base64');
  const hash = crypto.createHash('sha256');
  const hmac = crypto.createHmac('sha512', secretBuffer);
  const hashDigest = hash.update(postData.nonce + message).digest('binary');
  const hmacDigest = hmac.update(endpoint + hashDigest, 'binary').digest('base64');
  
  return hmacDigest;
}

export default router;
