package io.strategiz.binanceus.controller;

import io.strategiz.binanceus.util.BinanceUSConfigHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for managing Binance US configuration
 */
@RestController
@RequestMapping("/api/binanceus/config")
public class BinanceUSConfigController {
    
    private static final Logger log = LoggerFactory.getLogger(BinanceUSConfigController.class);
    
    @Autowired
    private BinanceUSConfigHelper configHelper;
    
    /**
     * Check if Binance US API keys are configured for a user
     * 
     * @param email User email
     * @return Configuration status and details
     */
    @GetMapping("/check/{email}")
    @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "https://strategiz.io"}, allowedHeaders = "*")
    public ResponseEntity<Object> checkBinanceUSConfig(@PathVariable String email) {
        log.info("Received request to check Binance US configuration for user: {}", email);
        
        Map<String, Object> result = configHelper.checkBinanceUSConfig(email);
        
        if ("error".equals(result.get("status"))) {
            return ResponseEntity.badRequest().body(result);
        }
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * Add Binance US configuration for a user
     * 
     * @param request Request containing email, apiKey, and secretKey
     * @return Operation status and details
     */
    @PostMapping("/add")
    @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "https://strategiz.io"}, allowedHeaders = "*")
    public ResponseEntity<Object> addBinanceUSConfig(@RequestBody Map<String, String> request) {
        log.info("Received request to add Binance US configuration");
        
        String email = request.get("email");
        String apiKey = request.get("apiKey");
        String secretKey = request.get("secretKey");
        
        if (email == null || apiKey == null || secretKey == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Email, apiKey, and secretKey are required"
            ));
        }
        
        Map<String, Object> result = configHelper.addBinanceUSConfig(email, apiKey, secretKey);
        
        if ("error".equals(result.get("status"))) {
            return ResponseEntity.badRequest().body(result);
        }
        
        return ResponseEntity.ok(result);
    }
}
