package io.strategiz.client.facebook.helper;

import io.strategiz.client.facebook.model.FacebookUserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.Optional;

/**
 * Helper class for retrieving user information from Facebook
 */
@Component
public class FacebookUserInfoHelper {

    private static final Logger logger = LoggerFactory.getLogger(FacebookUserInfoHelper.class);
    private static final String USER_INFO_URL = "https://graph.facebook.com/me";

    private final RestTemplate restTemplate;

    public FacebookUserInfoHelper(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Retrieve user information from Facebook using access token
     */
    public Optional<FacebookUserInfo> getUserInfo(String accessToken) {
        try {
            String userInfoUrl = buildUserInfoUrl(accessToken);
            ResponseEntity<Map<String, Object>> response = makeUserInfoRequest(userInfoUrl);
            
            if (isValidUserData(response)) {
                return extractUserInfo(response);
            }
            
            logger.error("Failed to get user data from Facebook");
            return Optional.empty();
            
        } catch (Exception e) {
            logger.error("Error getting user info from Facebook", e);
            return Optional.empty();
        }
    }

    private String buildUserInfoUrl(String accessToken) {
        return UriComponentsBuilder.fromUriString(USER_INFO_URL)
                .queryParam("fields", "id,name,email")
                .queryParam("access_token", accessToken)
                .toUriString();
    }

    private ResponseEntity<Map<String, Object>> makeUserInfoRequest(String userInfoUrl) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
        
        return restTemplate.exchange(
                userInfoUrl,
                HttpMethod.GET,
                requestEntity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );
    }

    private boolean isValidUserData(ResponseEntity<Map<String, Object>> response) {
        Map<String, Object> userData = response.getBody();
        return userData != null && userData.containsKey("id");
    }

    private Optional<FacebookUserInfo> extractUserInfo(ResponseEntity<Map<String, Object>> response) {
        Map<String, Object> userData = response.getBody();
        String facebookId = (String) userData.get("id");
        String email = (String) userData.get("email");
        String name = (String) userData.get("name");
        
        return Optional.of(new FacebookUserInfo(facebookId, email, name));
    }
} 