# API Keys Configuration for Market Data

This service uses multiple APIs to fetch real-time market data. Here's how to configure them:

## CoinGecko API (Cryptocurrency Data)

CoinGecko provides free cryptocurrency market data with reasonable rate limits.

- **Free Tier**: No API key required, but rate limited to 10-50 calls/minute
- **Pro API Key**: Sign up at https://www.coingecko.com/en/api/pricing for higher limits
- **Configuration**: Set `coingecko.api.key` in application properties or environment variable `COINGECKO_API_KEY`

## Alpha Vantage API (Stock Market Data)

Alpha Vantage provides free stock market data with an API key.

- **Free API Key**: Get one at https://www.alphavantage.co/support/#api-key
- **Limits**: 5 API requests per minute, 500 requests per day
- **Configuration**: Set `alphavantage.api.key` in application properties or environment variable `ALPHA_VANTAGE_API_KEY`
- **Demo Mode**: Uses `demo` as API key with limited functionality

## Coinbase API (Cryptocurrency Exchange Data)

Currently not implemented in the MarketTickerController. For future implementation:

- **Public Data**: No authentication required for public market data
- **Private Data**: Requires API key and secret from https://www.coinbase.com/settings/api

## Environment Variables

For production, set these environment variables:

```bash
export COINGECKO_API_KEY=your_coingecko_api_key_here
export ALPHA_VANTAGE_API_KEY=your_alpha_vantage_api_key_here
```

## Testing with Real Data

1. Get your API keys from the providers above
2. Set them in your environment or application properties
3. Run the application and check the `/v1/market/tickers` endpoint
4. The controller will fall back to demo data if API calls fail

## Rate Limiting

Be aware of rate limits:
- CoinGecko: 10-50 calls/minute (free tier)
- Alpha Vantage: 5 calls/minute (free tier)
- The controller caches responses for 30 seconds to help with rate limits