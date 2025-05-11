package io.strategiz.coinbase.admindashboard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coinbase-admin-dashboard")
@CrossOrigin(origins = {"http://localhost:3000", "https://strategiz.io"}, allowedHeaders = "*")
public class CoinbaseAdminDashboardController {
    @Autowired
    private CoinbaseAdminDashboardService service;

    @GetMapping("/raw-account-data")
    public ResponseEntity<String> getRawAccountData(@RequestParam String email) {
        return service.getRawAccountData(email);
    }

    @GetMapping("/portfolio-data")
    public ResponseEntity<String> getPortfolioData(@RequestParam String email) {
        return service.getPortfolioData(email);
    }

    @GetMapping("/check-credentials")
    public ResponseEntity<String> checkCredentials(@RequestParam String email) {
        return service.checkCredentials(email);
    }
}
