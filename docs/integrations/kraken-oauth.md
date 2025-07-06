# Kraken OAuth Integration

This document describes how to integrate with Kraken's OAuth system for secure API access.

## Overview

Kraken supports OAuth 2.0 for secure API access without exposing user credentials. The integration allows users to connect their Kraken accounts to Strategiz for trading and portfolio management.

## OAuth Flow

### 1. Authorization Request

```java
@Service
public class KrakenOAuthService {
    
    public String generateAuthorizationUrl(String clientId, String redirectUri) {
        return "https://api.kraken.com/oauth/authorize" +
               "?client_id=" + clientId +
               "&redirect_uri=" + redirectUri +
               "&response_type=code" +
               "&scope=trading,portfolio";
    }
}
```

### 2. Authorization Code Exchange

```java
public KrakenTokenResponse exchangeCodeForToken(String authorizationCode, String clientId, String clientSecret) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    
    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("grant_type", "authorization_code");
    params.add("code", authorizationCode);
    params.add("client_id", clientId);
    params.add("client_secret", clientSecret);
    
    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
    
    return restTemplate.postForObject(
        "https://api.kraken.com/oauth/token",
        request,
        KrakenTokenResponse.class
    );
}
```

### 3. Token Refresh

```java
public KrakenTokenResponse refreshAccessToken(String refreshToken, String clientId, String clientSecret) {
    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("grant_type", "refresh_token");
    params.add("refresh_token", refreshToken);
    params.add("client_id", clientId);
    params.add("client_secret", clientSecret);
    
    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
    
    return restTemplate.postForObject(
        "https://api.kraken.com/oauth/token",
        request,
        KrakenTokenResponse.class
    );
}
```

## Configuration

### Application Properties

```properties
# Kraken OAuth Configuration
kraken.oauth.client-id=${KRAKEN_CLIENT_ID}
kraken.oauth.client-secret=${KRAKEN_CLIENT_SECRET}
kraken.oauth.redirect-uri=${KRAKEN_REDIRECT_URI}
kraken.oauth.scope=trading,portfolio
kraken.oauth.authorization-uri=https://api.kraken.com/oauth/authorize
kraken.oauth.token-uri=https://api.kraken.com/oauth/token
```

### Configuration Class

```java
@Configuration
@ConfigurationProperties(prefix = "kraken.oauth")
public class KrakenOAuthConfig {
    
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String scope;
    private String authorizationUri;
    private String tokenUri;
    
    // Getters and setters...
}
```

## Security Considerations

### 1. Token Storage

- **Encrypt tokens** before storing in database
- **Use secure storage** for refresh tokens
- **Implement token rotation** for enhanced security

### 2. API Rate Limiting

- **Respect Kraken's rate limits**
- **Implement exponential backoff**
- **Cache frequently accessed data**

### 3. Error Handling

```java
@Component
public class KrakenErrorHandler {
    
    public void handleOAuthError(KrakenOAuthException e) {
        switch (e.getErrorCode()) {
            case "invalid_grant":
                // Handle expired authorization code
                break;
            case "invalid_client":
                // Handle invalid client credentials
                break;
            case "access_denied":
                // Handle user denied access
                break;
            default:
                // Handle other errors
        }
    }
}
```

## Testing

### Test Script

The `scripts/test-kraken-oauth.sh` script can be used to test the OAuth flow:

```bash
#!/bin/bash
# Test Kraken OAuth integration
curl -X POST "http://localhost:9090/api/v1/oauth/kraken/authorize" \
  -H "Content-Type: application/json" \
  -d '{
    "redirectUri": "http://localhost:3000/oauth/callback",
    "scope": "trading,portfolio"
  }'
```

### Unit Tests

```java
@SpringBootTest
class KrakenOAuthServiceTest {
    
    @Test
    void testAuthorizationUrlGeneration() {
        String authUrl = krakenOAuthService.generateAuthorizationUrl(
            "test-client-id",
            "http://localhost:3000/callback"
        );
        
        assertThat(authUrl).contains("client_id=test-client-id");
        assertThat(authUrl).contains("redirect_uri=http://localhost:3000/callback");
    }
}
```

## API Endpoints

### Authorization

```http
POST /api/v1/oauth/kraken/authorize
Content-Type: application/json

{
  "redirectUri": "http://localhost:3000/oauth/callback",
  "scope": "trading,portfolio"
}
```

### Token Exchange

```http
POST /api/v1/oauth/kraken/token
Content-Type: application/json

{
  "authorizationCode": "abc123",
  "redirectUri": "http://localhost:3000/oauth/callback"
}
```

### Token Refresh

```http
POST /api/v1/oauth/kraken/refresh
Content-Type: application/json

{
  "refreshToken": "refresh_token_here"
}
```

## Troubleshooting

### Common Issues

1. **Invalid redirect URI** - Ensure the redirect URI is registered in Kraken's OAuth settings
2. **Expired authorization code** - Authorization codes expire after 10 minutes
3. **Invalid scope** - Ensure requested scopes are authorized for your application
4. **Rate limiting** - Implement proper rate limiting and backoff strategies

### Debug Logging

Enable debug logging for OAuth operations:

```properties
logging.level.io.strategiz.service.provider.kraken=DEBUG
logging.level.org.springframework.web.client.RestTemplate=DEBUG
``` 