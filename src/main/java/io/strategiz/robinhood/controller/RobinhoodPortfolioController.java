package io.strategiz.robinhood.controller;

import io.strategiz.robinhood.service.RobinhoodService;
import io.strategiz.robinhood.service.FirestoreService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for Robinhood portfolio-specific endpoints
 * Handles all data processing for the Robinhood portfolio page
 */
@Slf4j
@RestController
@RequestMapping("/api/robinhood/portfolio")
public class RobinhoodPortfolioController {

    private final RobinhoodService robinhoodService;
    private final FirestoreService firestoreService;

    @Autowired
    public RobinhoodPortfolioController(RobinhoodService robinhoodService, FirestoreService firestoreService) {
        this.robinhoodService = robinhoodService;
        this.firestoreService = firestoreService;
    }

    /**
     * Get portfolio data for the Robinhood portfolio page
     * This endpoint:
     * 1. Fetches raw data from Robinhood API using credentials from Firestore
     * 2. Parses and processes the data into a standardized format (positions, balances, etc)
     * 3. Returns processed data ready for display
     *
     * @param email User email for retrieving API credentials
     * @return Processed portfolio data with all necessary metrics
     */
    @GetMapping("/data")
    public ResponseEntity<Object> getPortfolioData(@RequestParam String email) {
        log.info("Getting Robinhood portfolio data for user: {}", email);
        try {
            // Retrieve user's Robinhood credentials from Firestore
            Map<String, String> credentials = firestoreService.getRobinhoodCredentials(email);

            if (credentials == null || credentials.isEmpty() ||
                !credentials.containsKey("username") || !credentials.containsKey("password")) {
                log.warn("No valid Robinhood credentials found for user: {}", email);
                return ResponseEntity.ok(Map.of(
                        "error", "No valid Robinhood credentials found"
                ));
            }

            String username = credentials.get("username");
            String password = credentials.get("password");

            // Fetch raw account data from Robinhood
            Object rawAccountData = robinhoodService.getRawAccountData(username, password);

            // TODO: Parse and process the raw data into a standardized portfolio format for the portfolio page
            // This should extract stocks, crypto, balances, and positions into a structure like:
            // {
            //    "positions": [...],
            //    "balances": {...},
            //    ...
            // }
            Map<String, Object> processedData = Map.of(
                    "rawData", rawAccountData
                    // TODO: Add parsed positions, balances, etc. for frontend consumption
            );

            return ResponseEntity.ok(processedData);
        } catch (Exception e) {
            log.error("Error getting Robinhood portfolio data", e);
            return ResponseEntity.ok(Map.of(
                    "error", "Error retrieving Robinhood portfolio data: " + e.getMessage()
            ));
        }
    }
}
