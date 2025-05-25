package strategiz.api.exchange.coinbase.controller;

import strategiz.service.exchange.coinbase.util.CoinbaseConfigHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for managing Coinbase configuration
 */
@RestController
@RequestMapping("/api/coinbase/config")
public class CoinbaseConfigController {
    
    private static final Logger log = LoggerFactory.getLogger(CoinbaseConfigController.class);
    
    @Autowired
    private CoinbaseConfigHelper configHelper;
    
    /**
     * Check if Coinbase API keys are configured for a user
     * 
     * @param email User email
     * @return Configuration status and details
     */
    @GetMapping("/check/{email}")
    public ResponseEntity<Object> checkCoinbaseConfig(@PathVariable String email) {
        log.info("Received request to check Coinbase configuration for user: {}", email);
        
        Map<String, Object> result = configHelper.checkCoinbaseConfig(email);
        
        if ("error".equals(result.get("status"))) {
            return ResponseEntity.badRequest().body(result);
        }
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * Add Coinbase configuration for a user
     * 
     * @param request Request containing email, apiKey, and privateKey
     * @return Operation status and details
     */
    @PostMapping("/add")
    public ResponseEntity<Object> addCoinbaseConfig(@RequestBody Map<String, String> request) {
        log.info("Received request to add Coinbase configuration");
        
        String email = request.get("email");
        String apiKey = request.get("apiKey");
        String privateKey = request.get("privateKey");
        
        if (email == null || apiKey == null || privateKey == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Email, apiKey, and privateKey are required"
            ));
        }
        
        Map<String, Object> result = configHelper.addCoinbaseConfig(email, apiKey, privateKey);
        
        if ("error".equals(result.get("status"))) {
            return ResponseEntity.badRequest().body(result);
        }
        
        return ResponseEntity.ok(result);
    }
}
