# Strategiz Data User Module

This module provides data access for user profiles, authentication methods, and related data in the Strategiz platform.

## Database Structure

The module interacts with the following Firestore collections and subcollections:

### `users` Collection

The primary collection for storing user information with a modern, scalable structure.

```
users/
  └── {userId}/
      ├── profile: {
      │   ├── name: string,
      │   ├── email: string,
      │   ├── photoURL: string,
      │   ├── verifiedEmail: boolean,
      │   ├── subscriptionTier: string ("premium", "basic", "free", etc.),
      │   ├── tradingMode: string ("demo" or "real"),
      │   └── isActive: boolean
      │ }
      ├── connectedProviders: [
      │   {
      │     ├── provider: string ("kraken", "binanceus", "charlesschwab"),
      │     ├── accountType: string ("paper" or "real"),
      │     └── connectedAt: timestamp
      │   }
      │ ]
      │
      ├── createdBy: string,
      ├── createdAt: timestamp,
      ├── modifiedBy: string,
      ├── modifiedAt: timestamp,
      ├── version: number,
      ├── isActive: boolean,
      │
      ├── authentication_methods/
      │   └── {authMethodId}/
      │       ├── type: string ("TOTP", "SMS_OTP", "PASSKEY", "OAUTH_GOOGLE", etc.),
      │       ├── name: string,
      │       ├── [type-specific fields],
      │       ├── createdBy: string,
      │       ├── createdAt: timestamp,
      │       ├── modifiedBy: string,
      │       ├── modifiedAt: timestamp,
      │       ├── version: number,
      │       ├── isActive: boolean,
      │       └── lastVerifiedAt: timestamp
      │
      ├── api_credentials/
      │   └── {credentialId}/
      │       ├── provider: string ("coinbase", "kraken", "binanceus", etc.),
      │       ├── apiKey: string (encrypted),
      │       ├── privateKey: string (encrypted),
      │       ├── [provider-specific fields],
      │       ├── createdBy: string,
      │       ├── createdAt: timestamp,
      │       ├── modifiedBy: string,
      │       ├── modifiedAt: timestamp,
      │       ├── version: number,
      │       └── isActive: boolean
      │
      ├── devices/
      │   └── {deviceId}/
      │       ├── deviceName: string,
      │       ├── agentId: string,
      │       ├── platform: {
      │       │   ├── userAgent: string,
      │       │   ├── platform: string,
      │       │   ├── type: string,
      │       │   ├── brand: string,
      │       │   ├── model: string,
      │       │   └── version: string
      │       │ },
      │       ├── lastLoginAt: timestamp,
      │       ├── createdBy: string,
      │       ├── createdAt: timestamp,
      │       ├── modifiedBy: string,
      │       ├── modifiedAt: timestamp,
      │       ├── version: number,
      │       └── isActive: boolean
      │
      ├── preferences/
      │   ├── theme/
      │   │   ├── value: string ("light", "dark", "system"),
      │   │   ├── createdBy: string,
      │   │   ├── createdAt: timestamp,
      │   │   ├── modifiedBy: string,
      │   │   ├── modifiedAt: timestamp,
      │   │   ├── version: number,
      │   │   └── isActive: boolean
      │   │
      │   ├── notifications/
      │   │   ├── settings: {
      │   │   │   ├── sms: boolean,
      │   │   │   ├── push: boolean,
      │   │   │   ├── trades: boolean,
      │   │   │   └── performance: boolean
      │   │   │ },
      │   │   ├── createdBy: string,
      │   │   ├── createdAt: timestamp,
      │   │   ├── modifiedBy: string,
      │   │   ├── modifiedAt: timestamp,
      │   │   ├── version: number,
      │   │   └── isActive: boolean
      │   │
      │   └── ethereum/
      │       ├── provider: string,
      │       ├── address: string,
      │       ├── network: string,
      │       ├── createdBy: string,
      │       ├── createdAt: timestamp,
      │       ├── modifiedBy: string,
      │       ├── modifiedAt: timestamp,
      │       ├── version: number,
      │       └── isActive: boolean
      │
      └── market_watchlist/
          └── {assetId}/
              ├── symbol: string ("BTC", "AAPL", etc.),
              ├── name: string ("Bitcoin", "Apple Inc.", etc.),
              ├── type: string ("crypto", "stock", etc.),
              ├── addedAt: timestamp,
              ├── createdBy: string,
              ├── createdAt: timestamp,
              ├── modifiedBy: string,
              ├── modifiedAt: timestamp,
              ├── version: number,
              └── isActive: boolean
```

