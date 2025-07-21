package io.strategiz.framework.secrets.client;

import io.strategiz.framework.secrets.config.VaultProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * HTTP implementation of VaultClient.
 * Handles all HTTP communication with Vault server.
 */
@Component
public class VaultHttpClient implements VaultClient {
    
    private static final Logger log = LoggerFactory.getLogger(VaultHttpClient.class);
    
    private final RestTemplate restTemplate;
    private final VaultProperties properties;
    private final Environment environment;
    
    @Autowired
    public VaultHttpClient(VaultProperties properties, Environment environment) {
        this.restTemplate = new RestTemplate();
        this.properties = properties;
        this.environment = environment;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> read(String path) {
        try {
            String url = properties.getAddress() + "/v1/" + path;
            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
                if (data != null && data.containsKey("data")) {
                    // KV v2 format
                    return (Map<String, Object>) data.get("data");
                } else {
                    // KV v1 format or direct data
                    return data;
                }
            }
            
            return null;
            
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return null;
            }
            throw new RuntimeException("Failed to read from Vault: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void write(String path, Map<String, Object> data) {
        String url = properties.getAddress() + "/v1/" + path;
        HttpHeaders headers = createHeaders();
        
        // For KV v2, wrap data in "data" field
        Map<String, Object> payload = new HashMap<>();
        payload.put("data", data);
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
        
        try {
            restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            log.debug("Successfully wrote to Vault path: {}", path);
        } catch (Exception e) {
            throw new RuntimeException("Failed to write to Vault: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void delete(String path) {
        String url = properties.getAddress() + "/v1/" + path;
        HttpHeaders headers = createHeaders();
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        try {
            restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);
            log.debug("Successfully deleted Vault path: {}", path);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete from Vault: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean isHealthy() {
        try {
            String url = properties.getAddress() + "/v1/sys/health";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            log.debug("Vault health check failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Create HTTP headers with Vault token
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // Get token from environment or Spring Cloud Vault configuration
        String token = environment.getProperty("VAULT_TOKEN");
        if (token == null) {
            token = environment.getProperty("spring.cloud.vault.token", "root-token");
        }
        headers.set("X-Vault-Token", token);
        
        return headers;
    }
}