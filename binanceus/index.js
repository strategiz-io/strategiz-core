/**
 * BinanceUS API Integration for Strategiz
 * 
 * This module provides a unified interface for interacting with the BinanceUS API.
 */

const axios = require('axios');
const crypto = require('crypto');
const qs = require('querystring');
require('dotenv').config();

// API URL
const BINANCEUS_API_URL = 'https://api.binance.us';

// Default credentials from environment variables
let credentials = {
  apiKey: process.env.BINANCEUS_API_KEY || '',
  secretKey: process.env.BINANCEUS_SECRET_KEY || ''
};

/**
 * Configure the BinanceUS API credentials
 * @param {Object} config - The configuration object
 */
function configure(config) {
  if (config.apiKey) credentials.apiKey = config.apiKey;
  if (config.secretKey) credentials.secretKey = config.secretKey;
}

/**
 * Make a public request to BinanceUS API
 * @param {string} method - HTTP method
 * @param {string} endpoint - API endpoint
 * @param {Object} params - Request parameters
 * @returns {Promise<Object>} - API response
 */
async function publicRequest(method, endpoint, params = {}) {
  try {
    const url = `${BINANCEUS_API_URL}${endpoint}`;
    const response = await axios({
      method,
      url,
      params
    });
    return response.data;
  } catch (error) {
    console.error(`Error making public request to ${endpoint}:`, error);
    throw error;
  }
}

/**
 * Make a signed request to BinanceUS API
 * @param {string} method - HTTP method
 * @param {string} endpoint - API endpoint
 * @param {Object} params - Request parameters
 * @returns {Promise<Object>} - API response
 */
async function signedRequest(method, endpoint, params = {}) {
  try {
    // Add timestamp
    const timestamp = Date.now();
    params = {
      ...params,
      timestamp
    };

    // Create signature
    const queryString = qs.stringify(params);
    const signature = crypto
      .createHmac('sha256', credentials.secretKey)
      .update(queryString)
      .digest('hex');

    // Add signature to params
    params.signature = signature;

    // Make request
    const url = `${BINANCEUS_API_URL}${endpoint}`;
    const response = await axios({
      method,
      url,
      params,
      headers: {
        'X-MBX-APIKEY': credentials.apiKey
      }
    });

    return response.data;
  } catch (error) {
    console.error(`Error making signed request to ${endpoint}:`, error);
    throw error;
  }
}

/**
 * Get exchange information
 * @returns {Promise<Object>} - Exchange information
 */
async function getExchangeInfo() {
  return publicRequest('GET', '/api/v3/exchangeInfo');
}

/**
 * Get ticker prices for all symbols
 * @returns {Promise<Array>} - Array of ticker prices
 */
async function getTickerPrices() {
  return publicRequest('GET', '/api/v3/ticker/price');
}

/**
 * Get account information
 * @returns {Promise<Object>} - Account information
 */
async function getAccount() {
  return signedRequest('GET', '/api/v3/account');
}

/**
 * Get account balance with USD values
 * @returns {Promise<Object>} - Account balance with USD values
 */
async function getAccountBalance() {
  try {
    // Get account information
    const account = await getAccount();
    
    // Get ticker prices
    const tickers = await getTickerPrices();
    
    // Create price mapping
    const priceMap = {};
    tickers.forEach(ticker => {
      priceMap[ticker.symbol] = parseFloat(ticker.price);
    });
    
    // Filter non-zero balances
    const balances = account.balances.filter(
      balance => parseFloat(balance.free) > 0 || parseFloat(balance.locked) > 0
    );
    
    // Calculate USD values
    const balancesWithUSD = balances.map(balance => {
      const asset = balance.asset;
      const free = parseFloat(balance.free);
      const locked = parseFloat(balance.locked);
      const total = free + locked;
      
      let usdValue = 0;
      
      // Handle USD directly
      if (asset === 'USD' || asset === 'USDT' || asset === 'USDC' || asset === 'BUSD' || asset === 'DAI') {
        usdValue = total;
      } else {
        // Try to find direct USD pair
        const usdPair = `${asset}USD`;
        const usdtPair = `${asset}USDT`;
        
        if (priceMap[usdPair]) {
          usdValue = total * priceMap[usdPair];
        } else if (priceMap[usdtPair]) {
          usdValue = total * priceMap[usdtPair];
        } else {
          // Try to find BTC pair and convert through BTC
          const btcPair = `${asset}BTC`;
          if (priceMap[btcPair] && priceMap['BTCUSD']) {
            usdValue = total * priceMap[btcPair] * priceMap['BTCUSD'];
          }
        }
      }
      
      return {
        asset,
        free,
        locked,
        total,
        usdValue
      };
    });
    
    // Sort by USD value
    balancesWithUSD.sort((a, b) => b.usdValue - a.usdValue);
    
    // Calculate total USD value
    const totalUSDValue = balancesWithUSD.reduce((sum, balance) => sum + balance.usdValue, 0);
    
    return {
      balances: balancesWithUSD,
      totalUSDValue,
      rawData: account
    };
  } catch (error) {
    console.error('Error getting account balance:', error);
    throw error;
  }
}

/**
 * Test the API connection
 * @returns {Promise<Object>} - Test results
 */
async function testConnection() {
  try {
    // Test public API
    const pingResult = await publicRequest('GET', '/api/v3/ping');
    
    // Test private API
    let accountResult = null;
    let error = null;
    
    try {
      accountResult = await getAccount();
    } catch (err) {
      error = err.message;
    }
    
    return {
      publicApiWorking: true,
      privateApiWorking: !!accountResult,
      error,
      accountResult
    };
  } catch (error) {
    console.error('Error testing connection:', error);
    return {
      publicApiWorking: false,
      privateApiWorking: false,
      error: error.message
    };
  }
}

module.exports = {
  configure,
  getExchangeInfo,
  getTickerPrices,
  getAccount,
  getAccountBalance,
  testConnection,
  publicRequest,
  signedRequest
};
