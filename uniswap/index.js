/**
 * Uniswap Integration for Strategiz
 * 
 * This module provides a unified interface for interacting with the Uniswap protocol
 * to fetch positions, liquidity, and other DeFi-related data.
 */

const axios = require('axios');
const { ethers } = require('ethers');
const { abi: IUniswapV3PoolABI } = require('@uniswap/v3-core/artifacts/contracts/interfaces/IUniswapV3Pool.sol/IUniswapV3Pool.json');
const { abi: INonfungiblePositionManagerABI } = require('@uniswap/v3-periphery/artifacts/contracts/interfaces/INonfungiblePositionManager.sol/INonfungiblePositionManager.json');
require('dotenv').config();

// Ethereum provider URL (default to Infura mainnet)
const PROVIDER_URL = process.env.ETH_PROVIDER_URL || `https://mainnet.infura.io/v3/${process.env.INFURA_API_KEY}`;

// Uniswap contract addresses
const UNISWAP_POSITION_MANAGER = '0xC36442b4a4522E871399CD717aBDD847Ab11FE88'; // NFT Position Manager
const UNISWAP_FACTORY = '0x1F98431c8aD98523631AE4a59f267346ea31F984'; // Factory address

// Token price API URL
const COINGECKO_API_URL = 'https://api.coingecko.com/api/v3';

// Initialize provider
let provider;
try {
  provider = new ethers.providers.JsonRpcProvider(PROVIDER_URL);
} catch (error) {
  console.error('Failed to initialize Ethereum provider:', error);
}

// Cache for token prices (to avoid rate limiting)
const tokenPriceCache = new Map();
const TOKEN_CACHE_TTL = 5 * 60 * 1000; // 5 minutes

/**
 * Configure the Ethereum provider
 * @param {Object} config - Configuration object
 */
function configure(config) {
  if (config.providerUrl) {
    try {
      provider = new ethers.providers.JsonRpcProvider(config.providerUrl);
      console.log('Ethereum provider configured with custom URL');
    } catch (error) {
      console.error('Failed to initialize custom Ethereum provider:', error);
    }
  }
}

/**
 * Get token price from CoinGecko API
 * @param {string} tokenAddress - Token contract address
 * @returns {Promise<number>} - Token price in USD
 */
async function getTokenPrice(tokenAddress) {
  // Check cache first
  const cacheKey = tokenAddress.toLowerCase();
  const cachedData = tokenPriceCache.get(cacheKey);
  
  if (cachedData && (Date.now() - cachedData.timestamp) < TOKEN_CACHE_TTL) {
    return cachedData.price;
  }
  
  try {
    const response = await axios.get(`${COINGECKO_API_URL}/simple/token_price/ethereum`, {
      params: {
        contract_addresses: tokenAddress,
        vs_currencies: 'usd'
      }
    });
    
    const price = response.data[tokenAddress.toLowerCase()]?.usd || 0;
    
    // Update cache
    tokenPriceCache.set(cacheKey, {
      price,
      timestamp: Date.now()
    });
    
    return price;
  } catch (error) {
    console.error(`Error fetching price for token ${tokenAddress}:`, error);
    return 0;
  }
}

/**
 * Get ETH price in USD
 * @returns {Promise<number>} - ETH price in USD
 */
async function getEthPrice() {
  try {
    const response = await axios.get(`${COINGECKO_API_URL}/simple/price`, {
      params: {
        ids: 'ethereum',
        vs_currencies: 'usd'
      }
    });
    
    return response.data.ethereum.usd;
  } catch (error) {
    console.error('Error fetching ETH price:', error);
    return 0;
  }
}

/**
 * Get Uniswap positions for a wallet address
 * @param {string} walletAddress - Ethereum wallet address
 * @returns {Promise<Array>} - Array of positions
 */
