package io.strategiz.api.monitoring.health;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import io.strategiz.service.exchange.coinbase.CoinbaseService;
import io.strategiz.service.exchange.kraken.KrakenService;
import io.strategiz.service.exchange.binanceus.BinanceUSService;

/**
 * Health indicator for financial provider API integrations
 * Monitors connectivity to Coinbase, Kraken, and Binance US APIs
 */
@Component
public class ProviderApiHealthIndicator implements HealthIndicator {

    private final CoinbaseService coinbaseService;
    private final KrakenService krakenService;
    private final BinanceUSService binanceUSService;

    @Autowired
    public ProviderApiHealthIndicator(
            CoinbaseService coinbaseService,
            KrakenService krakenService,
            BinanceUSService binanceUSService) {
        this.coinbaseService = coinbaseService;
        this.krakenService = krakenService;
        this.binanceUSService = binanceUSService;
    }

    @Override
    public Health health() {
        boolean coinbaseHealthy = checkCoinbaseHealth();
        boolean krakenHealthy = checkKrakenHealth();
        boolean binanceHealthy = checkBinanceHealth();
        
        Health.Builder builder = Health.up()
                .withDetail("coinbase", coinbaseHealthy ? "UP" : "DOWN")
                .withDetail("kraken", krakenHealthy ? "UP" : "DOWN")
                .withDetail("binanceus", binanceHealthy ? "UP" : "DOWN");
        
        // If any of the provider APIs are down, mark the overall status as DOWN
        if (!coinbaseHealthy || !krakenHealthy || !binanceHealthy) {
            builder = Health.down();
        }
        
        return builder.build();
    }
    
    private boolean checkCoinbaseHealth() {
        try {
            // Check if Coinbase API is accessible
            // This uses the real API, not mock data
            return coinbaseService.isApiAvailable();
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean checkKrakenHealth() {
        try {
            // Check if Kraken API is accessible
            return krakenService.isApiAvailable();
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean checkBinanceHealth() {
        try {
            // Check if Binance US API is accessible
            // This uses the real API, not mock data
            return binanceUSService.isApiAvailable();
        } catch (Exception e) {
            return false;
        }
    }
}
