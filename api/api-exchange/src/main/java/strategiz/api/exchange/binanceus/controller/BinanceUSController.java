package strategiz.api.exchange.binanceus.controller;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import strategiz.data.exchange.binanceus.model.Account;
import strategiz.data.exchange.binanceus.model.request.BalanceRequest;
import strategiz.data.exchange.binanceus.model.response.BalanceResponse;
import strategiz.data.exchange.binanceus.model.response.ExchangeInfoResponse;
import strategiz.data.exchange.binanceus.model.response.StatusResponse;
import strategiz.data.exchange.binanceus.model.response.TickerPricesResponse;
import strategiz.service.exchange.binanceus.BinanceUSService;
import strategiz.service.exchange.binanceus.FirestoreService;
import strategiz.service.exchange.binanceus.util.AdminUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Controller for Binance US API integration
 */
@Slf4j
@RestController
@RequestMapping("/api/binanceus")
public class BinanceUSController {

    @Autowired
    private BinanceUSService binanceUSService;

    @Autowired
    private FirestoreService firestoreService;

    @Autowired
    @Qualifier("binanceRestTemplate")
    private RestTemplate binanceRestTemplate;

    /**
     * Test connection to Binance US API with provided credentials
     * 
     * @param authHeader Authorization header containing Firebase ID token
     * @param credentials Map containing API key and secret key
     * @return Status response
     */
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
                firestoreService.saveBinanceUSCredentials(userId, apiKey, secretKey);
                return ResponseEntity.ok(StatusResponse.success("Connection successful"));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(StatusResponse.error("Invalid API credentials"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(StatusResponse.error("Error testing connection: " + e.getMessage()));
        }
    }

    /**
     * Get exchange information from Binance US API
     * 
     * @return Exchange information response
     */
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

    /**
     * Get ticker prices from Binance US API
     * 
     * @return Ticker prices response
     */
    @GetMapping("/ticker/prices")
    public ResponseEntity<TickerPricesResponse> getTickerPrices() {
        try {
            TickerPricesResponse response = new TickerPricesResponse();
            response.setTickerPrices(binanceUSService.getTickerPrices());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting ticker prices", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * Get account balances from Binance US API
     * 
     * @param authHeader Authorization header containing Firebase ID token
     * @return Balance response
     */
    @GetMapping("/balances")
    public ResponseEntity<BalanceResponse> getBalances(@RequestHeader("Authorization") String authHeader) {
        try {
            String idToken = authHeader.replace("Bearer ", "");
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            String userId = decodedToken.getUid();

            Map<String, String> credentials = firestoreService.getBinanceUSCredentials(userId);
            if (credentials == null || credentials.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            }

            String apiKey = credentials.get("apiKey");
            String secretKey = credentials.get("secretKey");

            if (apiKey == null || secretKey == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            }

            BalanceResponse response = binanceUSService.getBalances(apiKey, secretKey);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting balances", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * Get account balances from Binance US API with provided credentials
     * 
     * @param request Balance request containing API credentials
     * @return Balance response
     */
    @PostMapping("/balances")
    public ResponseEntity<BalanceResponse> getBalancesWithCredentials(@RequestBody BalanceRequest request) {
        try {
            String apiKey = request.getApiKey();
            String secretKey = request.getSecretKey();

            if (apiKey == null || secretKey == null) {
                return ResponseEntity.badRequest().body(null);
            }

            BalanceResponse response = binanceUSService.getBalances(apiKey, secretKey);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting balances with credentials", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * Get raw account data from Binance US API
     * 
     * @param authHeader Authorization header containing Firebase ID token
     * @return Raw account data
     */
    @GetMapping("/raw-account-data")
    public ResponseEntity<?> getRawAccountData(@RequestHeader("Authorization") String authHeader) {
        try {
            String idToken = authHeader.replace("Bearer ", "");
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            String userId = decodedToken.getUid();

            // Check if user is admin
            if (!AdminUtil.isAdmin(decodedToken)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "status", "error",
                    "message", "Admin access required"
                ));
            }

            Map<String, String> credentials = firestoreService.getBinanceUSCredentials(userId);
            if (credentials == null || credentials.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "status", "error",
                    "message", "API credentials not found"
                ));
            }

            String apiKey = credentials.get("apiKey");
            String secretKey = credentials.get("secretKey");

            if (apiKey == null || secretKey == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "status", "error",
                    "message", "API credentials are incomplete"
                ));
            }

            Account account = binanceUSService.getAccount(apiKey, secretKey);
            return ResponseEntity.ok(account);
        } catch (Exception e) {
            log.error("Error getting raw account data", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error: " + e.getMessage()
            ));
        }
    }
}
