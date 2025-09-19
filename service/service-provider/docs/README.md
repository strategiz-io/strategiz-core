# Service Provider Module Documentation

This module implements the provider integration services for Strategiz, allowing users to connect various trading platforms and exchanges to their Strategiz account.

## 📚 Controller Documentation

Each controller in this module has comprehensive documentation following our standard template. Documentation includes business purpose, technical specs, design diagrams, testing, and monitoring details.

### Available API Documentation

| API Name | Controller | Documentation | Status | Description |
|----------|------------|---------------|--------|-------------|
| Provider Connection API | CreateProviderController | [📄 View](provider-connection-api.mdx) | ✅ Complete | Connect external providers (exchanges/brokerages) |
| Provider Query API | ReadProviderController | _pending_ | 📝 TODO | Query provider information and status |
| Provider Update API | UpdateProviderController | _pending_ | 📝 TODO | Update provider configurations |
| Provider Disconnection API | DeleteProviderController | _pending_ | 📝 TODO | Disconnect and remove providers |
| Provider OAuth Callback API | ProviderCallbackController | _pending_ | 📝 TODO | Handle OAuth authorization callbacks |

### Documentation Template

New controllers should be documented using our standard template:
- [📋 Controller Documentation Template](CONTROLLER_DOCUMENTATION_TEMPLATE.mdx)

## Architecture Overview

The provider integration service is built using a provider strategy pattern with these key components:

1. **ProviderApiService Interface**: Defines the contract for all provider implementations
2. **AbstractProviderApiService**: Base class providing shared functionality 
3. **ProviderApiFactory**: Factory that returns the appropriate provider implementation
4. **ProviderOAuthService**: Facade service that delegates to provider-specific implementations

## Supported Providers

The service currently supports or plans to support these providers:

| Provider | Status | Documentation |
|----------|--------|---------------|
| [Kraken](src/main/java/io/strategiz/service/provider/kraken/README.md) | Implemented | [Kraken API Docs](https://docs.kraken.com/rest/) |
| BinanceUS | Planned | [BinanceUS API Docs](https://docs.binance.us/) |
| Charles Schwab | Planned | [Charles Schwab API Docs](https://developer.schwab.com/) |

Click on the provider name to view detailed integration documentation.

## Getting Started

1. Review the provider-specific README files for detailed setup instructions.
2. Configure the necessary credentials in your application properties.
3. Ensure the service-provider module is included in your application dependencies.

## Adding a New Provider

To add a new provider:

1. Create a new package under `io.strategiz.service.provider.<provider-name>`
2. Implement the `ProviderApiService` interface in a `<Provider>ApiService` class
3. Create a README.md file with integration instructions
4. Add appropriate tests

Example implementation structure:
```
service-provider/
├── src/
│   └── main/
│       └── java/
│           └── io/
│               └── strategiz/
│                   └── service/
│                       └── provider/
│                           ├── api/
│                           │   ├── ProviderApiService.java
│                           │   ├── AbstractProviderApiService.java
│                           │   └── ProviderApiFactory.java
│                           ├── kraken/
│                           │   ├── KrakenApiService.java
│                           │   └── README.md
│                           ├── binanceus/
│                           │   └── BinanceUSApiService.java
│                           ├── controller/
│                           │   └── ProviderOAuthController.java
│                           └── ProviderOAuthService.java
```

## Security Considerations

All provider integrations follow these security principles:

- OAuth client credentials are stored securely and never exposed
- Access tokens and refresh tokens are stored in the user repository
- All API requests use HTTPS
- Sensitive data is never logged or included in API responses
- Token renewal happens automatically when required

## Testing

Run unit tests:
```bash
./gradlew test
```

Run integration tests:
```bash
./gradlew integrationTest
```
