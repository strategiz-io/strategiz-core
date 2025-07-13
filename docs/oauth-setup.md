# OAuth Setup Guide

This guide explains how to set up OAuth providers (Google and Facebook) for the Strategiz platform.

## Google OAuth Setup

### 1. Create a Google Cloud Project

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select an existing one
3. Enable the Google+ API or Google Identity Platform API

### 2. Configure OAuth Consent Screen

1. Navigate to "APIs & Services" > "OAuth consent screen"
2. Choose "External" for user type
3. Fill in the required information:
   - App name: "Strategiz"
   - User support email
   - Developer contact information
4. Add scopes:
   - `email`
   - `profile`
   - `openid`
5. Add test users if in development mode

### 3. Create OAuth 2.0 Credentials

1. Go to "APIs & Services" > "Credentials"
2. Click "Create Credentials" > "OAuth client ID"
3. Choose "Web application"
4. Configure the following:
   - **Name**: "Strategiz Web Client"
   - **Authorized JavaScript origins**:
     - `http://localhost:3000` (development)
     - `https://your-domain.com` (production)
   - **Authorized redirect URIs**:
     - `http://localhost:8080/auth/oauth/google/callback` (development)
     - `https://api.your-domain.com/auth/oauth/google/callback` (production)
5. Save and copy the Client ID and Client Secret

### 4. Configure Backend Environment Variables

Set the following environment variables for the backend:

```bash
# Google OAuth Configuration
export AUTH_GOOGLE_CLIENT_ID="your-google-client-id"
export AUTH_GOOGLE_CLIENT_SECRET="your-google-client-secret"
export AUTH_GOOGLE_REDIRECT_URI="http://localhost:8080/auth/oauth/google/callback"
```

For production, use HashiCorp Vault:
```bash
vault kv put secret/strategiz/oauth/google \
  client-id="your-google-client-id" \
  client-secret="your-google-client-secret"
```

### 5. Configure Frontend Environment Variables

Create a `.env` file in the frontend directory:

```bash
# API Configuration
REACT_APP_API_URL=http://localhost:8080/api
REACT_APP_FRONTEND_URL=http://localhost:3000
```

For production:
```bash
REACT_APP_API_URL=https://api.your-domain.com/api
REACT_APP_FRONTEND_URL=https://your-domain.com
```

## Facebook OAuth Setup

### 1. Create a Facebook App

1. Go to [Facebook Developers](https://developers.facebook.com/)
2. Click "My Apps" > "Create App"
3. Choose "Consumer" as the app type
4. Fill in app details

### 2. Configure Facebook Login

1. Add "Facebook Login" product to your app
2. Configure settings:
   - **Valid OAuth Redirect URIs**:
     - `http://localhost:8080/auth/oauth/facebook/callback` (development)
     - `https://api.your-domain.com/auth/oauth/facebook/callback` (production)
   - **Allowed Domains for the JavaScript SDK**: Add your domains
3. Enable required permissions:
   - `email`
   - `public_profile`

### 3. Configure Backend Environment Variables

```bash
# Facebook OAuth Configuration
export AUTH_FACEBOOK_CLIENT_ID="your-facebook-app-id"
export AUTH_FACEBOOK_CLIENT_SECRET="your-facebook-app-secret"
export AUTH_FACEBOOK_REDIRECT_URI="http://localhost:8080/auth/oauth/facebook/callback"
```

## OAuth Flow Architecture

### Sign-In Flow
1. User clicks "Sign in with Google/Facebook"
2. Frontend redirects to backend endpoint: `/auth/oauth/{provider}/auth?isSignup=false`
3. Backend redirects to provider's OAuth page with proper parameters
4. User authenticates with provider
5. Provider redirects back to backend callback: `/auth/oauth/{provider}/callback`
6. Backend processes callback, creates/updates user, generates JWT tokens
7. Backend redirects to frontend with tokens
8. Frontend stores tokens and redirects to dashboard

### Sign-Up Flow
1. User clicks "Sign up with Google/Facebook"
2. Frontend calls backend API: `/auth/oauth/{provider}/authorization-url?isSignup=true`
3. Backend returns OAuth URL with state parameter
4. Frontend redirects to OAuth URL
5. Same callback flow as sign-in
6. Backend creates new user account

## Troubleshooting

### Common Issues

1. **"redirect_uri_mismatch" error**
   - Ensure the redirect URI in your OAuth app settings exactly matches the one in your backend configuration
   - Check for trailing slashes and protocol (http vs https)

2. **"invalid_client" error**
   - Verify client ID and secret are correctly set
   - Ensure environment variables are loaded properly

3. **CORS errors**
   - Add your frontend domain to backend CORS configuration
   - Ensure credentials are included in fetch requests

4. **User data not persisting**
   - Check that tokens are being stored in localStorage
   - Verify Redux state is being updated properly

### Testing OAuth Locally

1. Start the backend:
```bash
cd strategiz-core
export AUTH_GOOGLE_CLIENT_ID="your-client-id"
export AUTH_GOOGLE_CLIENT_SECRET="your-client-secret"
mvn spring-boot:run -pl application
```

2. Start the frontend:
```bash
cd strategiz-ui
npm start
```

3. Navigate to http://localhost:3000/auth/signin
4. Click "Continue with Google" or "Continue with Facebook"

## Security Best Practices

1. **Never commit OAuth credentials** to version control
2. **Use environment variables** for local development
3. **Use HashiCorp Vault** for production secrets
4. **Implement PKCE** for additional security (planned)
5. **Validate state parameter** to prevent CSRF attacks
6. **Use HTTPS** in production for all OAuth redirects
7. **Implement rate limiting** on OAuth endpoints
8. **Log OAuth attempts** for security auditing

## Implementation Details

### Frontend OAuth Configuration
- Configuration file: `src/features/auth/config/oauthConfig.ts`
- Provides clean URL abstraction for OAuth endpoints
- Supports multiple providers with enable/disable flags
- Environment-aware URL generation

### Backend OAuth Implementation
- Controllers: `GoogleOAuthController`, `FacebookOAuthController`
- Services: `GoogleOAuthService`, `FacebookOAuthService`
- Client modules: `client-google`, `client-facebook`
- Error handling: Uses StrategizException with proper error codes

### Supported OAuth Providers
- ✅ Google
- ✅ Facebook
- 🔄 GitHub (planned)
- 🔄 LinkedIn (planned)
- 🔄 Twitter/X (planned)
- 🔄 Microsoft (planned)