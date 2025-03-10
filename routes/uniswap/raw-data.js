const express = require('express');
const router = express.Router();
const admin = require('firebase-admin');
const { ethers } = require('ethers');
const { abi: INonfungiblePositionManagerABI } = require('@uniswap/v3-periphery/artifacts/contracts/interfaces/INonfungiblePositionManager.sol/INonfungiblePositionManager.json');
const { RateLimiterMemory } = require('rate-limiter-flexible');

// Uniswap contract addresses
const UNISWAP_POSITION_MANAGER = '0xC36442b4a4522E871399CD717aBDD847Ab11FE88'; // NFT Position Manager

// Create a specific rate limiter for raw data admin endpoints
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
  const walletAddress = req.params.walletAddress;
  const cacheKey = `${userId}-${walletAddress}`;
  
  // Check if we have fresh cached data
  if (rawDataCache[cacheKey] && Date.now() - rawDataCache[cacheKey].timestamp < CACHE_TTL) {
    return res.json(rawDataCache[cacheKey].data);
  }
  
  // Apply rate limiting
  try {
    await rawDataRateLimiter.consume(req.ip);
    next();
  } catch (error) {
    console.error('Rate limit exceeded for Uniswap raw data API:', req.ip);
    return res.status(429).json({ 
      error: 'Too many requests to Uniswap raw data API. Please try again later.',
      retryAfter: Math.ceil(error.msBeforeNext / 1000) || 60
    });
  }
};

// GET /api/uniswap/raw-data/:userId/:walletAddress
router.get('/:userId/:walletAddress', checkCacheAndRateLimit, async (req, res) => {
  const userId = req.params.userId;
  const walletAddress = req.params.walletAddress;
  const cacheKey = `${userId}-${walletAddress}`;
  
  try {
    // Validate Ethereum address format
    if (!/^0x[a-fA-F0-9]{40}$/.test(walletAddress)) {
      return res.status(400).json({ error: 'Invalid Ethereum wallet address' });
    }
    
    // Get Ethereum provider URL from environment or user settings
    let providerUrl = process.env.ETH_PROVIDER_URL || `https://mainnet.infura.io/v3/${process.env.INFURA_API_KEY}`;
    
    // Check if user has custom provider settings
    const db = admin.firestore();
    const doc = await db.collection('users').doc(userId).collection('settings').doc('ethereum').get();
    
    if (doc.exists) {
      const settings = doc.data();
      if (settings.providerUrl) {
        providerUrl = settings.providerUrl;
      }
    }
    
    // Initialize provider
    const provider = new ethers.providers.JsonRpcProvider(providerUrl);
    
    // Get completely unmodified raw data directly from the blockchain
    const rawData = {
      walletAddress,
      ethBalance: null,
      positions: [],
      positionDetails: [],
      timestamp: Date.now()
    };
    
    // Get ETH balance - completely unmodified
    const ethBalance = await provider.getBalance(walletAddress);
    rawData.ethBalance = {
      wei: ethBalance.toString(),
      eth: ethers.utils.formatEther(ethBalance)
    };
    
    // Initialize the position manager contract
    const positionManager = new ethers.Contract(
      UNISWAP_POSITION_MANAGER,
      INonfungiblePositionManagerABI,
      provider
    );
    
    // Get the balance of positions (NFTs) - completely unmodified
    const balance = await positionManager.balanceOf(walletAddress);
    
    // Get all position IDs - completely unmodified
    for (let i = 0; i < balance; i++) {
      const tokenId = await positionManager.tokenOfOwnerByIndex(walletAddress, i);
      rawData.positions.push(tokenId.toString());
      
      // Get position details - completely unmodified
      const position = await positionManager.positions(tokenId);
      rawData.positionDetails.push({
        tokenId: tokenId.toString(),
        rawPosition: {
          nonce: position.nonce.toString(),
          operator: position.operator,
          token0: position.token0,
          token1: position.token1,
          fee: position.fee.toString(),
          tickLower: position.tickLower.toString(),
          tickUpper: position.tickUpper.toString(),
          liquidity: position.liquidity.toString(),
          feeGrowthInside0LastX128: position.feeGrowthInside0LastX128.toString(),
          feeGrowthInside1LastX128: position.feeGrowthInside1LastX128.toString(),
          tokensOwed0: position.tokensOwed0.toString(),
          tokensOwed1: position.tokensOwed1.toString()
        }
      });
    }
    
    // Cache the completely unmodified raw data
    rawDataCache[cacheKey] = {
      data: rawData,
      timestamp: Date.now()
    };
    
    // Return the completely unmodified raw data
    return res.json(rawData);
  } catch (error) {
    console.error('Error fetching Uniswap raw data:', error);
    return res.status(500).json({ 
      error: 'Failed to fetch Uniswap raw data',
      message: error.message
    });
  }
});

module.exports = router;
