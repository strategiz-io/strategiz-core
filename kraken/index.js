/**
 * Kraken API Integration for Strategiz
 * 
 * This module provides a unified interface for interacting with the Kraken API.
 */

const axios = require('axios');
const crypto = require('crypto');
const qs = require('querystring');
require('dotenv').config();

// API URL
const KRAKEN_API_URL = 'https://api.kraken.com';

// Default credentials from environment variables
let credentials = {
  apiKey: process.env.KRAKEN_API_KEY || '',
  apiSecret: process.env.KRAKEN_API_SECRET || ''
};

/**
 * Configure the Kraken API credentials
 * @param {Object} config - The configuration object
 */
function configure(config) {
  if (config.apiKey) credentials.apiKey = config.apiKey;
  if (config.apiSecret) credentials.apiSecret = config.apiSecret;
}

/**
 * Get the server time from Kraken API
 * @returns {Promise<Object>} - The server time
 */
async function getServerTime() {
  try {
    const response = await axios.get(`${KRAKEN_API_URL}/0/public/Time`);
    return response.data;
  } catch (error) {
    console.error('Error getting server time:', error);
    throw error;
  }
}

/**
 * Create a signature for private Kraken API requests
 * @param {string} path - API endpoint path
 * @param {Object} request - Request data
 * @param {number} nonce - Nonce value
 * @returns {string} - Request signature
 */
function getMessageSignature(path, request, nonce) {
  const message = qs.stringify(request);
  const secret_buffer = Buffer.from(credentials.apiSecret, 'base64');
  const hash = crypto.createHash('sha256');
  const hmac = crypto.createHmac('sha512', secret_buffer);
  const hash_digest = hash.update(nonce + message).digest('binary');
  const hmac_digest = hmac.update(path + hash_digest, 'binary').digest('base64');
  return hmac_digest;
}

/**
 * Make a public request to Kraken API
 * @param {string} method - HTTP method
 * @param {string} endpoint - API endpoint
 * @param {Object} params - Request parameters
 * @returns {Promise<Object>} - API response
 */
