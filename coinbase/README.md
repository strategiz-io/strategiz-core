# Coinbase Integration for Strategiz

This module provides a unified interface for interacting with the Coinbase API to retrieve account balances and other data.

## Features

- Fetch account balances with USD values
- Access raw API data for debugging and transparency
- Rate limiting to comply with Coinbase API restrictions
- Caching to improve performance and reduce API calls
- Comprehensive error handling

## Setup

1. Create a Coinbase API key with the following permissions:
   - `wallet:accounts:read`
   - `wallet:transactions:read`

2. Add the following environment variables to your `.env` file:
   ```
   COINBASE_API_KEY=your_api_key
   COINBASE_API_SECRET=your_api_secret
   COINBASE_API_PASSPHRASE=your_api_passphrase
   ```

## API Endpoints

### Balance Endpoint

```
GET /api/coinbase/balance/:userId
```

Returns the user's Coinbase account balances with USD values calculated for each asset.

#### Response Format

```json
{
  "balances": [
    {
      "asset": "BTC",
      "name": "Bitcoin",
      "free": 0.12345,
      "locked": 0,
      "total": 0.12345,
      "usdValue": 5432.10
    },
    // Additional assets...
  ],
  "totalUSDValue": 10000.00
}
```

### Raw Data Endpoint

```
GET /api/coinbase/raw-data/:userId
```

Returns the completely unmodified raw data from the Coinbase API, providing full transparency for debugging and verification purposes.

## Usage in Code

```javascript
const coinbase = require('./coinbase');

// Configure with custom credentials (optional)
coinbase.configure({
  apiKey: 'your_api_key',
  apiSecret: 'your_api_secret',
  apiPassphrase: 'your_api_passphrase'
});

// Get account balances
const balances = await coinbase.getAccountBalances();
console.log(balances);

// Get raw account data
const rawData = await coinbase.getAccounts();
console.log(rawData);
```

## Error Handling

The module includes comprehensive error handling for:
- Authentication failures
- Rate limit errors
- Network issues
- Malformed responses

## Documentation

For more detailed information, see:
- [Coinbase Integration Guide](./COINBASE_INTEGRATION.md)
- [Troubleshooting Guide](./COINBASE_TROUBLESHOOTING.md)

## Security Considerations

- API keys are stored securely in environment variables
- All requests use HTTPS
- Authentication headers are never logged
- User verification is performed for all private endpoints
