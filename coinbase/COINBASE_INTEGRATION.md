# Coinbase Integration Guide

This document provides a comprehensive guide for integrating Coinbase API into the Strategiz platform.

## Overview

The Coinbase integration allows users to:
- View their Coinbase account balances
- See transaction history
- Access raw data directly from the Coinbase API for debugging and transparency

## Authentication

Coinbase uses OAuth 2.0 for API authentication. The integration requires:
- API Key
- API Secret
- Passphrase

These credentials should be stored securely in environment variables and never exposed in client-side code.

## API Endpoints

The integration exposes the following endpoints:

### 1. Balance Endpoint

```
GET /api/coinbase/balance/:userId
```

This endpoint returns the user's Coinbase account balances with USD values calculated for each asset.

### 2. Raw Data Endpoint

```
GET /api/coinbase/raw-data/:userId
```

This endpoint returns the completely unmodified raw data from the Coinbase API, providing full transparency for debugging and verification purposes.

## Rate Limiting

The Coinbase API has rate limits that must be respected:
- Public endpoints: 3 requests per second
- Private endpoints: 5 requests per second

Our implementation includes rate limiting to ensure compliance with these restrictions.

## Caching

To minimize API calls and improve performance, responses are cached for:
- Balance data: 5 minutes
- Raw data: 2 minutes

## Error Handling

The integration includes robust error handling for:
- Authentication failures
- Rate limit errors
- Network issues
- Malformed responses

## Security Considerations

1. API keys are stored securely in environment variables
2. All requests use HTTPS
3. Authentication headers are never logged
4. User verification is performed for all private endpoints

## Testing

Comprehensive tests are included to verify:
- Authentication flow
- Data retrieval
- Error handling
- Rate limiting

## Troubleshooting

For common issues and solutions, see the COINBASE_TROUBLESHOOTING.md document.
