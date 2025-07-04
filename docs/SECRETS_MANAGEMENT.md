# Strategiz Secrets Management

## Overview

This document describes how to manage secrets and credentials in the Strategiz application. We use environment variables loaded from a `.env` file to securely manage API keys, client IDs, client secrets, and other sensitive information.

## Environment Variables

### Configuration Approach

Credentials are loaded using the following priority:

1. Environment variables from `.env` file (highest priority)
2. System environment variables (if available)
3. Values in `application.properties` files (lowest priority)

### Setup Instructions

1. **Create a `.env` file** in the root directory of the project:

   ```
   # OAuth Credentials
   AUTH_GOOGLE_CLIENT_ID=your_google_client_id_here
   AUTH_GOOGLE_CLIENT_SECRET=your_google_client_secret_here
   AUTH_FACEBOOK_CLIENT_ID=your_facebook_client_id_here
   AUTH_FACEBOOK_CLIENT_SECRET=your_facebook_client_secret_here
   
   # Other API Keys
   API_ETHERSCAN_KEY=your_etherscan_api_key_here
   ```

2. **Add the file to `.gitignore`** (this should already be done)

   ```
   # Ignore environment variables file with secrets
   .env
   ```

3. **Verify Environment Variables**

   When the application starts, it will log whether credentials were successfully loaded:
   
   ```
   2025-06-28 19:25:12.544 INFO  [...] EnvFileLoader - Found AUTH_FACEBOOK_CLIENT_ID in .env file
   2025-06-28 19:25:12.544 INFO  [...] EnvFileLoader - Found AUTH_FACEBOOK_CLIENT_SECRET in .env file
   2025-06-28 19:25:12.545 INFO  [...] OAuthCredentialsConfig - Facebook Client ID loaded successfully
   ```

## Production Deployment

For production deployments:

1. **On Docker/Kubernetes**: Use Kubernetes secrets or Docker environment variables
2. **On Cloud Providers**:
   - Use environment variables in your deployment configuration
   - For Firebase: Use Firebase Functions environment configuration
   ```
   firebase functions:config:set auth.facebook.client_id="1971747536646640" auth.facebook.client_secret="your-secret"
   ```

## Default Fallbacks

If environment variables are not found and `.env` file is not present, the application will fall back to values defined in:

- `/service/service-auth/src/main/resources/application.properties` for auth-related credentials

## Future Improvements

In the future, we plan to implement a more robust secrets management solution using one of the following:

- HashiCorp Vault
- AWS Secrets Manager
- Google Secret Manager
- Azure Key Vault
