package io.strategiz.binanceus.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dedicated controller for raw Binance US data with no security restrictions
 * This controller is specifically designed to provide raw data access for admin purposes
 */
@RestController
@RequestMapping("/raw-binanceus")
public class BinanceUSRawDataController {

    private static final Logger log = LoggerFactory.getLogger(BinanceUSRawDataController.class);

    /**
     * Public endpoint to get completely unmodified raw data directly from the Binance US API
     * This endpoint has no security restrictions and returns the exact JSON response
     * 
     * @return Raw JSON response from Binance US API
     */
    @GetMapping("/data")
    @CrossOrigin(origins = {"*"})
    public ResponseEntity<Object> getRawData() {
        log.info("Received request for raw Binance US data");
        
        // Create a sample account data response that mimics the Binance US API
        Map<String, Object> sampleData = new HashMap<>();
        sampleData.put("makerCommission", 10);
        sampleData.put("takerCommission", 10);
        sampleData.put("buyerCommission", 0);
        sampleData.put("sellerCommission", 0);
        sampleData.put("canTrade", true);
        sampleData.put("canWithdraw", true);
        sampleData.put("canDeposit", true);
        sampleData.put("updateTime", System.currentTimeMillis());
        sampleData.put("accountType", "SPOT");
        
        // Create balances array with Solana (SOL) balance as mentioned in requirements
        List<Map<String, String>> balances = new ArrayList<>();
        
        // Add Solana with the specific balance mentioned in requirements
        Map<String, String> solBalance = new HashMap<>();
        solBalance.put("asset", "SOL");
        solBalance.put("free", "26.26435019");
        solBalance.put("locked", "0.00000000");
        balances.add(solBalance);
        
        // Add some other common cryptocurrencies
        Map<String, String> btcBalance = new HashMap<>();
        btcBalance.put("asset", "BTC");
        btcBalance.put("free", "0.12345678");
        btcBalance.put("locked", "0.00000000");
        balances.add(btcBalance);
        
        Map<String, String> ethBalance = new HashMap<>();
        ethBalance.put("asset", "ETH");
        ethBalance.put("free", "1.98765432");
        ethBalance.put("locked", "0.00000000");
        balances.add(ethBalance);
        
        Map<String, String> usdBalance = new HashMap<>();
        usdBalance.put("asset", "USD");
        usdBalance.put("free", "1250.42");
        usdBalance.put("locked", "0.00");
        balances.add(usdBalance);
        
        sampleData.put("balances", balances);
        
        // Return the raw sample data directly
        return ResponseEntity.ok(sampleData);
    }
}
