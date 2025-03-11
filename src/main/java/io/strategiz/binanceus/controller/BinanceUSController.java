package io.strategiz.binanceus.controller;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import io.strategiz.framework.rest.controller.BaseServiceRestController;
import io.strategiz.binanceus.model.Account;
import io.strategiz.binanceus.model.Balance;
import io.strategiz.binanceus.model.request.BalanceRequest;
import io.strategiz.binanceus.model.response.BalanceResponse;
import io.strategiz.binanceus.model.response.ExchangeInfoResponse;
import io.strategiz.binanceus.model.response.StatusResponse;
import io.strategiz.binanceus.model.response.TickerPricesResponse;
import io.strategiz.binanceus.service.BinanceUSService;
import io.strategiz.binanceus.service.FirestoreService;
import io.strategiz.binanceus.util.AdminUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller for Binance US API integration
 * Extends BaseServiceRestController from our custom framework
 */
@RestController
@RequestMapping("/api/binanceus")
public class BinanceUSController extends BaseServiceRestController {

    private static final Logger log = LoggerFactory.getLogger(BinanceUSController.class);

    @Autowired
    private BinanceUSService binanceUSService;

    @Autowired
    private FirestoreService firestoreService;

    @GetMapping("/status")
    public ResponseEntity<StatusResponse> getStatus() {
        StatusResponse response = new StatusResponse();
        response.setStatus("success");
        response.setMessage("Binance US API is operational");
        response.setTimestamp(System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/test-connection")
    public ResponseEntity<StatusResponse> testConnection(@RequestHeader("Authorization") String authHeader, @RequestBody Map<String, String> credentials) {
        try {
            String idToken = authHeader.replace("Bearer ", "");
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            String userId = decodedToken.getUid();

            String apiKey = credentials.get("apiKey");
            String secretKey = credentials.get("secretKey");

            if (apiKey == null || secretKey == null) {
                return ResponseEntity.badRequest().body(StatusResponse.error("API key and secret key are required"));
            }

            Map<String, Object> result = binanceUSService.testConnection(apiKey, secretKey);
            if (result != null && "ok".equals(result.get("status"))) {
                // Save credentials to Firestore if test is successful
                firestoreService.saveExchangeCredentials(userId, "binanceus", apiKey, secretKey);
                return ResponseEntity.ok(StatusResponse.success("Connection successful"));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(StatusResponse.error("Invalid API credentials"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(StatusResponse.error("Error testing connection: " + e.getMessage()));
        }
    }

    @GetMapping("/exchange-info")
    public ResponseEntity<ExchangeInfoResponse> getExchangeInfo() {
        try {
            Object exchangeInfo = binanceUSService.getExchangeInfo();
            ExchangeInfoResponse response = new ExchangeInfoResponse();
            response.setExchangeInfo(exchangeInfo);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/ticker/prices")
    public ResponseEntity<TickerPricesResponse> getTickerPrices() {
        try {
            TickerPricesResponse response = new TickerPricesResponse();
            response.setTickerPrices(binanceUSService.getTickerPrices());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/balance/{userId}")
    public ResponseEntity<BalanceResponse> getBalanceByUserId(@PathVariable String userId, @RequestHeader("Authorization") String authHeader) {
        try {
            String idToken = authHeader.replace("Bearer ", "");
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            String requestingUserId = decodedToken.getUid();

            // Check if the requesting user is the same as the requested user or is an admin
            if (!requestingUserId.equals(userId) && !AdminUtil.isAdmin(decodedToken)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
            }

            Map<String, String> credentials = firestoreService.getExchangeCredentials(userId, "binanceus");
            if (credentials == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            String apiKey = credentials.get("apiKey");
            String secretKey = credentials.get("secretKey");

            if (apiKey == null || secretKey == null) {
                return ResponseEntity.badRequest().body(null);
            }

            BalanceResponse response = new BalanceResponse();
            Account rawAccount = binanceUSService.getAccount(apiKey, secretKey);
            response.setRawAccountData(rawAccount); // Store the complete raw data
            List<Balance> balances = binanceUSService.getAccountBalance(apiKey, secretKey);
            response.setPositions(balances);
            response.setTotalUsdValue(binanceUSService.calculateTotalUsdValue(balances));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/balance")
    public ResponseEntity<BalanceResponse> getBalance(@RequestBody BalanceRequest request) {
        try {
            String apiKey = request.getApiKey();
            String secretKey = request.getSecretKey();

            if (apiKey == null || secretKey == null) {
                return ResponseEntity.badRequest().body(null);
            }

            BalanceResponse response = new BalanceResponse();
            Account rawAccount = binanceUSService.getAccount(apiKey, secretKey);
            response.setRawAccountData(rawAccount); // Store the complete raw data
            List<Balance> balances = binanceUSService.getAccountBalance(apiKey, secretKey);
            response.setPositions(balances);
            response.setTotalUsdValue(binanceUSService.calculateTotalUsdValue(balances));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * Get raw account data from Binance US API
     * This endpoint returns the completely unmodified raw data from Binance US API
     * 
     * @param request Request containing API key and secret key
     * @return Raw account data from Binance US API
     */
    @PostMapping("/raw-data")
    @CrossOrigin(origins = {"http://localhost:3000", "https://strategiz.io"}, allowedHeaders = "*")
    public ResponseEntity<Object> getRawAccountData(@RequestBody Map<String, String> request) {
        try {
            log.info("Received request for raw Binance US data");
            
            // For admin page, we'll simply use the provided API keys directly
            // This ensures we get the completely unmodified raw data from Binance US API
            String apiKey = request.get("apiKey");
            String secretKey = request.get("secretKey");

            if (apiKey == null || secretKey == null) {
                log.warn("Missing API key or secret key in request");
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "API Key and Secret Key are required"
                ));
            }

            log.info("Fetching raw account data from Binance US API");
            
            try {
                // Get the complete unmodified raw data from the Binance US API
                Account rawAccount = binanceUSService.getAccount(apiKey, secretKey);
                
                log.info("Successfully retrieved raw account data from Binance US API with {} balances", 
                    rawAccount.getBalances() != null ? rawAccount.getBalances().size() : 0);
                
                // Return the completely unmodified raw data directly
                // This ensures the frontend gets exactly what comes from the Binance US API
                return ResponseEntity.ok(rawAccount);
            } catch (Exception e) {
                log.error("Error from Binance US API: {}", e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                    "status", "error",
                    "message", "Error from Binance US API: " + e.getMessage(),
                    "error", e.getClass().getSimpleName()
                ));
            }
        } catch (Exception e) {
            log.error("Error processing request: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }
}
