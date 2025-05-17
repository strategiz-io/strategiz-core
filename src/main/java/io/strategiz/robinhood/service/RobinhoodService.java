package io.strategiz.robinhood.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RobinhoodService {
    public Object getRawAccountData(String username, String password) {
        try {
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
}
