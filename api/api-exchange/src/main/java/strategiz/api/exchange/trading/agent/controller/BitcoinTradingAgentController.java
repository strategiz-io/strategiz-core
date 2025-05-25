package strategiz.api.exchange.trading.agent.controller;

import io.strategiz.service.exchange.coinbase.FirestoreService;
import io.strategiz.service.exchange.trading.agent.model.TradingSignal;
import io.strategiz.service.exchange.trading.agent.service.BitcoinTradingAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.List;

/**
 * Controller for Bitcoin trading agent API
 * This controller exposes endpoints to generate trading signals based on real Coinbase data
 */
@RestController
@RequestMapping("/api/trading/bitcoin")
@Slf4j
public class BitcoinTradingAgentController {

    @Autowired
    private BitcoinTradingAgent bitcoinTradingAgent;
    
    @Autowired
    @Qualifier("coinbaseFirestoreService")
    private FirestoreService firestoreService;
    
    private static final List<String> VALID_TIMEFRAMES = Arrays.asList("1d", "6h", "1h", "15m");
    
    /**
     * Generate a trading signal for Bitcoin
     * 
     * @param httpRequest HTTP request
     * @param timeframe Timeframe for analysis (1d, 6h, 1h, 15m)
     * @return Trading signal with buy/sell recommendation
     */
    @GetMapping("/signal")
    @CrossOrigin(origins = {"http://localhost:3000", "https://strategiz.io"}, allowedHeaders = "*")
    public ResponseEntity<Map<String, Object>> generateTradingSignal(
            HttpServletRequest httpRequest,
            @RequestParam(value = "timeframe", defaultValue = "1d") String timeframe) {
        
        log.info("Received request to generate Bitcoin trading signal on {} timeframe", timeframe);
        
        // Validate timeframe
        if (!VALID_TIMEFRAMES.contains(timeframe)) {
            log.error("Invalid timeframe requested: {}", timeframe);
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Invalid Timeframe",
                "message", "Supported timeframes: " + String.join(", ", VALID_TIMEFRAMES),
                "timestamp", new Date().toString()
            ));
        }
        
        try {
            // Get the user email from the request header or parameter
            String userId = httpRequest.getHeader("X-User-Email");
            if (userId == null || userId.isEmpty()) {
                userId = httpRequest.getParameter("email");
                
                if (userId == null || userId.isEmpty()) {
                    log.error("No user email provided");
                    return ResponseEntity.badRequest().body(Map.of(
                        "error", "User Not Found",
                        "message", "User email is required to access trading signals",
                        "timestamp", new Date().toString()
                    ));
                }
            }
            
            log.info("Generating Bitcoin trading signal for user: {}", userId);
            
            // Get Coinbase API credentials from Firestore
            Map<String, String> credentials = firestoreService.getCoinbaseCredentials(userId);
            
            if (credentials == null || credentials.isEmpty()) {
                log.warn("Coinbase API credentials not found for user: {}", userId);
                return ResponseEntity.ok(Map.of(
                    "error", "Credentials Not Found",
                    "message", "Coinbase API credentials not configured. Please set up your Coinbase API credentials first.",
                    "timestamp", new Date().toString(),
                    "userEmail", userId
                ));
            }
            
            String apiKey = credentials.get("apiKey");
            String privateKey = credentials.get("privateKey");
            String passphrase = credentials.get("passphrase");
            
            // Check if we have valid credentials including passphrase
            if (apiKey == null || apiKey.isEmpty() || privateKey == null || privateKey.isEmpty() 
                    || passphrase == null || passphrase.isEmpty()) {
                log.warn("Coinbase API credentials incomplete for user: {}", userId);
                return ResponseEntity.ok(Map.of(
                    "error", "Incomplete Credentials",
                    "message", "Coinbase API credentials incomplete. Make sure you have API key, private key, and passphrase configured.",
                    "timestamp", new Date().toString(),
                    "userEmail", userId
                ));
            }
            
            // Generate trading signal using real Coinbase data
            TradingSignal signal = bitcoinTradingAgent.generateTradingSignal(apiKey, privateKey, timeframe);
            
            // Prepare the response
            Map<String, Object> response = new HashMap<>();
            response.put("signal", signal.getSignalType().toString());
            response.put("strength", signal.getStrength().toString());
            response.put("strengthValue", signal.getStrength().ordinal());
            response.put("asset", signal.getAssetSymbol());
            response.put("currentPrice", signal.getCurrentPrice());
            response.put("targetPrice", signal.getTargetPrice());
            response.put("rationale", signal.getRationale());
            response.put("timeframe", signal.getTimeframe());
            response.put("timestamp", signal.getTimestamp().toString());
            response.put("formattedSignal", signal.getFormattedSignal());
            response.put("additionalMetrics", signal.getAdditionalMetrics());
            
            log.info("Successfully generated Bitcoin trading signal for user: {}", userId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error generating Bitcoin trading signal: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Internal Server Error",
                "message", "Error generating Bitcoin trading signal: " + e.getMessage(),
                "timestamp", new Date().toString()
            ));
        }
    }
    
    /**
     * Get available timeframes for Bitcoin trading signal analysis
     * 
     * @return Available timeframes
     */
    @GetMapping("/timeframes")
    @CrossOrigin(origins = {"http://localhost:3000", "https://strategiz.io"}, allowedHeaders = "*")
    public ResponseEntity<Map<String, Object>> getAvailableTimeframes() {
        Map<String, Object> response = new HashMap<>();
        response.put("timeframes", VALID_TIMEFRAMES);
        response.put("defaultTimeframe", "1d");
        response.put("timestamp", new Date().toString());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Health check endpoint for Bitcoin trading agent
     * 
     * @return Health status
     */
    @GetMapping("/health")
    @CrossOrigin(origins = {"http://localhost:3000", "https://strategiz.io"}, allowedHeaders = "*")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        log.info("Received health check request for Bitcoin trading agent");
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "up");
        response.put("service", "Bitcoin Trading Agent");
        response.put("timestamp", new Date().toString());
        
        return ResponseEntity.ok(response);
    }
}
