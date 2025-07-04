# Security Features

This document outlines the security measures implemented in the Strategiz Core backend to protect user data and API credentials.

## Authentication

### Firebase Authentication

Strategiz uses Firebase Authentication for user identity management:

- **Token-based Authentication**: JWT tokens are used for authenticating API requests
- **User Management**: Firebase handles user registration, login, and session management
- **Role-based Access Control**: Different user roles (user, admin) have different permissions
- **Secure Session Handling**: Sessions are managed securely with appropriate timeout policies

### API Key Management

Exchange API credentials are handled with the utmost security:

- **Secure Credential Storage**: API keys are stored in Firebase Firestore with encryption
- **User-specific Credentials**: Each user's API credentials are stored separately
- **No Client-side Access**: API credentials are never exposed to the client
- **Server-side API Requests**: All API calls to exchanges are made from the server, not the client

## API Security

### Request Protection

- **Rate Limiting**: Prevents abuse of the API endpoints
  - Global rate limiting for all API requests
  - Exchange-specific rate limiting for regular endpoints
  - More restrictive rate limiting for admin raw data endpoints

- **Input Validation**: All user input is validated before processing

### Response Protection

- **CORS Protection**: Restricts which domains can access the API
- **Helmet Security Headers**: Adds various HTTP headers for enhanced security:
  - Content-Security-Policy
  - X-XSS-Protection
  - X-Content-Type-Options
  - X-Frame-Options
  - Referrer-Policy

## Data Security

### Storage Security

- **Encryption at Rest**: Sensitive data is encrypted before storage
- **Minimal Data Collection**: Only necessary data is collected and stored
- **Data Isolation**: User data is isolated in separate database collections

### Transmission Security

- **HTTPS Only**: All API communication is encrypted using HTTPS
- **Secure Headers**: Security headers are set for all responses
- **No Sensitive Data in URLs**: Sensitive data is never passed in URL parameters

## Exchange-specific Security

- **HMAC Signatures**: Exchange API requests use HMAC signatures for authentication
- **Timestamp Validation**: Requests include timestamps to prevent replay attacks
- **IP Restriction**: API access can be restricted to specific IP addresses where supported
- **Minimum-required Permissions**: API keys are created with the minimum required permissions

## Auditing and Monitoring

- **Request Logging**: All API requests are logged for auditing purposes
- **Error Monitoring**: Errors are logged and monitored for potential security issues
- **Failed Authentication Tracking**: Failed authentication attempts are tracked to detect brute force attacks

## Security Best Practices for Developers

1. **Never hardcode credentials** in source code
2. **Always validate user input** before processing
3. **Use parameterized queries** for database operations
4. **Keep dependencies updated** to patch security vulnerabilities
5. **Follow the principle of least privilege** when designing API endpoints
6. **Implement proper error handling** that doesn't expose sensitive information

## Reporting Security Issues

If you discover a security vulnerability in Strategiz Core, please report it by sending an email to security@strategiz.io. 

Please do not disclose security vulnerabilities publicly until they have been addressed by the team.

# Strategiz Security Architecture

## Overview

Strategiz implements a **progressive authentication system** with **risk-based access control** using **PASETO v4.public tokens** and **OIDC-compliant claims**.

## 🏗️ Architecture Components

### **Core Modules**
- **`business-token-auth`** - Token creation, claims population, session management
- **`service-auth`** - Authentication endpoints, signup flow, passkey registration  
- **`service-base`** - Shared CRUD operation constants
- **Individual Services** - Resource-specific access control

### **Authentication Flow**
```
1. Profile Creation    → ACR "1" (Partial Authentication)
2. Auth Method Setup   → ACR "2.x" (Full Authentication + Assurance Level)
3. Provider Integration → OAuth consent flow or demo mode
```

## 🎫 Token Technology

### **PASETO v4.public**
- **Public key cryptography** with Ed25519 signatures
- **Stateless** - all information in token
- **Tamper-proof** - cryptographically signed
- **No encryption** - claims are readable but verified

### **HTTP-Only Cookies**
```http
Set-Cookie: strategiz_session=v4.public.eyJ...; 
           HttpOnly; Secure; SameSite=Strict; 
           Path=/; Max-Age=86400; Domain=strategiz.io
```

## 🔐 Security Model

### **Progressive Authentication**
- **Step 1**: Profile creation (limited access)
- **Step 2**: Authentication method setup (full access based on strength)
- **Step 3**: Provider integration (extended capabilities)

### **Risk-Based Access Control**
- **ACR Levels**: Authentication completion + assurance strength
- **Dynamic Scopes**: Permissions calculated based on authentication strength
- **Operation Limits**: Financial limits based on assurance level

### **Assurance Levels**
- **Basic (2.1)**: Single-factor authentication
- **Substantial (2.2)**: Multi-factor authentication  
- **High (2.3)**: Hardware cryptographic authentication

## 🛡️ Security Features

### **Token Security**
- ✅ **Obfuscated Claims** - Numeric mappings for auth methods and assurance
- ✅ **Short Expiry** - 24-hour access tokens
- ✅ **Unique IDs** - JTI for token revocation
- ✅ **Audience Validation** - Tokens bound to specific services

### **Authentication Security**
- ✅ **Passkeys (FIDO2/WebAuthn)** - Phishing-resistant authentication
- ✅ **TOTP** - Time-based one-time passwords
- ✅ **SMS OTP** - Secondary verification channel
- ✅ **Device Fingerprinting** - Additional security layer

### **Access Control**
- ✅ **Least Privilege** - Minimal scopes granted
- ✅ **Operation-Level Control** - Granular permissions
- ✅ **Resource Isolation** - Each service owns its resources
- ✅ **Step-up Authentication** - Higher assurance for sensitive operations

## 📊 Compliance

### **Standards Alignment**
- **OIDC** - OpenID Connect claims and flows
- **OAuth2** - Scope-based authorization
- **NIST SP 800-63** - Digital identity guidelines (AAL1/AAL2/AAL3)
- **FIDO2** - Modern authentication standards

### **Security Considerations**
- **XSS Protection** - HTTP-only cookies
- **CSRF Protection** - SameSite cookies
- **Token Leakage** - No sensitive data in tokens
- **Audit Trail** - Authentication method tracking

## 🔗 Related Documentation

- [`business-token-auth/README.md`](../business/business-token-auth/README.md) - Complete token specification
- [`service-auth/README.md`](../service/service-auth/README.md) - Authentication endpoints
- [API_ENDPOINTS.md](./API_ENDPOINTS.md) - Service endpoint documentation

## 🚀 Quick Start

For developers implementing authentication:

1. **Token Validation**: Check `acr` claim for required assurance level
2. **Scope Checking**: Use `@PreAuthorize("hasScope('operation:resource')")`
3. **Claims Access**: Extract user info from `sub`, track methods via `amr`

See detailed implementation examples in [`business-token-auth` documentation](../business/business-token-auth/README.md).
