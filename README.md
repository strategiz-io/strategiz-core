# Strategiz Core

Server-side API and services for the Strategiz platform. This repository contains all the backend code for handling exchange API integrations, user authentication, and data processing.

## Features

- **Exchange API Integrations**:
  - Binance US: Complete API integration with rate limiting and caching
  - Kraken: Complete API integration with rate limiting and caching
  - Coinbase: Complete API integration with rate limiting and caching
  - Uniswap: Complete integration with Ethereum blockchain for liquidity positions
  - Raw data endpoints for admin pages showing completely unmodified API responses

- **Rate Limiting**:
  - Global rate limiting for all API requests
  - Exchange-specific rate limiting for regular endpoints
  - More restrictive rate limiting for admin raw data endpoints

- **Caching**:
  - Server-side caching for both regular and raw data responses
  - Configurable TTL (Time To Live) for different types of data

- **User Authentication**:
  - Firebase Authentication integration
  - Secure storage of user credentials

## Getting Started

### Prerequisites

- Node.js (v14 or higher)
- npm (v6 or higher)
- Firebase account and project

### Installation

1. Clone the repository:
   ```
   git clone https://github.com/cuztomizer/strategiz-core.git
   cd strategiz-core
   ```

2. Install dependencies:
   ```
   npm install
   ```

3. Create a `.env` file in the root directory based on the `.env.example` file
   ```
   cp .env.example .env
   ```
   Then edit the `.env` file with your actual credentials.

4. Set up Firebase Service Account:
   - Go to your Firebase project settings
   - Navigate to "Service accounts"
   - Click "Generate new private key"
   - Save the JSON file as `serviceAccountKey.json` in the root directory

5. Start the server:
   ```
   npm run dev
   ```

## API Endpoints

### Health Check
- `GET /health` - Check if the server is running

### Binance US API
- `GET /api/binanceus/balance/:userId` - Get user's Binance US account balance
- `GET /api/binanceus/raw-data/:userId` - Get completely unmodified raw data from Binance US API (admin only)

### Kraken API
- `GET /api/kraken/balance/:userId` - Get user's Kraken account balance
- `GET /api/kraken/raw-data/:userId` - Get completely unmodified raw data from Kraken API (admin only)

### Coinbase API
- `GET /api/coinbase/balance/:userId` - Get user's Coinbase account balance
- `GET /api/coinbase/raw-data/:userId` - Get completely unmodified raw data from Coinbase API (admin only)

### Uniswap API
- `GET /api/uniswap/portfolio/:userId/:walletAddress` - Get user's Uniswap portfolio
- `GET /api/uniswap/raw-data/:userId/:walletAddress` - Get completely unmodified raw data from Uniswap (admin only)

## Security Features

- **Secure Credential Storage**: API keys are stored in Firebase Firestore with encryption
- **Server-side API Requests**: All API calls to exchanges are made from the server, not the client
- **User-specific Credentials**: Each user's API credentials are stored separately
- **Rate Limiting**: Prevents abuse of the API endpoints
- **CORS Protection**: Restricts which domains can access the API
- **Helmet Security Headers**: Adds various HTTP headers for enhanced security

## Exchange-Specific Notes

### Binance US
- Balances are returned in the "balances" array within the account data
- Admin page displays completely unmodified raw data from the API

### Kraken
- Asset names in Kraken have special prefixes (X for crypto, Z for fiat)
- The admin page shows the original asset name from the API alongside the cleaned version

### Coinbase
- Uses OAuth 2.0 for authentication
- Requires API key and secret for authentication

### Uniswap
- Requires an Ethereum provider URL (e.g., Infura)
- Interacts directly with the Ethereum blockchain
- Uses ethers.js for blockchain interactions

## Troubleshooting

### Server Won't Start
- Check if port 3001 is already in use
- Verify that your `serviceAccountKey.json` file is valid
- Make sure all dependencies are installed

### Authentication Errors
- Verify that your API credentials are correct
- Check if your credentials have the necessary permissions
- Ensure the system clock is synchronized (for HMAC timestamp validation)

### Rate Limiting Issues
- The server implements rate limiting to prevent API abuse
- If you're getting 429 (Too Many Requests) errors, wait and try again later
- Different endpoints have different rate limits

## Contributing

Please follow the established code style and add appropriate tests for new features.