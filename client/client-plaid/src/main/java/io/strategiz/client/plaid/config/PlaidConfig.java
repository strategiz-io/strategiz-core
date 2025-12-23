package io.strategiz.client.plaid.config;

import com.plaid.client.ApiClient;
import com.plaid.client.request.PlaidApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;

/**
 * Configuration for Plaid API client.
 * Loads credentials from Vault via framework-secrets.
 */
@Configuration
public class PlaidConfig {

    private static final Logger log = LoggerFactory.getLogger(PlaidConfig.class);

    @Value("${plaid.client-id:}")
    private String clientId;

    @Value("${plaid.secret:}")
    private String secret;

    @Value("${plaid.environment:sandbox}")
    private String environment; // sandbox, development, production

    // Setters for Vault injection
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getClientId() {
        return clientId;
    }

    public String getEnvironment() {
        return environment;
    }

    @Bean
    public PlaidApi plaidApi() {
        log.info("Initializing Plaid API client for environment: {}", environment);

        HashMap<String, String> apiKeys = new HashMap<>();
        apiKeys.put("clientId", clientId);
        apiKeys.put("secret", secret);

        ApiClient apiClient = new ApiClient(apiKeys);

        // Set base path based on environment
        // Plaid only supports Sandbox and Production environments
        switch (environment.toLowerCase()) {
            case "production":
                apiClient.setPlaidAdapter(ApiClient.Production);
                break;
            case "sandbox":
            case "development":
            default:
                // Development uses Sandbox for testing
                apiClient.setPlaidAdapter(ApiClient.Sandbox);
                break;
        }

        log.info("Plaid API client initialized successfully");
        return apiClient.createService(PlaidApi.class);
    }
}
