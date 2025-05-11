package io.strategiz.coinbase.advanced;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coinbase-advanced")
@CrossOrigin(origins = {"http://localhost:3000", "https://strategiz.io"}, allowedHeaders = "*")
public class CoinbaseAdvancedTradeController {
    @Autowired
    private CoinbaseAdvancedTradeService service;

    @GetMapping("/raw-account-data")
    public ResponseEntity<String> getRawAccountData(@RequestParam String email) {
        return service.getRawAccountData(email);
    }
}
