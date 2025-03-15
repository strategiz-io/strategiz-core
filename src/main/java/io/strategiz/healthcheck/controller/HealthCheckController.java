package io.strategiz.healthcheck.controller;

import io.strategiz.binanceus.service.BinanceUSService;
import io.strategiz.healthcheck.model.StatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for health check endpoints
 * Provides endpoints to check the health of various services
 */
@RestController
public class HealthCheckController {

    private static final Logger log = LoggerFactory.getLogger(HealthCheckController.class);

    @Autowired
    private BinanceUSService binanceUSService;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    @Qualifier("binanceRestTemplate")
    private RestTemplate binanceRestTemplate;

    /**
     * Basic health check endpoint (replaces the previous controller's endpoint)
     * @return Status information
     */
    @GetMapping("/api/health")
    public Map<String, Object> healthCheck() {
        log.info("Basic health check endpoint called");
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("message", "Strategiz Core API is running");
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    /**
     * General API status endpoint
     * 
     * @return Status response
     */
    @GetMapping("/api/healthcheck/status")
    public ResponseEntity<StatusResponse> getStatus() {
        StatusResponse response = new StatusResponse();
        response.setStatus("success");
        response.setMessage("API is operational");
        response.setTimestamp(System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    /**
     * Test connectivity to the Binance US API
     * This endpoint checks if the Binance US API is accessible
     * 
     * @return Status response with raw API response
     */
    @GetMapping("/api/healthcheck/binanceus")
    @CrossOrigin(origins = {"http://localhost:3000", "https://strategiz.io"}, allowedHeaders = "*", allowCredentials = "true")
    public ResponseEntity<Object> testBinanceUSApi() {
        log.info("Testing connectivity to Binance US API");
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", System.currentTimeMillis());
        
        try {
            // Add custom headers to avoid browser detection
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            headers.set("Accept", "application/json");
            headers.set("Content-Type", "application/json");
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            // Use the specialized binanceRestTemplate for this request
            ResponseEntity<String> rawResponse = binanceRestTemplate.exchange(
                "https://api.binance.us/api/v3/ping", 
                HttpMethod.GET, 
                entity, 
                String.class
            );
            
            log.info("Raw response from Binance US API: {}", rawResponse.getBody());
            
            response.put("status", "success");
            response.put("message", "Connected to Binance US API");
            response.put("rawResponse", rawResponse.getBody());
            response.put("statusCode", rawResponse.getStatusCodeValue());
            response.put("headers", rawResponse.getHeaders());
            
            // Also try the service method for comparison
            try {
                Object serviceResponse = binanceUSService.publicRequest(
                    HttpMethod.GET, 
                    "/api/v3/ping", 
                    null, 
                    Object.class
                );
                response.put("serviceResponse", serviceResponse);
            } catch (Exception e) {
                response.put("serviceError", e.getMessage());
                response.put("serviceErrorType", e.getClass().getSimpleName());
            }
            
            // Also try the direct request method
            try {
                String directResponse = binanceUSService.directRequest("/api/v3/ping", null);
                response.put("directResponse", directResponse);
            } catch (Exception e) {
                response.put("directError", e.getMessage());
                response.put("directErrorType", e.getClass().getSimpleName());
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error connecting to Binance US API: {}", e.getMessage(), e);
            
            response.put("status", "error");
            response.put("message", "Error connecting to Binance US API: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
            
            if (e instanceof ResourceAccessException) {
                log.error("Network error connecting to Binance US API", e);
                response.put("detail", "Network error: Unable to connect to Binance US API. Check your network connection and proxy settings.");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
            }
            
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(response);
        }
    }

    /**
     * Debug endpoint to test connectivity to the Binance US API with detailed information
     * 
     * @return Detailed connectivity information
     */
    @GetMapping("/api/healthcheck/binanceus/debug")
    @CrossOrigin(origins = {"http://localhost:3000", "https://strategiz.io"}, allowedHeaders = "*", allowCredentials = "true")
    public ResponseEntity<Object> debugBinanceUSConnectivity() {
        log.info("Received request for detailed Binance US API connectivity debugging");
        
        try {
            Map<String, Object> debugInfo = new HashMap<>();
            debugInfo.put("timestamp", System.currentTimeMillis());
            
            // Try direct ping with standard RestTemplate
            try {
                String rawPingResponse = restTemplate.getForObject("https://api.binance.us/api/v3/ping", String.class);
                debugInfo.put("standardRestTemplatePingSuccess", true);
                debugInfo.put("standardRestTemplatePingResponse", rawPingResponse);
            } catch (Exception e) {
                debugInfo.put("standardRestTemplatePingSuccess", false);
                debugInfo.put("standardRestTemplatePingError", e.getMessage());
                debugInfo.put("standardRestTemplatePingErrorType", e.getClass().getSimpleName());
            }
            
            // Try direct ping with specialized binanceRestTemplate
            try {
                // Add custom headers to avoid browser detection
                HttpHeaders headers = new HttpHeaders();
                headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
                headers.set("Accept", "application/json");
                headers.set("Content-Type", "application/json");
                
                HttpEntity<String> entity = new HttpEntity<>(headers);
                
                ResponseEntity<String> rawResponse = binanceRestTemplate.exchange(
                    "https://api.binance.us/api/v3/ping", 
                    HttpMethod.GET, 
                    entity, 
                    String.class
                );
                
                debugInfo.put("binanceRestTemplatePingSuccess", true);
                debugInfo.put("binanceRestTemplatePingResponse", rawResponse.getBody());
                debugInfo.put("binanceRestTemplatePingStatusCode", rawResponse.getStatusCodeValue());
                debugInfo.put("binanceRestTemplatePingHeaders", rawResponse.getHeaders());
            } catch (Exception e) {
                debugInfo.put("binanceRestTemplatePingSuccess", false);
                debugInfo.put("binanceRestTemplatePingError", e.getMessage());
                debugInfo.put("binanceRestTemplatePingErrorType", e.getClass().getSimpleName());
            }
            
            // Try service ping
            try {
                Object servicePingResponse = binanceUSService.publicRequest(
                    HttpMethod.GET, 
                    "/api/v3/ping", 
                    null, 
                    Object.class
                );
                debugInfo.put("servicePingSuccess", true);
                debugInfo.put("servicePingResponse", servicePingResponse);
            } catch (Exception e) {
                debugInfo.put("servicePingSuccess", false);
                debugInfo.put("servicePingError", e.getMessage());
                debugInfo.put("servicePingErrorType", e.getClass().getSimpleName());
            }
            
            // Try direct request method
            try {
                String directResponse = binanceUSService.directRequest("/api/v3/ping", null);
                debugInfo.put("directRequestSuccess", true);
                debugInfo.put("directRequestResponse", directResponse);
            } catch (Exception e) {
                debugInfo.put("directRequestSuccess", false);
                debugInfo.put("directRequestError", e.getMessage());
                debugInfo.put("directRequestErrorType", e.getClass().getSimpleName());
            }
            
            // Get system proxy settings
            debugInfo.put("systemProxyHost", System.getProperty("http.proxyHost"));
            debugInfo.put("systemProxyPort", System.getProperty("http.proxyPort"));
            
            return ResponseEntity.ok(debugInfo);
        } catch (Exception e) {
            log.error("Error during Binance US API connectivity debugging: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Error during Binance US API connectivity debugging: " + e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            errorResponse.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Debug endpoint to test connectivity to the Binance US API with detailed information
     * 
     * @return Detailed connectivity information
     */
    @GetMapping("/api/healthcheck/binanceus/public-test")
    @CrossOrigin(origins = {"http://localhost:3000", "https://strategiz.io"}, allowedHeaders = "*")
    public ResponseEntity<Object> publicTestBinanceUSConnectivity() {
        log.info("Received public request for Binance US API connectivity test");
        
        try {
            // Test basic connectivity to Binance US API
            Map<String, Object> result = new HashMap<>();
            result.put("timestamp", System.currentTimeMillis());
            
            try {
                // Try to ping the Binance US API directly with RestTemplate
                log.info("Testing ping endpoint with direct RestTemplate call");
                String rawPingResponse = restTemplate.getForObject("https://api.binance.us/api/v3/ping", String.class);
                
                result.put("directPingSuccess", true);
                result.put("directPingResponse", rawPingResponse);
                log.info("Direct ping successful: {}", rawPingResponse);
                
                // Try to ping using the service
                log.info("Testing ping endpoint with service");
                Object servicePingResponse = binanceUSService.publicRequest(
                    HttpMethod.GET, 
                    "/api/v3/ping", 
                    null, 
                    Object.class
                );
                
                result.put("servicePingSuccess", true);
                result.put("servicePingResponse", servicePingResponse);
                log.info("Service ping successful: {}", servicePingResponse);
                
                // Try to get exchange info directly
                log.info("Testing exchange info endpoint with direct RestTemplate call");
                String rawExchangeInfo = restTemplate.getForObject("https://api.binance.us/api/v3/exchangeInfo", String.class);
                
                result.put("directExchangeInfoSuccess", true);
                result.put("directExchangeInfoResponseLength", rawExchangeInfo != null ? rawExchangeInfo.length() : 0);
                result.put("directExchangeInfoResponsePreview", 
                    rawExchangeInfo != null && rawExchangeInfo.length() > 500 ? 
                    rawExchangeInfo.substring(0, 500) + "..." : 
                    rawExchangeInfo);
                log.info("Direct exchange info successful");
                
                result.put("status", "success");
                result.put("message", "Successfully connected to Binance US API");
            } catch (Exception e) {
                log.error("Error connecting to Binance US API: {}", e.getMessage(), e);
                result.put("status", "error");
                result.put("message", "Error connecting to Binance US API: " + e.getMessage());
                result.put("errorType", e.getClass().getSimpleName());
                
                if (e instanceof ResourceAccessException) {
                    result.put("errorDetail", "Network error: Unable to connect to Binance US API. Check network and proxy settings.");
                }
            }
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Unexpected error during public connectivity test: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Unexpected error: " + e.getMessage());
            errorResponse.put("error", e.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
