const express = require('express');
const router = express.Router();
const crypto = require('crypto');
const axios = require('axios');

/**
 * POST /api/binanceus/admin/raw-data
 * Get completely unmodified raw data from Binance US API
 * This endpoint accepts API keys directly in the request body
 */
router.post('/raw-data', async (req, res) => {
  try {
    const { apiKey, secretKey } = req.body;
    
    if (!apiKey || !secretKey) {
      return res.status(400).json({ 
        status: 'error', 
        message: 'API Key and Secret Key are required' 
      });
    }
    
    // Make a direct request to Binance US API to get completely unmodified raw data
    const timestamp = Date.now();
    const queryString = `timestamp=${timestamp}`;
    const signature = crypto
      .createHmac('sha256', secretKey)
      .update(queryString)
      .digest('hex');
    
    const url = `https://api.binance.us/api/v3/account?${queryString}&signature=${signature}`;
    
    const response = await axios.get(url, {
      headers: {
        'X-MBX-APIKEY': apiKey
      }
    });
    
    // Return the completely unmodified raw data directly
    // This ensures the frontend gets exactly what comes from the Binance US API
    return res.json(response.data);
  } catch (error) {
    console.error('Error fetching raw Binance US data:', error);
    
    // Handle rate limiting errors specifically
    if (error.response && error.response.status === 429) {
      return res.status(429).json({ 
        status: 'error',
        message: 'Rate limit exceeded on Binance US API',
        retryAfter: parseInt(error.response.headers['retry-after'] || '60')
      });
    }
    
    // Handle other API errors
    if (error.response && error.response.data) {
      return res.status(error.response.status || 500).json({
        status: 'error',
        message: error.response.data.msg || 'Failed to fetch Binance US raw data',
        code: error.response.data.code
      });
    }
    
    return res.status(500).json({ 
      status: 'error',
      message: 'Failed to fetch Binance US raw data' 
    });
  }
});

module.exports = router;
