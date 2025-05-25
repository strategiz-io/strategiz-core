package strategiz.api.exchange.trading.agent.gemini.controller;

import strategiz.service.exchange.firestore.FirestoreService;
import strategiz.service.exchange.trading.agent.gemini.GeminiTradingAgent;
import strategiz.service.exchange.trading.agent.gemini.model.GeminiTradingSignal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Controller for Gemini AI Trading Agent API
 * Uses real Coinbase API data with Google's Vertex AI and Agent Development Kit (ADK)
 */
@RestController
@RequestMapping("/api/trading/gemini")
@Slf4j
public class GeminiTradingController {

    @Autowired
    private GeminiTradingAgent geminiTradingAgent;
    
    @Autowired
    @Qualifier("coinbaseFirestoreService")
    private FirestoreService firestoreService;
    
    private static final List<String> VALID_TIMEFRAMES = Arrays.asList("1d", "6h", "1h", "15m");
    private static final List<String> VALID_RISK_PROFILES = Arrays.asList("conservative", "moderate", "aggressive");
    
    /**
     * Generate an AI trading signal for Bitcoin using Gemini and real Coinbase data
     * 
     * @param httpRequest HTTP request
     * @param timeframe Timeframe for analysis (1d, 6h, 1h, 15m)
     * @param riskProfile Risk profile for analysis (conservative, moderate, aggressive)
     * @return AI-generated trading signal
     */
    @GetMapping("/signal")
    @CrossOrigin(origins = {"http://localhost:3000", "https://strategiz.io"}, allowedHeaders = "*")
    public ResponseEntity<Map<String, Object>> generateAITradingSignal(
            HttpServletRequest httpRequest,
            @RequestParam(value = "timeframe", defaultValue = "1d") String timeframe,
            @RequestParam(value = "riskProfile", defaultValue = "moderate") String riskProfile) {
        
        String requestId = UUID.randomUUID().toString();
        log.info("[{}] Received request to generate Gemini AI trading signal (timeframe: {}, risk: {})", 
                requestId, timeframe, riskProfile);
        
        // Validate timeframe
        if (!VALID_TIMEFRAMES.contains(timeframe)) {
            log.error("[{}] Invalid timeframe requested: {}", requestId, timeframe);
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Invalid Timeframe",
                "message", "Supported timeframes: " + String.join(", ", VALID_TIMEFRAMES),
                "timestamp", LocalDateTime.now().toString()
            ));
        }
        
        // Validate risk profile
        if (!VALID_RISK_PROFILES.contains(riskProfile)) {
            log.error("[{}] Invalid risk profile requested: {}", requestId, riskProfile);
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Invalid Risk Profile",
                "message", "Supported risk profiles: " + String.join(", ", VALID_RISK_PROFILES),
                "timestamp", LocalDateTime.now().toString()
            ));
        }
        
        try {
            // Get the user email from the request header or parameter
            String userId = httpRequest.getHeader("X-User-Email");
            if (userId == null || userId.isEmpty()) {
                userId = httpRequest.getParameter("email");
                
                if (userId == null || userId.isEmpty()) {
                    log.error("[{}] No user email provided", requestId);
                    return ResponseEntity.badRequest().body(Map.of(
                        "error", "User Not Found",
                        "message", "User email is required to access trading signals",
                        "timestamp", LocalDateTime.now().toString()
                    ));
                }
            }
            
            log.info("[{}] Generating Gemini AI trading signal for user: {}", requestId, userId);
            
            // Get Coinbase API credentials from Firestore
            Map<String, String> credentials = firestoreService.getCoinbaseCredentials(userId);
            
            if (credentials == null || credentials.isEmpty()) {
                log.warn("[{}] Coinbase API credentials not found for user: {}", requestId, userId);
                return ResponseEntity.ok(Map.of(
                    "error", "Credentials Not Found",
                    "message", "Coinbase API credentials not configured. Please set up your Coinbase API credentials first.",
                    "timestamp", LocalDateTime.now().toString(),
                    "userEmail", userId
                ));
            }
            
            String apiKey = credentials.get("apiKey");
            String privateKey = credentials.get("privateKey");
            String passphrase = credentials.get("passphrase");
            
            // Check if we have valid credentials including passphrase
            if (apiKey == null || apiKey.isEmpty() || privateKey == null || privateKey.isEmpty() 
                    || passphrase == null || passphrase.isEmpty()) {
                log.warn("[{}] Coinbase API credentials incomplete for user: {}", requestId, userId);
                return ResponseEntity.ok(Map.of(
                    "error", "Incomplete Credentials",
                    "message", "Coinbase API credentials incomplete. Make sure you have API key, private key, and passphrase configured.",
                    "timestamp", LocalDateTime.now().toString(),
                    "userEmail", userId
                ));
            }
            
            // Generate AI trading signal using real Coinbase data with Gemini
            GeminiTradingSignal signal = geminiTradingAgent.generateTradingSignal(
                    apiKey, privateKey, passphrase, timeframe, riskProfile);
            
            // Prepare the response
            Map<String, Object> response = new HashMap<>();
            response.put("signal", signal.getSignal());
            response.put("confidence", signal.getConfidence());
            response.put("currentPrice", signal.getCurrentPrice());
            response.put("targetPrice", signal.getTargetPrice());
            response.put("stopLoss", signal.getStopLoss());
            response.put("timeframe", signal.getTimeframe());
            response.put("rationale", signal.getRationale());
            response.put("keyIndicators", signal.getKeyIndicators());
            response.put("timestamp", signal.getTimestamp().toString());
            response.put("formattedSignal", signal.getFormattedSignal());
            response.put("requestId", requestId);
            
            log.info("[{}] Successfully generated Gemini AI trading signal for user: {}", requestId, userId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("[{}] Error generating Gemini AI trading signal: {}", requestId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Internal Server Error",
                "message", "Error generating AI trading signal: " + e.getMessage(),
                "timestamp", LocalDateTime.now().toString(),
                "requestId", requestId
            ));
        }
    }
    
    /**
     * Get available timeframes and risk profiles for AI trading signal analysis
     * 
     * @return Available parameters for AI trading signals
     */
    @GetMapping("/parameters")
    @CrossOrigin(origins = {"http://localhost:3000", "https://strategiz.io"}, allowedHeaders = "*")
    public ResponseEntity<Map<String, Object>> getAvailableParameters() {
        Map<String, Object> response = new HashMap<>();
        
        response.put("timeframes", VALID_TIMEFRAMES);
        response.put("defaultTimeframe", "1d");
        
        response.put("riskProfiles", VALID_RISK_PROFILES);
        response.put("defaultRiskProfile", "moderate");
        
        response.put("description", "AI trading agent using Gemini with real Coinbase data");
        response.put("provider", "Google Vertex AI");
        response.put("modelName", "gemini-pro");
        response.put("timestamp", LocalDateTime.now().toString());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Health check endpoint for Gemini AI trading agent
     * 
     * @return Health status
     */
    @GetMapping("/health")
    @CrossOrigin(origins = {"http://localhost:3000", "https://strategiz.io"}, allowedHeaders = "*")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        log.info("Health check for Gemini AI trading agent");
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "up");
        response.put("service", "Gemini AI Trading Agent");
        response.put("implementation", "Google Vertex AI with Agent Development Kit (ADK)");
        response.put("timestamp", LocalDateTime.now().toString());
        
        return ResponseEntity.ok(response);
    }
}
