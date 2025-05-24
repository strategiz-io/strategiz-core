package io.strategiz.service.portfolio;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of BrokerageService for Robinhood
 */
@Slf4j
@Service
public class RobinhoodBrokerageService implements BrokerageService {

    @Override
    public Map<String, Object> getPortfolioData(Map<String, String> credentials) {
        try {
            // Validate credentials
            if (credentials == null || !credentials.containsKey("username") || !credentials.containsKey("password")) {
                throw new IllegalArgumentException("Missing required Robinhood credentials");
            }
            
            // Get raw data
            Object rawData = getRawAccountData(credentials);
            
            // Process raw data into standardized portfolio format
            // TODO: Implement proper data transformation
            Map<String, Object> processedData = new HashMap<>();
            processedData.put("provider", getProviderName());
            processedData.put("rawData", rawData);
            // Add more processed data here
            
            return processedData;
        } catch (Exception e) {
            log.error("Error getting Robinhood portfolio data", e);
            throw new RuntimeException("Failed to get Robinhood portfolio data: " + e.getMessage(), e);
        }
    }

    @Override
    public Object getRawAccountData(Map<String, String> credentials) {
        try {
            // Extract credentials
            String username = credentials.get("username");
            String password = credentials.get("password");
            
            // Robinhood unofficial API endpoints
            String loginUrl = "https://api.robinhood.com/oauth2/token/";
            String accountsUrl = "https://api.robinhood.com/accounts/";

            // Prepare login payload
            String payload = String.format(
                "grant_type=password&scope=internal&client_id=c82SH0WZOsabOXGP2sxqcj34FxkvfnWRZBKlBjFS&expires_in=86400&username=%s&password=%s",
                username, password
            );

            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest loginRequest = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(loginUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(payload))
                .build();

            java.net.http.HttpResponse<String> loginResponse = client.send(loginRequest, java.net.http.HttpResponse.BodyHandlers.ofString());
            if (loginResponse.statusCode() != 200) {
                log.error("Robinhood login failed: {}", loginResponse.body());
                throw new RuntimeException("Robinhood login failed: " + loginResponse.body());
            }

            // Parse access token from login response
            com.fasterxml.jackson.databind.JsonNode loginJson =
                new com.fasterxml.jackson.databind.ObjectMapper().readTree(loginResponse.body());
            String accessToken = loginJson.get("access_token").asText();

            // Fetch account data
            java.net.http.HttpRequest accountsRequest = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(accountsUrl))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

            java.net.http.HttpResponse<String> accountsResponse = client.send(accountsRequest, java.net.http.HttpResponse.BodyHandlers.ofString());
            if (accountsResponse.statusCode() != 200) {
                log.error("Robinhood accounts fetch failed: {}", accountsResponse.body());
                throw new RuntimeException("Robinhood accounts fetch failed: " + accountsResponse.body());
            }

            // Return raw JSON
            return new com.fasterxml.jackson.databind.ObjectMapper().readTree(accountsResponse.body());
        } catch (Exception e) {
            log.error("Error fetching Robinhood raw data", e);
            throw new RuntimeException("Failed to fetch Robinhood data: " + e.getMessage(), e);
        }
    }
    
    @Override
    public String getProviderName() {
        return "robinhood";
    }
}