async function publicRequest(method, endpoint, params = {}) {
  try {
    const url = `${KRAKEN_API_URL}${endpoint}`;
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
 * Make a private request to Kraken API
 * @param {string} method - HTTP method
 * @param {string} endpoint - API endpoint
 * @param {Object} params - Request parameters
 * @returns {Promise<Object>} - API response
 */
async function privateRequest(method, endpoint, params = {}) {
  try {
    const url = `${KRAKEN_API_URL}${endpoint}`;
    const nonce = Date.now().toString();
    
    const data = {
      nonce,
      ...params
    };
    
    const signature = getMessageSignature(endpoint, data, nonce);
    
    const response = await axios({
      method,
      url,
      data: qs.stringify(data),
      headers: {
        'API-Key': credentials.apiKey,
        'API-Sign': signature,
        'Content-Type': 'application/x-www-form-urlencoded'
      }
    });
    
    return response.data;
  } catch (error) {
    console.error(`Error making private request to ${endpoint}:`, error);
    throw error;
  }
}

/**
 * Get asset pairs information
 * @param {Array<string>} pairs - Asset pairs to get info for
 * @returns {Promise<Object>} - Asset pairs information
 */
async function getAssetPairs(pairs = []) {
  const params = {};
  if (pairs.length > 0) {
    params.pair = pairs.join(',');
  }
  return publicRequest('GET', '/0/public/AssetPairs', params);
}

/**
 * Get ticker information
 * @param {Array<string>} pairs - Asset pairs to get ticker info for
 * @returns {Promise<Object>} - Ticker information
 */
async function getTicker(pairs = []) {
  if (pairs.length === 0) {
    throw new Error('At least one pair is required');
  }
  
  const params = {
    pair: pairs.join(',')
  };
  
  return publicRequest('GET', '/0/public/Ticker', params);
}

/**
 * Get account balance
 * @returns {Promise<Object>} - Account balance
 */
async function getBalance() {
  return privateRequest('POST', '/0/private/Balance');
}

/**
 * Get open orders
 * @returns {Promise<Object>} - Open orders
 */
async function getOpenOrders() {
  return privateRequest('POST', '/0/private/OpenOrders');
}

/**
 * Get closed orders
 * @returns {Promise<Object>} - Closed orders
 */
async function getClosedOrders() {
  return privateRequest('POST', '/0/private/ClosedOrders');
}

/**
 * Get trade history
 * @returns {Promise<Object>} - Trade history
 */
async function getTradeHistory() {
  return privateRequest('POST', '/0/private/TradesHistory');
}

/**
 * Clean Kraken asset names by removing X/Z prefixes
 * @param {string} asset - The asset name to clean
 * @returns {string} - Cleaned asset name
 */
function getCleanAssetName(asset) {
  // Kraken prefixes most crypto assets with X and fiat with Z
  // This function removes those prefixes for better readability
  
  // Special cases that don't follow the X/Z prefix rule
  const specialCases = {
    'XXBT': 'BTC',
    'XETH': 'ETH',
    'XXRP': 'XRP',
    'XXLM': 'XLM',
    'XLTC': 'LTC',
    'XDAO': 'DAO',
    'XETC': 'ETC',
    'XICN': 'ICN',
    'XMLN': 'MLN',
    'XNMC': 'NMC',
    'XREP': 'REP',
    'XXDG': 'DOGE',
    'XXMR': 'XMR',
    'XXVN': 'XVN',
    'XZEC': 'ZEC',
    'ZUSD': 'USD',
    'ZEUR': 'EUR',
    'ZGBP': 'GBP',
    'ZJPY': 'JPY',
    'ZCAD': 'CAD',
    'ZAUD': 'AUD'
  };
  
  // Check if it's a special case
  if (specialCases[asset]) {
    return specialCases[asset];
  }
  
  // Remove X or Z prefix if present
  if (asset.startsWith('X') || asset.startsWith('Z')) {
    return asset.substring(1);
  }
  
  return asset;
}

/**
 * Get account balance with USD values
 * @returns {Promise<Object>} - Account balance with USD values
 */
async function getAccountBalance() {
  try {
    // Get balance
    const balanceResponse = await getBalance();
    
    if (balanceResponse.error && balanceResponse.error.length > 0) {
      throw new Error(balanceResponse.error.join(', '));
    }
    
    const balances = balanceResponse.result;
    
    // Get pairs for USD conversion
    const assetPairs = await getAssetPairs();
    
    if (assetPairs.error && assetPairs.error.length > 0) {
      throw new Error(assetPairs.error.join(', '));
    }
    
    // Find all USD pairs
    const usdPairs = [];
    for (const pair in assetPairs.result) {
      if (pair.endsWith('USD') || pair.endsWith('USDT') || pair.endsWith('USDC')) {
        usdPairs.push(pair);
      }
    }
    
    // Get ticker prices for USD pairs
    const tickerResponse = await getTicker(usdPairs);
    
    if (tickerResponse.error && tickerResponse.error.length > 0) {
      throw new Error(tickerResponse.error.join(', '));
    }
    
    const tickers = tickerResponse.result;
    
    // Create price mapping
    const priceMap = {};
    for (const pair in tickers) {
      const baseAsset = pair.replace('USD', '').replace('USDT', '').replace('USDC', '');
      priceMap[baseAsset] = parseFloat(tickers[pair].c[0]);
    }
    
    // Add USD price directly
    priceMap['USD'] = 1;
    priceMap['USDT'] = 1;
    priceMap['USDC'] = 1;
    
    // Process balances
    const processedBalances = [];
    let totalUSDValue = 0;
    
    for (const asset in balances) {
      const balance = parseFloat(balances[asset]);
      if (balance <= 0) continue;
      
      const cleanAssetName = getCleanAssetName(asset);
      let usdValue = 0;
      
      // Calculate USD value
      if (priceMap[cleanAssetName]) {
        usdValue = balance * priceMap[cleanAssetName];
      } else if (cleanAssetName === 'USD' || cleanAssetName === 'USDT' || cleanAssetName === 'USDC') {
        usdValue = balance;
      }
      
      totalUSDValue += usdValue;
      
      processedBalances.push({
        asset: cleanAssetName,
        originalAsset: asset, // Keep the original asset name for reference
        balance,
        usdValue
      });
    }
    
    // Sort by USD value
    processedBalances.sort((a, b) => b.usdValue - a.usdValue);
    
    return {
      balances: processedBalances,
      totalUSDValue,
      rawData: balanceResponse.result
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
    const serverTimeResult = await getServerTime();
    
    // Test private API
    let balanceResult = null;
    let error = null;
    
    try {
      balanceResult = await getBalance();
    } catch (err) {
      error = err.message;
    }
    
    return {
      publicApiWorking: serverTimeResult && serverTimeResult.result,
      privateApiWorking: balanceResult && balanceResult.result,
      error,
      balanceResult
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
  getServerTime,
  getAssetPairs,
  getTicker,
  getBalance,
  getOpenOrders,
  getClosedOrders,
  getTradeHistory,
  getAccountBalance,
  getCleanAssetName,
  testConnection,
  publicRequest,
  privateRequest
};
