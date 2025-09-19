# 🔐 Strategiz Authentication Service

Complete authentication system providing secure user sign-up, sign-in, and multi-factor authentication for the Strategiz platform.

## 📚 **Documentation Index**

### **📋 Core Documentation**
- **🏠 This README** - Complete service overview and API reference
- **📧 [Sign Up Flow Guide](src/main/resources/static/docs/signup-flow-guide.md)** - Detailed multi-step signup implementation
- **📱 [TOTP Authentication](docs/TOTP.md)** - Time-based one-time password setup and usage
- **🔐 [Session Management](./SESSION-MANAGEMENT.md)** - Complete session lifecycle, architecture, and cleanup policies
- **🔗 [API Documentation](http://localhost:8080/swagger-ui/index.html)** - Interactive Swagger UI

## 📑 Table of Contents

- [🏗️ **Architecture Overview**](#️-architecture-overview)
- [🚀 **Quick Start**](#-quick-start)
- [🔑 **Authentication Methods**](#-authentication-methods)
- [⚙️ **Configuration**](#️-configuration)
- [🔧 **API Reference**](#-api-reference)
- [🛡️ **Security Features**](#️-security-features)
- [🧪 **Testing**](#-testing)
- [📋 **Troubleshooting**](#-troubleshooting)

---

## 🏗️ Architecture Overview

The service-auth module provides a **modular authentication system** with support for multiple authentication methods:

```
service-auth/
├── 📧 Email OTP          # Email-based one-time passwords
├── 🛡️ Passkey           # WebAuthn/FIDO2 passwordless authentication  
├── 📱 TOTP               # Time-based one-time passwords (Google Authenticator)
├── 🌐 OAuth              # Social login (Google, Facebook)
├── 🔐 Session Management # JWT token-based sessions
└── 📝 User Signup        # Multi-step registration process
```

### **Key Principles:**
- **🔒 Security First** - All methods follow modern security best practices
- **🔌 Pluggable Design** - Easy to add/remove authentication methods
- **📱 Multi-Factor Ready** - Built-in support for MFA combinations
- **🌐 Cross-Platform** - Works with web, mobile, and desktop clients
- **⚡ Performance Optimized** - Efficient token management and validation

---

## 🚀 Quick Start

### **Prerequisites**
- Java 21+
- Spring Boot 3.x
- Firebase/Firestore database
- HashiCorp Vault (for secrets)

### **Environment Setup**
```bash
# 1. Start the authentication service
cd scripts/deployment/local
./build-and-deploy.sh

# 2. Access API documentation
open http://localhost:8080/swagger-ui/index.html

# 3. Test health endpoint
curl http://localhost:8080/actuator/health
```

### **Configuration**
Set these environment variables or configure in Vault:
```bash
# OAuth Credentials
export AUTH_GOOGLE_CLIENT_ID="your-google-client-id"
export AUTH_GOOGLE_CLIENT_SECRET="your-google-client-secret"
export AUTH_FACEBOOK_CLIENT_ID="your-facebook-client-id"
export AUTH_FACEBOOK_CLIENT_SECRET="your-facebook-client-secret"

# Frontend URL for redirects
export FRONTEND_URL="http://localhost:3000"
```

---

---

## 🔑 Authentication Methods

> **📖 For detailed signup flow implementation, see [Sign Up Flow Guide](src/main/resources/static/docs/signup-flow-guide.md)**

Strategiz supports multiple secure authentication methods:

### 🛡️ Passkey Authentication

**WebAuthn/FIDO2 passwordless authentication** - the most secure and user-friendly option.

#### **Features:**
- ✅ **Passwordless** - No passwords to remember or steal
- ✅ **Phishing Resistant** - Cryptographically bound to your domain
- ✅ **Multi-Device** - Works across phones, laptops, security keys
- ✅ **Biometric** - Face ID, Touch ID, Windows Hello
- ✅ **Hardware Keys** - YubiKey, Titan, etc.

#### **Registration Flow:**
```javascript
// 1. Begin passkey registration
POST /auth/passkey/registration/begin
{
  "email": "user@example.com",
  "displayName": "John Doe",
  "verificationCode": "123456"  // From email verification
}

// Response contains WebAuthn challenge
{
  "publicKeyCredentialCreationOptions": {
    "challenge": "base64-encoded-challenge",
    "rp": { "name": "Strategiz", "id": "strategiz.io" },
    "user": { "id": "user-id", "name": "user@example.com", "displayName": "John Doe" },
    // ... other WebAuthn options
  }
}

// 2. Create credential using WebAuthn API
const credential = await navigator.credentials.create({
  publicKey: response.publicKeyCredentialCreationOptions
});

// 3. Complete registration
POST /auth/passkey/registration/complete
{
  "email": "user@example.com",
  "credentialId": credential.id,
  "attestationResponse": {
    "clientDataJSON": "...",
    "attestationObject": "..."
  }
}
```

#### **Authentication Flow:**
```javascript
// 1. Begin authentication
POST /auth/passkey/authentication/begin
{
  "email": "user@example.com"
}

// Response contains challenge
{
  "publicKeyCredentialRequestOptions": {
    "challenge": "base64-encoded-challenge",
    "allowCredentials": [/* user's registered credentials */]
  }
}

// 2. Get assertion using WebAuthn API
const assertion = await navigator.credentials.get({
  publicKey: response.publicKeyCredentialRequestOptions
});

// 3. Complete authentication
POST /auth/passkey/authentication/complete
{
  "email": "user@example.com", 
  "credentialId": assertion.id,
  "assertionResponse": {
    "clientDataJSON": "...",
    "authenticatorData": "...",
    "signature": "..."
  }
}

// Response contains JWT token
{
  "success": true,
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": { /* user details */ }
}
```

### 📱 TOTP Authentication

**Time-based One-Time Password** using authenticator apps like Google Authenticator, Authy, or 1Password.

#### **Setup Flow:**
```javascript
// 1. Initialize TOTP setup
POST /auth/totp/setup/initialize
{
  "email": "user@example.com"
}

// Response contains QR code and secret
{
  "success": true,
  "qrCodeUri": "otpauth://totp/Strategiz:user@example.com?secret=...",
  "secret": "JBSWY3DPEHPK3PXP", // Base32 encoded
  "qrCodeDataUri": "data:image/png;base64,..." // QR code image
}

// 2. User scans QR code with authenticator app

// 3. Complete setup with first TOTP code
POST /auth/totp/setup/complete
{
  "email": "user@example.com",
  "totpCode": "123456"
}
```

#### **Authentication Flow:**
```javascript
POST /auth/totp/authenticate
{
  "email": "user@example.com",
  "totpCode": "654321"
}

// Response contains JWT token
{
  "success": true,
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": { /* user details */ }
}
```

### 📧 Email OTP Authentication

**Email-based one-time passwords** for users who prefer email-based authentication.

#### **Authentication Flow:**
```javascript
// 1. Request OTP via email
POST /auth/email-otp/send
{
  "email": "user@example.com",
  "purpose": "signin"
}

// 2. User receives email with 6-digit code

// 3. Authenticate with OTP
POST /auth/email-otp/verify
{
  "email": "user@example.com",
  "code": "123456",
  "purpose": "signin"
}

// Response contains JWT token
{
  "success": true,
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": { /* user details */ }
}
```

### 🌐 OAuth Authentication

**Social login** with Google and Facebook for quick sign-up and sign-in.

#### **Supported Providers:**
- **🔵 Google** - Google accounts
- **🔵 Facebook** - Facebook accounts

#### **OAuth Flow:**
```javascript
// 1. Redirect user to OAuth provider
GET /auth/oauth/{provider}?redirect_uri=http://localhost:3000/auth/callback

// User authorizes on provider's site

// 2. Handle callback with authorization code
GET /auth/oauth/{provider}/callback?code=auth_code&state=csrf_token

// 3. Backend exchanges code for user info and creates/updates user

// 4. Redirect to frontend with JWT token
// http://localhost:3000/auth/callback?token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

#### **Supported Scopes:**
- **Google**: `email profile openid`
- **Facebook**: `email public_profile`

---

## ⚙️ Configuration

### **Application Properties**
See `src/main/resources/application.properties` for all configuration options:

```properties
# OAuth Provider Configuration
oauth.providers.google.client-id=${AUTH_GOOGLE_CLIENT_ID:}
oauth.providers.google.client-secret=${AUTH_GOOGLE_CLIENT_SECRET:}
oauth.providers.google.redirect-uri=${AUTH_GOOGLE_REDIRECT_URI:http://localhost:8080/auth/oauth/google/callback}

oauth.providers.facebook.client-id=${AUTH_FACEBOOK_CLIENT_ID:}
oauth.providers.facebook.client-secret=${AUTH_FACEBOOK_CLIENT_SECRET:}

# Frontend URL for redirects
oauth.frontend-url=${FRONTEND_URL:http://localhost:3000}

# Email Configuration
strategiz.email.verification.code-length=6
strategiz.email.verification.expiry-minutes=10

# Security Configuration  
strategiz.auth.session.jwt-expiry-hours=24
strategiz.auth.passkey.timeout-seconds=300
strategiz.auth.totp.window-size=1
```

### **Environment Variables**
For production deployment, set these environment variables:

```bash
# OAuth Credentials (get from provider consoles)
AUTH_GOOGLE_CLIENT_ID=your-google-client-id
AUTH_GOOGLE_CLIENT_SECRET=your-google-client-secret
AUTH_FACEBOOK_CLIENT_ID=your-facebook-client-id  
AUTH_FACEBOOK_CLIENT_SECRET=your-facebook-client-secret

# URLs
FRONTEND_URL=https://app.strategiz.io
AUTH_GOOGLE_REDIRECT_URI=https://api.strategiz.io/auth/oauth/google/callback
AUTH_FACEBOOK_REDIRECT_URI=https://api.strategiz.io/auth/oauth/facebook/callback

# Database
FIREBASE_PROJECT_ID=strategiz-io
GOOGLE_APPLICATION_CREDENTIALS=/app/firebase-credentials.json
```

---

## 🔧 API Reference

### **📋 Complete Endpoint List**

#### **Email Verification**
- `POST /auth/verification/email/send-code` - Send verification code
- `POST /auth/verification/email/verify-code` - Verify code
- `GET /auth/verification/email/status` - Check verification status

#### **Passkey Authentication**
- `POST /auth/passkey/registration/begin` - Start passkey registration
- `POST /auth/passkey/registration/complete` - Complete passkey registration
- `POST /auth/passkey/authentication/begin` - Start passkey authentication
- `POST /auth/passkey/authentication/complete` - Complete passkey authentication
- `GET /auth/passkey/credentials` - List user's passkeys
- `DELETE /auth/passkey/credentials/{id}` - Delete a passkey

#### **TOTP Authentication**
- `POST /auth/totp/setup/initialize` - Initialize TOTP setup
- `POST /auth/totp/setup/complete` - Complete TOTP setup
- `POST /auth/totp/authenticate` - Authenticate with TOTP
- `DELETE /auth/totp/disable` - Disable TOTP

#### **Email OTP Authentication**
- `POST /auth/email-otp/send` - Send email OTP
- `POST /auth/email-otp/verify` - Verify email OTP

#### **OAuth Authentication**
- `GET /auth/oauth/{provider}` - Initiate OAuth flow
- `GET /auth/oauth/{provider}/callback` - Handle OAuth callback

#### **Session Management**
- `POST /auth/signout` - Sign out user
- `POST /auth/refresh` - Refresh JWT token
- `GET /auth/me` - Get current user info

### **📊 Interactive API Documentation**
Access the complete Swagger UI documentation at:
**🔗 [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)**

---

## 🛡️ Security Features

### **🔐 Built-in Security**
- ✅ **JWT Tokens** - Stateless authentication with configurable expiry
- ✅ **CSRF Protection** - Cross-site request forgery prevention
- ✅ **Rate Limiting** - Prevent brute force attacks
- ✅ **Input Validation** - Comprehensive request validation
- ✅ **Secure Headers** - HSTS, CSP, and other security headers
- ✅ **Encrypted Storage** - Sensitive data encrypted at rest

### **🛡️ Authentication Security**
- ✅ **Passkey (WebAuthn)** - Phishing-resistant, hardware-backed
- ✅ **TOTP** - Time-based, replay-resistant codes
- ✅ **Email OTP** - Secure delivery, time-limited codes
- ✅ **OAuth** - Industry-standard social login

### **📧 Email Security**
- ✅ **Purpose-based codes** - Different codes for signup/signin/reset
- ✅ **Time-limited** - Codes expire after configurable time
- ✅ **Single-use** - Codes invalidated after use
- ✅ **Rate limiting** - Prevent spam and abuse

### **🔑 Session Security**
- ✅ **JWT with RS256** - Asymmetric signing for enhanced security
- ✅ **Token refresh** - Automatic token renewal
- ✅ **Secure logout** - Proper session invalidation
- ✅ **Device tracking** - Optional device fingerprinting

---

## 🧪 Testing

### **🔧 Manual Testing**
```bash
# 1. Start the service
cd scripts/deployment/local
./build-and-deploy.sh

# 2. Test health endpoint
curl http://localhost:8080/actuator/health

# 3. Test email verification
curl -X POST http://localhost:8080/auth/verification/email/send-code \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","purpose":"signup"}'

# 4. Open Swagger UI for interactive testing
open http://localhost:8080/swagger-ui/index.html
```

### **🧪 Automated Testing**
```bash
# Run all tests
mvn test

# Run specific test classes
mvn test -Dtest=PasskeyRegistrationServiceTest
mvn test -Dtest=TotpAuthenticationServiceTest
mvn test -Dtest=EmailOtpServiceTest
```

### **🌐 Frontend Integration Testing**
For comprehensive frontend integration examples and testing:
- **📧 [Sign Up Flow Guide](src/main/resources/static/docs/signup-flow-guide.md)** - Complete implementation examples
- **📱 [TOTP Guide](docs/TOTP.md)** - Authenticator app integration examples

---

## 📋 Troubleshooting

### **🔧 Common Issues**

#### **Passkey Issues**
```bash
# InvalidCharacterError during passkey setup
# → Fixed: Base64URL validation in passkeyClient.ts

# CORS errors during passkey authentication  
# → Fixed: allowedOriginPatterns() with credentials=true
```

#### **OAuth Issues**
```bash
# OAuth redirect mismatch
# Check: AUTH_GOOGLE_REDIRECT_URI matches Google Console settings
# Check: AUTH_FACEBOOK_REDIRECT_URI matches Facebook App settings

# Missing OAuth credentials
# Verify: Environment variables are set correctly
# Verify: Vault secrets are accessible
```

#### **Email Issues**
```bash
# Email codes not being sent
# Check: Email service configuration
# Check: Rate limiting not exceeded
# Check: Email provider credentials

# Codes not working
# Check: Time synchronization between client/server
# Check: Code hasn't expired (default: 10 minutes)
```

#### **TOTP Issues**
```bash
# TOTP codes not working
# Check: Device time is synchronized
# Check: Time window configuration (default: ±30 seconds)
# Check: Base32 secret encoding

# QR code not generating
# Check: QR code library dependency
# Check: Image generation service
```

### **🔍 Debug Commands**
```bash
# Check service logs
docker logs strategiz-auth-service

# Check database connections
curl http://localhost:8080/actuator/health/db

# Test specific authentication method
curl -X POST http://localhost:8080/auth/test/{method}
```

### **📞 Support**
- **📖 Documentation**: This README
- **🔗 API Docs**: [Swagger UI](http://localhost:8080/swagger-ui/index.html)
- **🐛 Issues**: Create GitHub issues for bugs
- **💬 Discussions**: Use GitHub Discussions for questions

---

## 📈 Monitoring & Observability

The service integrates with the **service-monitoring module** for comprehensive observability:

- **📊 Metrics**: Authentication success/failure rates, method usage
- **📝 Logging**: Structured logging for all authentication events  
- **🚨 Health Checks**: Custom health indicators for each auth method
- **📋 Dashboards**: Pre-built Grafana dashboards for monitoring

Access monitoring at:
- **Grafana**: `http://localhost:3001` (admin/strategiz123)
- **Prometheus**: `http://localhost:9090`
- **Health**: `http://localhost:8080/actuator/health`

---

## 🚀 What's Next?

### **🔄 Planned Features**
- **🔐 WebAuthn Level 2** - Advanced attestation and verification
- **📧 Magic Links** - Passwordless email-based authentication  
- **🔑 Hardware Keys** - Enhanced YubiKey and FIDO2 support
- **📱 SMS OTP** - SMS-based one-time passwords
- **🛡️ Risk-based Auth** - Adaptive authentication based on risk signals
- **🌐 More OAuth** - Apple, GitHub, Microsoft providers

### **🏗️ Architecture Evolution**
- **🔌 Plugin System** - Pluggable authentication methods
- **🌍 Multi-tenant** - Support for multiple organizations
- **📊 Analytics** - Advanced authentication analytics and insights
- **🔄 SSO Integration** - SAML and OpenID Connect support

---

**🎉 Happy Authenticating with Strategiz!** 🔐

For questions or contributions, please refer to the main project documentation or create an issue in the GitHub repository.
