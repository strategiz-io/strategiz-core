package io.strategiz.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Proxy controller to fetch the API config from api-strategiz-io.web.app
 * This helps bypass CORS issues during local development
 */
@RestController
@RequestMapping("/api")
public class ApiConfigProxyController {
    
    private static final Logger log = LoggerFactory.getLogger(ApiConfigProxyController.class);
    private static final String API_CONFIG_URL = "https://api-strategiz-io.web.app/api-config.json";
    
    @Autowired
    private RestTemplate restTemplate;
    
    /**
     * Fetches the API config JSON from api-strategiz-io.web.app and returns it
     * This acts as a proxy to avoid CORS issues
     * 
     * @return The API config JSON
     */
    @GetMapping("/proxy/api-config")
    public ResponseEntity<Object> getApiConfig() {
        log.info("Proxying request to {}", API_CONFIG_URL);
        try {
            Object apiConfig = restTemplate.getForObject(API_CONFIG_URL, Object.class);
            log.info("Successfully proxied API config");
            return ResponseEntity.ok(apiConfig);
        } catch (Exception e) {
            log.error("Error proxying API config: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }
}
