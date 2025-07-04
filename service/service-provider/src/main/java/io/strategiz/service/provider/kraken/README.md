# Kraken Provider Integration

This document provides detailed instructions for setting up and configuring the Kraken provider integration with the Strategiz platform.

## Overview

The Kraken integration allows users to connect their Kraken accounts to Strategiz via OAuth2 authentication. Once connected, users can:
- View their account balances
- Retrieve open orders
- Place new orders
- Get account information

## Prerequisites

- Kraken developer account (https://www.kraken.com/features/api)
- Registered Strategiz application with Kraken

## Registration Process

### 1. Register as a Kraken API Developer
- Visit the [Kraken API portal](https://www.kraken.com/features/api)
- Sign in with your existing Kraken account or create a new developer account
- Navigate to the developer dashboard

### 2. Register a New Application
- Application Name: "Strategiz" (or your preferred name)
- Website URL: Your application's homepage URL
- Redirect URI: The callback URL where Kraken will send the authorization code
  - Production: `https://your-app.com/api/v1/provider/callback/kraken`
  - Development: `http://localhost:8080/api/v1/provider/callback/kraken`
- Required Scopes:
  - `read`: For reading account information and balances
  - `trade`: For placing orders and retrieving trade history
  - Additional scopes as needed based on your implementation

### 3. API Credentials
After registration, Kraken will provide:
- **Client ID**: Public identifier for your application
- **Client Secret**: Private key that must be kept secure

## Configuration

### Adding Credentials to Strategiz

1. **Development/Testing Environment**
   - Add the following to your `application.properties` or `application.yml`:
   ```properties
   provider.kraken.client-id=your_client_id_here
   provider.kraken.client-secret=your_client_secret_here
   provider.oauth.redirect-uri=http://localhost:8080/api/v1/provider/callback/kraken
   ```

2. **Production Environment**
   - Use a secure secrets management system (e.g., AWS Secrets Manager, HashiCorp Vault)
   - Never commit these values to the repository
   - Configure your deployment environment with the appropriate values:
   ```properties
   provider.kraken.client-id=${KRAKEN_CLIENT_ID}
   provider.kraken.client-secret=${KRAKEN_CLIENT_SECRET}
   provider.oauth.redirect-uri=${OAUTH_REDIRECT_URI}
   ```

## OAuth Flow Implementation

The OAuth flow is implemented in the `KrakenApiService` class and follows this sequence:

1. **Authorization Request**
   - The user initiates the connection process from Strategiz
   - The application generates an authorization URL using the client ID and redirect URI
   - The user is redirected to Kraken's authentication page

2. **User Authentication**
   - The user logs in to their Kraken account and authorizes the requested permissions
   - Kraken redirects back to the specified redirect URI with an authorization code

3. **Token Exchange**
   - Strategiz backend exchanges the authorization code for access and refresh tokens
   - The tokens are securely stored in the user repository

4. **API Access**
   - The access token is used for subsequent API requests to Kraken
   - When the access token expires, the refresh token is used to obtain a new one

## Testing

1. **Local Testing**
   - Configure your local environment with test credentials
   - Use a tool like ngrok to create a public URL for your local server if testing the full OAuth flow

2. **Integration Tests**
   - Run integration tests to verify the OAuth flow works correctly
   - Mock Kraken API responses for unit tests

## Error Handling

The integration uses the centralized exception framework for consistent error handling:
- API errors from Kraken are wrapped in appropriate exception types
- All exceptions include context information (provider ID, user ID, etc.)
- Error responses follow the standard API response format

## Security Considerations

- Access tokens and refresh tokens are securely stored
- No sensitive information is logged or exposed in API responses
- Token refresh is handled automatically when tokens expire
- Users can disconnect their Kraken account at any time

## Troubleshooting

Common issues:
- Invalid redirect URI: Ensure the redirect URI configured in your application exactly matches the one registered with Kraken
- Invalid client credentials: Check that the client ID and secret are correct
- Missing scopes: Verify that the requested scopes match the ones registered with Kraken
- Rate limiting: Kraken may impose rate limits on API requests

## Support

For issues with the Kraken integration:
- Internal: Contact the Strategiz development team
- External: Refer to the [Kraken API documentation](https://docs.kraken.com/rest/) or contact Kraken support
