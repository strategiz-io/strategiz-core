# Portfolio Module Testing Guide

## Overview
This guide explains how to test the portfolio module endpoints after the Kraken integration.

## Endpoints

The portfolio module provides the following endpoints:

1. **Portfolio Summary** - `/v1/portfolio/summary`
   - Lightweight data for dashboard
   - Returns total value, day change, top holdings

2. **Portfolio Overview** - `/v1/portfolio/overview`
   - Complete portfolio data for portfolio page
   - Aggregates all connected providers

3. **Provider Portfolio** - `/v1/portfolio/providers/{providerId}`
   - Specific provider data (e.g., `/v1/portfolio/providers/kraken`)
   
4. **Refresh Portfolio** - `/v1/portfolio/refresh`
   - Triggers data refresh for all providers

## Authentication Required

All portfolio endpoints require Bearer token authentication.

## Testing Methods

### Method 1: Using Frontend Application

1. Start the backend:
   ```bash
   VAULT_ADDR=http://localhost:8200 VAULT_TOKEN=hvs.zjEbQaSDy8WmdiW0vEl81tFH \
   java -jar application/target/application-1.0-SNAPSHOT.jar --spring.profiles.active=dev-https --server.port=8443
   ```

2. Start the frontend:
   ```bash
   cd ../strategiz-ui
   REACT_APP_API_PROTOCOL=https REACT_APP_API_PORT=8443 REACT_APP_ACCEPT_SELF_SIGNED_CERTS=true \
   TSC_COMPILE_ON_ERROR=true NODE_OPTIONS='--max-old-space-size=8192' npm start
   ```

3. Log in through the UI
4. Open Browser DevTools → Network tab
5. Look for API calls to get the Bearer token from Authorization header
6. Use the token with the test script:
   ```bash
   ./test-portfolio-simple.sh 'YOUR_TOKEN_HERE'
   ```

### Method 2: Using TOTP Authentication

1. Register a TOTP device for a test user
2. Generate TOTP code using Google Authenticator
3. Authenticate to get a token:
   ```bash
   curl -k -X POST https://localhost:8443/v1/auth/totp/authentications \
     -H "Content-Type: application/json" \
     -d '{
       "userId": "YOUR_USER_ID",
       "code": "123456",
       "deviceId": "test-device"
     }'
   ```
4. Extract the `accessToken` from response
5. Use with test script

### Method 3: Manual cURL Testing

Once you have a token, test individual endpoints:

```bash
# Test Portfolio Summary
curl -k -X GET https://localhost:8443/v1/portfolio/summary \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Accept: application/json" | python3 -m json.tool

# Test Portfolio Overview
curl -k -X GET https://localhost:8443/v1/portfolio/overview \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Accept: application/json" | python3 -m json.tool

# Test Kraken Provider
curl -k -X GET https://localhost:8443/v1/portfolio/providers/kraken \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Accept: application/json" | python3 -m json.tool

# Refresh Portfolio
curl -k -X POST https://localhost:8443/v1/portfolio/refresh \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Accept: application/json" | python3 -m json.tool
```

## Expected Responses

### Portfolio Summary
```json
{
  "totalValue": 50000.00,
  "dayChange": 1234.56,
  "dayChangePercent": 2.53,
  "totalProfitLoss": 5678.90,
  "totalProfitLossPercent": 12.78,
  "topHoldings": [...],
  "providers": [...]
}
```

### Portfolio Overview
```json
{
  "summary": {...},
  "providers": [
    {
      "providerId": "kraken",
      "providerName": "Kraken",
      "connected": true,
      "totalValue": 25000.00,
      "positions": [...]
    }
  ],
  "allPositions": [...],
  "performance": {...}
}
```

### Provider Portfolio (Kraken)
```json
{
  "providerId": "kraken",
  "providerName": "Kraken",
  "accountType": "crypto",
  "connected": true,
  "totalValue": 25000.00,
  "positions": [
    {
      "symbol": "BTC",
      "name": "Bitcoin",
      "quantity": 0.5,
      "currentPrice": 50000.00,
      "currentValue": 25000.00
    }
  ],
  "syncStatus": "synced",
  "lastSynced": 1234567890000
}
```

## Troubleshooting

### Authentication Error
- Ensure token is valid and not expired
- Check token format: should start with `v4.local.`
- Verify Bearer prefix in Authorization header

### No Data Returned
- Check if user has connected providers
- Verify provider_data collection in Firestore
- Check demo mode setting for user

### Connection Refused
- Ensure backend is running on port 8443
- Check HTTPS certificate acceptance
- Verify CORS settings

## Demo Mode vs Live Mode

The portfolio service checks user's demo mode setting:
- **Demo Mode**: Returns stored/mock data from Firestore
- **Live Mode**: Attempts to fetch real-time data from providers

To toggle demo mode, update user profile through the profile service endpoints.