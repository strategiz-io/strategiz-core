# Coinbase Integration for Strategiz

This document outlines the Coinbase API integration for the Strategiz platform, which allows for retrieving real-time cryptocurrency data from Coinbase.

## Overview

The Coinbase integration consists of the following components:

1. **CoinbaseClient**: The main client class that handles all communication with the Coinbase API
2. **Model Classes**: Data models for representing Coinbase API responses
3. **Exception Handling**: Custom exception classes for handling Coinbase API errors

## Integration Details

The integration uses the real Coinbase API to fetch actual cryptocurrency data. No mock or simulated data is used.

### Key Features

- **Public API Access**: Fetch cryptocurrency prices, exchange rates, and other public data
- **Authenticated API Access**: Retrieve account information, balances, and transaction history (requires API credentials)
- **Secure Authentication**: Implements HMAC-SHA256 authentication as required by Coinbase
- **Error Handling**: Comprehensive error handling for API responses

## Usage Examples

### Fetching Cryptocurrency Prices

```java
// Create a RestTemplate
RestTemplate restTemplate = new RestTemplate();

// Initialize the Coinbase client
CoinbaseClient client = new CoinbaseClient(restTemplate);

// Fetch Bitcoin price
Map<String, String> params = new HashMap<>();
params.put("currency", "USD");

CoinbaseResponse<TickerPrice> response = client.publicRequest(
    HttpMethod.GET,
    "/prices/BTC-USD/spot",
    params,
    new ParameterizedTypeReference<CoinbaseResponse<TickerPrice>>() {}
);

// Get the price
String bitcoinPrice = response.getData().get(0).getAmount();
String currency = response.getData().get(0).getCurrency();
```

### Authenticated Requests (with API Credentials)

```java
// Initialize the Coinbase client
CoinbaseClient client = new CoinbaseClient(restTemplate);

// API credentials
String apiKey = "your-api-key";
String privateKey = "your-private-key";

// Fetch user accounts
CoinbaseResponse<Map<String, Object>> accountsResponse = client.signedRequest(
    HttpMethod.GET,
    "/accounts",
    null,
    apiKey,
    privateKey,
    new ParameterizedTypeReference<CoinbaseResponse<Map<String, Object>>>() {}
);

// Process the accounts
List<Map<String, Object>> accounts = accountsResponse.getData();
```

## Configuration

To use the Coinbase integration, you need to:

1. Include the `client-coinbase` module as a dependency in your project
2. Configure your Coinbase API credentials (for authenticated requests)
3. Ensure you have the required dependencies (Spring Web, Jackson, etc.)

## Security Considerations

- API keys should be stored securely and never committed to source control
- Use environment variables or a secure configuration store for API credentials
- Consider implementing rate limiting to avoid hitting Coinbase API limits

## Testing

The integration includes demonstration code that shows how to use the client to fetch real-time data from the Coinbase API.

To run the demo:
```
cd client/client-coinbase
./run-price-demo.bat
```

This will fetch and display current Bitcoin and Ethereum prices from the Coinbase API.
