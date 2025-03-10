/**
 * Coinbase API Integration for Strategiz
 * 
 * This module provides a unified interface for interacting with the Coinbase API.
 * It supports both Coinbase Standard API and Coinbase Cloud API authentication methods.
 */

const axios = require('axios');
const crypto = require('crypto');
const jwt = require('jsonwebtoken');
const fs = require('fs');
const path = require('path');
require('dotenv').config();

// API URLs
const COINBASE_API_URL = 'https://api.coinbase.com';
const COINBASE_CLOUD_API_URL = 'https://api.coinbase.com/api/v3';

// Default credentials from environment variables
let credentials = {
  standard: {
    apiKey: process.env.COINBASE_API_KEY || '',
    apiSecret: process.env.COINBASE_API_SECRET || ''
  },
  cloud: {
    apiKey: process.env.COINBASE_CLOUD_API_KEY || '',
    organizationId: process.env.COINBASE_CLOUD_ORGANIZATION_ID || '',
    privateKey: ''
  }
};

// Try to load private key from file if it exists
const privateKeyPath = path.join(__dirname, 'private-key.pem');
if (fs.existsSync(privateKeyPath)) {
  try {
    credentials.cloud.privateKey = fs.readFileSync(privateKeyPath, 'utf8');
  } catch (error) {
    console.error('Error loading private key:', error);
  }
}

/**
 * Configure the Coinbase API credentials
 * @param {Object} config - The configuration object
 */
function configure(config) {
  if (config.standard) {
    credentials.standard = { ...credentials.standard, ...config.standard };
  }
  
  if (config.cloud) {
    credentials.cloud = { ...credentials.cloud, ...config.cloud };
  }
}

/**
 * Creates a JWT token for Coinbase Cloud API authentication
 * @returns {Promise<string>} - The JWT token
 */
async function createJwtToken() {
  try {
    if (!credentials.cloud.apiKey || !credentials.cloud.organizationId || !credentials.cloud.privateKey) {
      throw new Error('Missing Coinbase Cloud API credentials');
    }
    
    // Create the JWT payload
    const now = Math.floor(Date.now() / 1000);
    const payload = {
      sub: credentials.cloud.apiKey,
      iss: credentials.cloud.organizationId,
      iat: now,
      exp: now + 300, // 5 minutes expiration
      aud: ['coinbase-cloud']
    };
    
    // Sign the JWT with the private key
    return new Promise((resolve, reject) => {
      jwt.sign(payload, credentials.cloud.privateKey, { algorithm: 'ES256' }, (err, token) => {
        if (err) {
          reject(err);
        } else {
          resolve(token);
        }
      });
    });
  } catch (error) {
    console.error('Error creating JWT token:', error);
    throw error;
  }
}

/**
 * Creates a signature for standard Coinbase API authentication
 * @param {string} timestamp - The timestamp
 * @param {string} method - The HTTP method
 * @param {string} requestPath - The request path
 * @param {string} body - The request body
 * @returns {string} - The signature
 */
function createSignature(timestamp, method, requestPath, body) {
  if (!credentials.standard.apiSecret) {
    throw new Error('Missing Coinbase API Secret');
  }
  
  const message = timestamp + method + requestPath + (body || '');
  const hmac = crypto.createHmac('sha256', credentials.standard.apiSecret);
  return hmac.update(message).digest('base64');
}

/**
 * Makes a request to the Coinbase Cloud API
 * @param {string} method - The HTTP method
 * @param {string} endpoint - The API endpoint
 * @param {Object} data - The request data
 * @returns {Promise<Object>} - The API response
 */
async function requestCloudApi(method, endpoint, data = null) {
  try {
    // Create JWT token
    const token = await createJwtToken();
    
    // Make the API request
    const response = await axios({
      method,
      url: `${COINBASE_CLOUD_API_URL}${endpoint}`,
      data: data,
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    });
    
    return response.data;
  } catch (error) {
    console.error('Error making Coinbase Cloud API request:', error);
    throw error;
  }
}

