# API Endpoints Reference

This document provides a comprehensive list of all API endpoints available in the Strategiz Core backend.

## Core Endpoints

### Health Check
- `GET /health` - Check if the server is running

## Authentication Endpoints

- `POST /api/auth/login` - Authenticate user with Firebase
- `POST /api/auth/register` - Register a new user
- `GET /api/auth/session` - Get current user session information
- `POST /api/auth/logout` - Log out the current user

## Exchange API Endpoints

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

## Portfolio Endpoints

- `GET /api/portfolio/summary/:userId` - Get a summary of the user's portfolio across all exchanges
- `GET /api/portfolio/allocation/:userId` - Get asset allocation details for the user's portfolio
- `GET /api/portfolio/performance/:userId` - Get performance metrics for the user's portfolio

## Strategy Endpoints

- `GET /api/strategy/list/:userId` - Get list of user's trading strategies
- `POST /api/strategy/create` - Create a new trading strategy
- `PUT /api/strategy/update/:strategyId` - Update an existing strategy
- `DELETE /api/strategy/delete/:strategyId` - Delete a strategy

## Response Format

All API endpoints return responses in the following JSON format:

```json
{
  "success": true,
  "data": {
    // Response data goes here
  },
  "error": null,
  "metadata": {
    "timestamp": "2025-05-31T21:37:10.000Z",
    "requestId": "abc123def456"
  }
}
```

For error responses:

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "ERROR_CODE",
    "message": "Human-readable error message",
    "details": {}
  },
  "metadata": {
    "timestamp": "2025-05-31T21:37:10.000Z",
    "requestId": "abc123def456"
  }
}
```

## Rate Limiting

All API endpoints are subject to rate limiting to prevent abuse. Rate limits vary by endpoint:

- Regular endpoints: 60 requests per minute
- Admin raw data endpoints: 10 requests per minute
- Authentication endpoints: 30 requests per minute

When a rate limit is exceeded, the API will return a 429 (Too Many Requests) response.
