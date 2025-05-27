# Strategiz Data User Module

This module provides data access for user profiles and provider configurations in the Strategiz platform.

## Database Structure

The module interacts with the following Firestore collections:

### `users` Collection

The primary collection for storing user information and provider configurations.

```
users/
  └── {userId}/
      ├── displayName: string
      ├── email: string
      ├── createdAt: timestamp
      ├── accountMode: string ("PAPER" or "LIVE")
      └── providers/
          └── {providerId}/
              ├── providerType: string ("EXCHANGE" or "BROKER")
              ├── accountType: string ("PAPER" or "REAL")
              ├── settings: map (non-sensitive settings)
              └── credentials/
                  └── {documentId: "default"}/
                      ├── apiKey: string
                      ├── privateKey: string
                      └── encryptedData: map
```

#### Fields Explanation

**Users Collection**
- `displayName`: User's display name
- `email`: User's email address
- `createdAt`: Timestamp when the user account was created
- `accountMode`: Global trading mode setting
  - `PAPER`: Simulation mode across all providers
  - `LIVE`: Real connections to providers (may include broker-specific paper accounts)

**Providers Sub-collection**
- `providerId`: Identifier for the provider (e.g., "kraken", "binanceus", "alpaca")
- `providerType`: Type of provider
  - `EXCHANGE`: Cryptocurrency exchange
  - `BROKER`: Traditional brokerage
- `accountType`: Account type within the provider (primarily for brokers)
  - `PAPER`: Paper trading account
  - `REAL`: Real money account
- `settings`: Map of non-sensitive provider-specific settings
  - May include preferred currency pairs, default fiat currency, etc.

**Credentials Sub-sub-collection**
- Stores sensitive API credentials separately from provider settings
- Allows for stricter security rules to be applied
- Uses a default document ID for simplicity

## Account Mode vs. Account Type

There are two related but distinct concepts in this data model:

1. **Account Mode** (user-level setting)
   - `PAPER`: Global simulation mode, no real connections to providers
   - `LIVE`: Real connections to providers, but may use broker-specific paper accounts

2. **Account Type** (provider-level setting)
   - `PAPER`: Paper trading account within a specific provider
   - `REAL`: Real money account within a specific provider

This hierarchical approach allows users to:
- Use the platform safely in complete `PAPER` mode
- Use `LIVE` mode with broker-specific paper accounts to test strategies with real market data
- Graduate to fully live trading with real accounts when ready

## Usage Examples

Here are some examples of how to use the data-user module with the new structure:

### Registering a New Provider

```java
// 1. First create and save the provider configuration
Provider provider = new Provider();
provider.setProviderType("EXCHANGE");
provider.setAccountType("PAPER"); // Using paper trading on this exchange
Map<String, Object> settings = new HashMap<>();
settings.put("defaultFiatCurrency", "USD");
settings.put("preferredPairs", Arrays.asList("BTC/USD", "ETH/USD"));
provider.setSettings(settings);

userRepository.saveProvider(userId, "binanceus", provider);

// 2. Then save credentials separately for better security
Credentials credentials = new Credentials();
credentials.setApiKey("your-api-key");
credentials.setPrivateKey("your-private-key");

userRepository.saveCredentials(userId, "binanceus", credentials);
```

### Switching Account Mode

```java
// Switch from PAPER to LIVE mode
userRepository.updateAccountMode(userId, "LIVE");

// User's providers retain their individual accountType settings
// Some can be PAPER and others can be REAL
```

### Retrieving Provider Configuration

```java
// Get provider configuration without credentials
Optional<Provider> providerOpt = userRepository.getProvider(userId, "binanceus");
if (providerOpt.isPresent()) {
    Provider provider = providerOpt.get();
    String accountType = provider.getAccountType(); // "PAPER" or "REAL"
    Map<String, Object> settings = provider.getSettings();
    // Use settings...
}

// Get credentials only when needed for API calls
Optional<Credentials> credentialsOpt = userRepository.getCredentials(userId, "binanceus");
if (credentialsOpt.isPresent()) {
    Credentials credentials = credentialsOpt.get();
    String apiKey = credentials.getApiKey();
    String privateKey = credentials.getPrivateKey();
    // Make API call...
}
```
### Retrieving User Account Mode

```java
User user = userRepository.getUserById("user123").orElseThrow();
String accountMode = user.getAccountMode();
if ("PAPER".equals(accountMode)) {
    // Handle paper mode
} else {
    // Handle live mode
}
```

### Managing Provider Settings

```java
// Get settings for a specific provider
Optional<ProviderSettings> settings = userRepository.getProviderSettings("user123", "kraken");

// Save new settings
ProviderSettings newSettings = new ProviderSettings();
newSettings.setProviderType("EXCHANGE");
newSettings.setAccountType("REAL");
userRepository.saveProviderSettings("user123", "kraken", newSettings);
```

## Integration with Service Layer

This data module is designed to be consumed by service layer modules, which will implement business logic around user preferences and provider settings. The service layer is responsible for enforcing any business rules related to account modes and provider settings.