async function getPositions(walletAddress) {
  if (!provider) {
    throw new Error('Ethereum provider not initialized');
  }
  
  try {
    console.log(`Fetching Uniswap positions for wallet: ${walletAddress}`);
    
    // Initialize the position manager contract
    const positionManager = new ethers.Contract(
      UNISWAP_POSITION_MANAGER,
      INonfungiblePositionManagerABI,
      provider
    );
    
    // Get the balance of positions (NFTs)
    const balance = await positionManager.balanceOf(walletAddress);
    console.log(`Found ${balance.toString()} positions`);
    
    const positions = [];
    const ethPrice = await getEthPrice();
    
    // Fetch each position
    for (let i = 0; i < balance; i++) {
      try {
        // Get token ID
        const tokenId = await positionManager.tokenOfOwnerByIndex(walletAddress, i);
        console.log(`Processing position with token ID: ${tokenId.toString()}`);
        
        // Get position details
        const position = await positionManager.positions(tokenId);
        
        // Get token addresses
        const token0Address = position.token0;
        const token1Address = position.token1;
        
        // Get token prices
        const token0Price = await getTokenPrice(token0Address);
        const token1Price = await getTokenPrice(token1Address);
        
        // Calculate USD values
        const token0Amount = ethers.utils.formatUnits(position.tokensOwed0, 18); // Assuming 18 decimals
        const token1Amount = ethers.utils.formatUnits(position.tokensOwed1, 18); // Assuming 18 decimals
        
        const token0Value = parseFloat(token0Amount) * token0Price;
        const token1Value = parseFloat(token1Amount) * token1Price;
        const totalValue = token0Value + token1Value;
        
        positions.push({
          tokenId: tokenId.toString(),
          token0: {
            address: token0Address,
            amount: token0Amount,
            price: token0Price,
            value: token0Value
          },
          token1: {
            address: token1Address,
            amount: token1Amount,
            price: token1Price,
            value: token1Value
          },
          fee: position.fee.toString(),
          liquidity: position.liquidity.toString(),
          totalValue
        });
      } catch (error) {
        console.error(`Error processing position ${i}:`, error);
      }
    }
    
    console.log(`Successfully processed ${positions.length} positions`);
    
    return {
      status: 'success',
      positions,
      totalValue: positions.reduce((sum, pos) => sum + pos.totalValue, 0)
    };
  } catch (error) {
    console.error('Error fetching Uniswap positions:', error);
    return {
      status: 'error',
      message: error.message,
      positions: [],
      totalValue: 0
    };
  }
}

/**
 * Get wallet token balances
 * @param {string} walletAddress - Ethereum wallet address
 * @returns {Promise<Object>} - Token balances with USD values
 */
async function getWalletBalances(walletAddress) {
  if (!provider) {
    throw new Error('Ethereum provider not initialized');
  }
  
  try {
    console.log(`Fetching token balances for wallet: ${walletAddress}`);
    
    // Get ETH balance
    const ethBalance = await provider.getBalance(walletAddress);
    const ethAmount = ethers.utils.formatEther(ethBalance);
    const ethPrice = await getEthPrice();
    const ethValue = parseFloat(ethAmount) * ethPrice;
    
    // For a complete solution, we would need to:
    // 1. Get a list of ERC20 tokens owned by the wallet (using an API like Etherscan or Moralis)
    // 2. For each token, get the balance and price
    // This is simplified for the example
    
    return {
      status: 'success',
      balances: [
        {
          symbol: 'ETH',
          name: 'Ethereum',
          address: 'native',
          amount: ethAmount,
          price: ethPrice,
          value: ethValue
        }
        // Other tokens would be added here
      ],
      totalValue: ethValue // Sum of all token values
    };
  } catch (error) {
    console.error('Error fetching wallet balances:', error);
    return {
      status: 'error',
      message: error.message,
      balances: [],
      totalValue: 0
    };
  }
}

/**
 * Get combined DeFi portfolio (positions + token balances)
 * @param {string} walletAddress - Ethereum wallet address
 * @returns {Promise<Object>} - Combined portfolio data
 */
async function getPortfolio(walletAddress) {
  try {
    console.log(`Fetching DeFi portfolio for wallet: ${walletAddress}`);
    
    // Get Uniswap positions
    const positionsData = await getPositions(walletAddress);
    
    // Get wallet token balances
    const balancesData = await getWalletBalances(walletAddress);
    
    // Calculate total portfolio value
    const totalValue = positionsData.totalValue + balancesData.totalValue;
    
    return {
      status: 'success',
      walletAddress,
      positions: positionsData.positions,
      balances: balancesData.balances,
      totalValue
    };
  } catch (error) {
    console.error('Error fetching DeFi portfolio:', error);
    return {
      status: 'error',
      message: error.message,
      walletAddress,
      positions: [],
      balances: [],
      totalValue: 0
    };
  }
}

/**
 * Test the connection to Ethereum provider
 * @returns {Promise<Object>} - Test results
 */
async function testConnection() {
  try {
    if (!provider) {
      return {
        status: 'error',
        message: 'Ethereum provider not initialized'
      };
    }
    
    // Test connection by getting network info
    const network = await provider.getNetwork();
    
    return {
      status: 'success',
      network: {
        name: network.name,
        chainId: network.chainId
      },
      message: `Connected to ${network.name} (Chain ID: ${network.chainId})`
    };
  } catch (error) {
    console.error('Error testing Ethereum connection:', error);
    return {
      status: 'error',
      message: error.message
    };
  }
}

module.exports = {
  configure,
  getPositions,
  getWalletBalances,
  getPortfolio,
  testConnection
};
