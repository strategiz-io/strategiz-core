# Exchange Integrations

This document provides detailed information about the cryptocurrency exchange integrations in the Strategiz Core backend.

## Integration Principles

All exchange integrations in Strategiz follow these core principles:

1. **Real Data Only**: All implementations use real API data and real user credentials, never mocks or test data
2. **Transparency**: Data shown to users is as close as possible to what comes from the APIs, with minimal transformation
3. **Security**: API keys are stored securely and all API calls are made server-side
4. **Consistency**: Common interface patterns are used across different exchange integrations

## Exchange-Specific Notes

### Binance US

#### Integration Details
- Uses REST API with HMAC SHA256 signature authentication
- Requires API key and secret for authenticated endpoints
- Rate limited to 1200 requests per minute for regular endpoints

#### Data Handling
- Balances are returned in the "balances" array within the account data
- Admin page displays completely unmodified raw data from the API
- Asset names follow a standardized format (e.g., "BTCUSDT")

#### Configuration
- Required credentials: API Key, API Secret
- Optional: Additional rate limiting settings

### Kraken

#### Integration Details
- Uses REST API with HMAC authentication
- Requires API key and secret for authenticated endpoints
- Rate limited to 15 requests per 45 seconds for private endpoints

#### Data Handling
- Asset names in Kraken have special prefixes (X for crypto, Z for fiat)
- The admin page shows the original asset name from the API alongside the cleaned version
- Special case handling for assets with ".F" suffix (futures contracts)

#### Configuration
- Required credentials: API Key, API Secret
- Optional: Additional rate limiting settings

### Coinbase

#### Integration Details
- Uses OAuth 2.0 for authentication
- Requires API key and secret for authentication
- Rate limited to 10000 requests per hour

#### Data Handling
- Account data includes both fiat and crypto balances
- Transactions include detailed fee information
- Admin page shows complete unmodified API responses

#### Configuration
- Required credentials: API Key, API Secret
- Optional: OAuth refresh token for extended access

### Uniswap

#### Integration Details
- Requires an Ethereum provider URL (e.g., Infura)
- Interacts directly with the Ethereum blockchain
- Uses ethers.js for blockchain interactions

#### Data Handling
- Liquidity positions are calculated from on-chain data
- Includes historical position value calculations
- Admin page shows raw blockchain data and transaction history

#### Configuration
- Required: Ethereum provider URL
- Required: User's Ethereum wallet address
- Optional: Gas price settings for transaction monitoring

## Raw Data Transparency

For each exchange integration, we provide special admin endpoints that return the completely unmodified data from the exchange APIs. This serves several purposes:

1. **Debugging**: Helps diagnose issues with data transformation
2. **Transparency**: Shows exactly what data we're receiving from exchanges
3. **Verification**: Allows verification that displayed data matches source data
4. **Development**: Assists developers in understanding the raw API response format

These raw data endpoints are:
- `GET /api/binanceus/raw-data/:userId`
- `GET /api/kraken/raw-data/:userId`
- `GET /api/coinbase/raw-data/:userId`
- `GET /api/uniswap/raw-data/:userId/:walletAddress`

Access to raw data endpoints is restricted to admin users only.

## Adding a New Exchange

To add a new exchange integration to Strategiz Core:

1. Create a new client module in the `client/` directory
2. Implement the exchange client with proper authentication and rate limiting
3. Create a corresponding data module for any exchange-specific data models
4. Create a service module that uses the client module
5. Create an API module with controllers for the required endpoints
6. Add appropriate unit and integration tests
7. Update configuration to include the new exchange

For detailed implementation guidelines, refer to the [Developer Guide](DEVELOPER_GUIDE.md).
