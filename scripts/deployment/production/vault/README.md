# Production Vault Setup for Strategiz

## Current Status

The backend has been configured to work without HashiCorp Vault in production by:

1. **Disabled Vault**: Updated `application-prod.properties` to set `vault.enabled=false`
2. **Environment Variables**: Configured to use environment variables instead of Vault for secrets
3. **Folder Structure**: Organized vault-related scripts in `scripts/deployment/production/vault/`

## Required Environment Variables

The backend now expects these environment variables to be set in Cloud Run:

### Firebase Configuration
- `FIREBASE_PROJECT_ID`: Firebase project ID (default: strategiz-io)
- `FIREBASE_DATABASE_URL`: Firebase Realtime Database URL
- `FIREBASE_SERVICE_ACCOUNT_KEY`: Firebase service account JSON credentials

### OAuth Configuration
- `GOOGLE_OAUTH_CLIENT_ID`: Google OAuth 2.0 client ID
- `GOOGLE_OAUTH_CLIENT_SECRET`: Google OAuth 2.0 client secret
- `FACEBOOK_OAUTH_CLIENT_ID`: Facebook App ID
- `FACEBOOK_OAUTH_CLIENT_SECRET`: Facebook App Secret

## Deployment Options

### Option 1: Google Secret Manager (Recommended)
Use Google Secret Manager to store secrets and inject them as environment variables:

1. Run `setup-secrets-manager.sh` to create secrets
2. Run `update-backend-with-secrets.sh` to deploy with Secret Manager integration

### Option 2: Environment Variables
Manually set environment variables in Cloud Run service configuration.

### Option 3: HashiCorp Vault
Deploy a Vault instance using the scripts in this folder.

## Files in this folder

- `setup-secrets-manager.sh`: Creates secrets in Google Secret Manager
- `update-backend-with-secrets.sh`: Deploys backend with Secret Manager integration
- `deploy-without-secrets.sh`: Basic deployment for testing (no OAuth will work)
- `deploy-vault.sh`: Deploy HashiCorp Vault on Cloud Run
- `vault-config.hcl`: Vault configuration file

## Current Issue

The passkey authentication is failing with HTTP 500 because the backend is trying to access Vault secrets that don't exist in the production environment.

## Next Steps

1. Set up authentication credentials in Google Cloud Console and Facebook Developer Console
2. Choose a secrets management approach (Secret Manager recommended)
3. Deploy the backend with proper secret configuration
4. Test authentication flows

## Testing

After proper deployment, test these endpoints:

- Health: `https://strategiz-core-43628135674.us-central1.run.app/actuator/health`
- Passkey: `https://strategiz-core-43628135674.us-central1.run.app/auth/passkeys/register/start`
- OAuth: `https://strategiz-core-43628135674.us-central1.run.app/auth/oauth/google/signin`