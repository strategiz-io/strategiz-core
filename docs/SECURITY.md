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
