package io.strategiz.client.google.helper;

import io.strategiz.client.google.model.GoogleTokenResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

/**
 * Helper class for Google OAuth token operations
 * Following Single Responsibility Principle - only handles token exchange
 */
@Component
public class GoogleTokenHelper {

    private static final Logger logger = LoggerFactory.getLogger(GoogleTokenHelper.class);
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";

    private final RestTemplate restTemplate;

    public GoogleTokenHelper(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Exchange authorization code for access token
     * 
     * @param code Authorization code from Google
     * @param clientId Google OAuth client ID
     * @param clientSecret Google OAuth client secret
     * @param redirectUri Redirect URI used in authorization
     * @return Google access token response
     */
    public Optional<GoogleTokenResponse> exchangeCodeForToken(String code, String clientId, 
                                                             String clientSecret, String redirectUri) {
        try {
            HttpEntity<MultiValueMap<String, String>> request = buildTokenRequest(code, clientId, clientSecret, redirectUri);
            ResponseEntity<Map<String, Object>> response = makeTokenRequest(request);
            
            if (isSuccessfulResponse(response)) {
                return extractTokenFromResponse(response);
            }
            
            logger.error("Failed to get access token from Google");
            return Optional.empty();
            
        } catch (Exception e) {
            logger.error("Error exchanging code for token", e);
            return Optional.empty();
        }
    }

    private HttpEntity<MultiValueMap<String, String>> buildTokenRequest(String code, String clientId, 
                                                                       String clientSecret, String redirectUri) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("redirect_uri", redirectUri);
        body.add("grant_type", "authorization_code");
        
        return new HttpEntity<>(body, headers);
    }

    private ResponseEntity<Map<String, Object>> makeTokenRequest(HttpEntity<MultiValueMap<String, String>> request) {
        return restTemplate.exchange(
                TOKEN_URL,
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );
    }

    private boolean isSuccessfulResponse(ResponseEntity<Map<String, Object>> response) {
        return response.getStatusCode().is2xxSuccessful() && 
               response.getBody() != null && 
               response.getBody().containsKey("access_token");
    }

    private Optional<GoogleTokenResponse> extractTokenFromResponse(ResponseEntity<Map<String, Object>> response) {
        Map<String, Object> body = response.getBody();
        String accessToken = (String) body.get("access_token");
        String tokenType = (String) body.get("token_type");
        Object expiresInObj = body.get("expires_in");
        Integer expiresIn = expiresInObj instanceof Integer ? (Integer) expiresInObj : null;
        String refreshToken = (String) body.get("refresh_token");
        String scope = (String) body.get("scope");
        
        if (accessToken != null) {
            return Optional.of(new GoogleTokenResponse(accessToken, tokenType, expiresIn, refreshToken, scope));
        }
        return Optional.empty();
    }
} 