/**
 * Makes a request to the Coinbase Standard API
 * @param {string} method - The HTTP method
 * @param {string} endpoint - The API endpoint
 * @param {Object} data - The request data
 * @returns {Promise<Object>} - The API response
 */
async function requestStandardApi(method, endpoint, data = null) {
  try {
    if (!credentials.standard.apiKey) {
      throw new Error('Missing Coinbase API Key');
    }
    
    // Prepare request details
    const timestamp = Math.floor(Date.now() / 1000).toString();
    const requestPath = endpoint;
    const body = data ? JSON.stringify(data) : '';
    
    // Create signature
    const signature = createSignature(timestamp, method, requestPath, body);
    
    // Make the API request
    const response = await axios({
      method,
      url: `${COINBASE_API_URL}${endpoint}`,
      data: data,
      headers: {
        'CB-ACCESS-KEY': credentials.standard.apiKey,
        'CB-ACCESS-SIGN': signature,
        'CB-ACCESS-TIMESTAMP': timestamp,
        'CB-VERSION': '2021-04-29',
        'Content-Type': 'application/json'
      }
    });
    
    return response.data;
  } catch (error) {
    console.error('Error making Coinbase Standard API request:', error);
    throw error;
  }
}

/**
 * Gets the user's Coinbase accounts
 * @returns {Promise<Object>} - The accounts data
 */
async function getAccounts() {
  try {
    // Try Cloud API first
    if (credentials.cloud.apiKey && credentials.cloud.organizationId && credentials.cloud.privateKey) {
      try {
        return await requestCloudApi('GET', '/brokerage/accounts');
      } catch (error) {
        console.warn('Cloud API request failed, falling back to Standard API:', error.message);
      }
    }
    
    // Fall back to Standard API
    if (credentials.standard.apiKey && credentials.standard.apiSecret) {
      return await requestStandardApi('GET', '/v2/accounts');
    }
    
    throw new Error('No valid Coinbase credentials configured');
  } catch (error) {
    console.error('Error getting Coinbase accounts:', error);
    throw error;
  }
}

/**
 * Gets the current price of a cryptocurrency
 * @param {string} currencyPair - The currency pair (e.g., 'BTC-USD')
 * @returns {Promise<Object>} - The price data
 */
async function getPrice(currencyPair) {
  try {
    // Standard API is better for price data
    return await requestStandardApi('GET', `/v2/prices/${currencyPair}/spot`);
  } catch (error) {
    console.error(`Error getting price for ${currencyPair}:`, error);
    throw error;
  }
}

/**
 * Tests the Coinbase API connection
 * @returns {Promise<Object>} - The test results
 */
async function testConnection() {
  const results = {
    standard: { success: false, error: null, data: null },
    cloud: { success: false, error: null, data: null }
  };
  
  // Test Standard API
  if (credentials.standard.apiKey && credentials.standard.apiSecret) {
    try {
      const data = await requestStandardApi('GET', '/v2/user');
      results.standard = {
        success: true,
        error: null,
        data: data
      };
    } catch (error) {
      results.standard = {
        success: false,
        error: error.message,
        data: null
      };
    }
  } else {
    results.standard = {
      success: false,
      error: 'No Standard API credentials configured',
      data: null
    };
  }
  
  // Test Cloud API
  if (credentials.cloud.apiKey && credentials.cloud.organizationId && credentials.cloud.privateKey) {
    try {
      const data = await requestCloudApi('GET', '/brokerage/products');
      results.cloud = {
        success: true,
        error: null,
        data: data
      };
    } catch (error) {
      results.cloud = {
        success: false,
        error: error.message,
        data: null
      };
    }
  } else {
    results.cloud = {
      success: false,
      error: 'No Cloud API credentials configured',
      data: null
    };
  }
  
  return results;
}

module.exports = {
  configure,
  getAccounts,
  getPrice,
  testConnection,
  requestStandardApi,
  requestCloudApi
};