## Fields Explanation

### Main User Document

**Profile Object**
- `name`: User's display name
- `email`: User's email address
- `photoURL`: URL to the user's profile photo
- `verifiedEmail`: Whether the user's email has been verified
- `subscriptionTier`: Subscription tier (premium, basic, free, etc.)
- `tradingMode`: Global trading mode setting ("demo" or "real")
- `isActive`: Whether the account is active

**Connected Providers Array**
- List of financial providers connected to the user's account
- Each connection includes provider name, account type, and connection timestamp

**Audit Fields (Present in all documents)**
- `createdBy`: User or system ID that created the record
- `createdAt`: Timestamp when the record was created
- `modifiedBy`: User or system ID that last modified the record
- `modifiedAt`: Timestamp when the record was last modified
- `version`: Numeric version, incremented with each update
- `isActive`: Boolean flag for soft deletion

### Authentication Methods Subcollection

Stores all authentication methods associated with a user in separate documents.

**Common Fields**
- `type`: Type of authentication method (TOTP, SMS_OTP, PASSKEY, OAUTH_GOOGLE, etc.)
- `name`: User-friendly name for the authentication method
- `lastVerifiedAt`: Timestamp when the method was last successfully used

**Type-Specific Fields**
- **TOTP**
  - `secret`: Encrypted TOTP secret
- **SMS_OTP**
  - `phoneNumber`: Phone number for SMS verification
  - `verified`: Whether the phone has been verified
- **PASSKEY**
  - `credentials`: Array of credential objects with IDs, public keys, etc.
- **OAuth Providers (Google, Facebook)**
  - `uid`: Provider-specific user ID

### API Credentials Subcollection

Stores API keys for various financial providers.

- `provider`: Name of the provider (coinbase, kraken, binanceus, etc.)
- `apiKey`: Encrypted API key
- `privateKey`: Encrypted private key (or secret key)
- Provider-specific fields as needed

### Devices Subcollection

Tracks devices used to access the account.

- `deviceName`: User-friendly device name
- `agentId`: Unique identifier for the device agent
- `platform`: Object containing device details (OS, browser, device model, etc.)
- `lastLoginAt`: Timestamp of the last login from this device

### Preferences Subcollection

Stores user preferences organized by category.

- Separate documents for different preference categories (theme, notifications, etc.)
- Each with specific settings relevant to that category

### Market Watchlist Subcollection

Tracks financial assets the user is monitoring.

- `symbol`: Trading symbol (BTC, AAPL, etc.)
- `name`: Full name of the asset
- `type`: Asset type (crypto, stock, etc.)
- `addedAt`: When the asset was added to the watchlist

## Data Integrity

This schema design ensures:

1. Clear separation of concerns with subcollections for different data types
2. Standardized audit fields across all documents and subcollections
3. Consistent data structures for authentication methods, devices, and user preferences
4. Support for multiple authentication methods per user
5. Secure storage of sensitive information in appropriate subcollections

## Sample Data

Here's an example of a populated user document and its subcollections:

```json
// Main user document: users/{userId}
{
  "profile": {
    "name": "Jane Smith",
    "email": "jane.smith@example.com",
    "photoURL": "https://example.com/photos/jane-profile.jpg",
    "verifiedEmail": true,
    "subscriptionTier": "premium",
    "tradingMode": "demo",
    "isActive": true
  },
  "connectedProviders": [
    {
      "provider": "binanceus",
      "accountType": "paper",
      "connectedAt": "2025-05-15T14:32:10Z"
    },
    {
      "provider": "kraken",
      "accountType": "real",
      "connectedAt": "2025-05-20T09:15:33Z"
    }
  ],
  "createdBy": "system",
  "createdAt": "2025-03-10T08:23:45Z",
  "modifiedBy": "jane.smith@example.com",
  "modifiedAt": "2025-05-20T09:15:33Z",
  "version": 4,
  "isActive": true
}

// Authentication method: users/{userId}/authentication_methods/{authMethodId1}
{
  "type": "TOTP",
  "name": "Google Authenticator",
  "secret": "ENCRYPTED_TOTP_SECRET_HERE",
  "lastVerifiedAt": "2025-06-01T18:22:10Z",
  "createdBy": "jane.smith@example.com",
  "createdAt": "2025-03-10T08:25:12Z",
  "modifiedBy": "jane.smith@example.com",
  "modifiedAt": "2025-03-10T08:25:12Z",
  "version": 1,
  "isActive": true
}

// Authentication method: users/{userId}/authentication_methods/{authMethodId2}
{
  "type": "OAUTH_GOOGLE",
  "name": "Google Account",
  "uid": "google-oauth2|104893341287346123456",
  "lastVerifiedAt": "2025-06-02T10:15:30Z",
  "createdBy": "system",
  "createdAt": "2025-03-10T08:23:45Z",
  "modifiedBy": "system",
  "modifiedAt": "2025-03-10T08:23:45Z",
  "version": 1,
  "isActive": true
}

// Authentication method: users/{userId}/authentication_methods/{authMethodId3}
{
  "type": "SMS_OTP",
  "name": "My iPhone",
  "phoneNumber": "+12125551234",
  "verified": true,
  "lastVerifiedAt": "2025-05-28T12:33:45Z",
  "createdBy": "jane.smith@example.com",
  "createdAt": "2025-04-15T16:42:30Z",
  "modifiedBy": "jane.smith@example.com",
  "modifiedAt": "2025-04-15T16:45:12Z",
  "version": 1,
  "isActive": true
}

// API credential: users/{userId}/api_credentials/{credentialId1}
{
  "provider": "binanceus",
  "apiKey": "ENCRYPTED_API_KEY_HERE",
  "privateKey": "ENCRYPTED_PRIVATE_KEY_HERE",
  "permissions": ["read", "trade"],
  "createdBy": "jane.smith@example.com",
  "createdAt": "2025-05-15T14:32:10Z",
  "modifiedBy": "jane.smith@example.com",
  "modifiedAt": "2025-05-15T14:32:10Z",
  "version": 1,
  "isActive": true
}

// Device: users/{userId}/devices/{deviceId1}
{
  "deviceName": "iPhone 15 Pro",
  "agentId": "agent_e7f8a234b9c12d345",
  "platform": {
    "userAgent": "Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Mobile/15E148 Safari/604.1",
    "platform": "iOS",
    "type": "mobile",
    "brand": "Apple",
    "model": "iPhone 15 Pro",
    "version": "18.0"
  },
  "lastLoginAt": "2025-06-02T21:45:10Z",
  "createdBy": "jane.smith@example.com",
  "createdAt": "2025-03-15T10:33:22Z",
  "modifiedBy": "jane.smith@example.com",
  "modifiedAt": "2025-06-02T21:45:10Z",
  "version": 3,
  "isActive": true
}

// Preference: users/{userId}/preferences/theme
{
  "value": "dark",
  "createdBy": "jane.smith@example.com",
  "createdAt": "2025-03-10T08:30:15Z",
  "modifiedBy": "jane.smith@example.com",
  "modifiedAt": "2025-05-01T11:22:33Z",
  "version": 2,
  "isActive": true
}

// Preference: users/{userId}/preferences/notifications
{
  "settings": {
    "sms": true,
    "push": true,
    "trades": true,
    "performance": false
  },
  "createdBy": "jane.smith@example.com",
  "createdAt": "2025-03-10T08:30:20Z",
  "modifiedBy": "jane.smith@example.com",
  "modifiedAt": "2025-04-22T15:12:45Z",
  "version": 3,
  "isActive": true
}

// Market watchlist item: users/{userId}/market_watchlist/{assetId1}
{
  "symbol": "BTC",
  "name": "Bitcoin",
  "type": "crypto",
  "addedAt": "2025-03-12T09:15:30Z",
  "createdBy": "jane.smith@example.com",
  "createdAt": "2025-03-12T09:15:30Z",
  "modifiedBy": "jane.smith@example.com",
  "modifiedAt": "2025-03-12T09:15:30Z",
  "version": 1,
  "isActive": true
}

// Market watchlist item: users/{userId}/market_watchlist/{assetId2}
{
  "symbol": "AAPL",
  "name": "Apple Inc.",
  "type": "stock",
  "addedAt": "2025-04-05T14:22:10Z",
  "createdBy": "jane.smith@example.com",
  "createdAt": "2025-04-05T14:22:10Z",
  "modifiedBy": "jane.smith@example.com",
  "modifiedAt": "2025-04-05T14:22:10Z",
  "version": 1,
  "isActive": true
}
```




