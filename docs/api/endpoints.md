---
title: API Endpoints
description: Complete API reference for Strategiz platform
---

# API Endpoints

This section provides comprehensive API documentation for all Strategiz services.

## Authentication API

### Sign In/Sign Up
- `POST /auth/signin` - User sign in
- `POST /auth/signup` - User registration
- `POST /auth/signout` - User sign out

### TOTP Authentication
- `POST /auth/totp/setup/initialize` - Initialize TOTP setup
- `POST /auth/totp/setup/complete` - Complete TOTP setup
- `POST /auth/totp/authenticate` - Authenticate with TOTP
- `GET /auth/totp/setup/status/{userId}` - Check TOTP setup status

### OAuth Authentication
- `GET /auth/oauth/{provider}/authorize` - OAuth authorization
- `POST /auth/oauth/{provider}/callback` - OAuth callback
- `DELETE /auth/oauth/{provider}/disconnect` - Disconnect OAuth provider

### SMS Authentication
- `POST /auth/sms/send` - Send SMS verification code
- `POST /auth/sms/verify` - Verify SMS code

## Portfolio API

- `GET /portfolio` - Get user portfolios
- `POST /portfolio` - Create new portfolio
- `PUT /portfolio/{id}` - Update portfolio
- `DELETE /portfolio/{id}` - Delete portfolio
- `GET /portfolio/{id}/performance` - Get portfolio performance metrics

## Strategy API

- `GET /strategy` - Get user strategies
- `POST /strategy` - Create new strategy
- `PUT /strategy/{id}` - Update strategy
- `DELETE /strategy/{id}` - Delete strategy
- `POST /strategy/{id}/execute` - Execute strategy
- `GET /strategy/{id}/backtest` - Run strategy backtest

## Provider API

- `GET /provider` - Get available providers
- `POST /provider/connect` - Connect to a provider
- `DELETE /provider/{id}/disconnect` - Disconnect from provider
- `GET /provider/{id}/status` - Check provider connection status
- `GET /provider/{id}/data` - Get data from provider

## User API

- `GET /user/profile` - Get user profile
- `PUT /user/profile` - Update user profile
- `GET /user/preferences` - Get user preferences
- `PUT /user/preferences` - Update user preferences

## Device API

- `GET /device` - Get registered devices
- `POST /device/register` - Register new device
- `DELETE /device/{id}` - Remove device
- `PUT /device/{id}/verify` - Verify device

## Base URL

All API endpoints are relative to: `https://api.strategiz.io/v1`

## Authentication

All API calls require a valid JWT token in the Authorization header:

```
Authorization: Bearer <your-jwt-token>
```

## Rate Limiting

API calls are rate-limited based on your subscription plan:
- **Free**: 100 requests per hour
- **Pro**: 1,000 requests per hour
- **Enterprise**: 10,000 requests per hour

## Error Responses

All API endpoints return consistent error responses:

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Invalid request parameters",
    "details": {
      "field": "email",
      "message": "Email is required"
    }
  }
}
``` 