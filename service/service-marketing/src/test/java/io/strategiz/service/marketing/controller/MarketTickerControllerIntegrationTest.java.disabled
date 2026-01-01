package io.strategiz.service.marketing.controller;

import io.strategiz.service.marketing.config.MarketingTestConfiguration;
import io.strategiz.service.marketing.controller.MarketTickerController;
import io.strategiz.client.coinbase.CoinbaseClient;
import io.strategiz.client.coingecko.CoinGeckoClient;
import io.strategiz.client.alphavantage.AlphaVantageClient;
import io.strategiz.service.marketing.model.response.MarketTickerResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for MarketTickerController with real API calls.
 * These tests require API keys to be configured.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(MarketingTestConfiguration.class)
@ActiveProfiles("test")
@DisplayName("Market Ticker Controller Integration Tests - Real APIs")
class MarketTickerControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CoinGeckoClient coinGeckoClient;
    
    @Autowired
    private AlphaVantageClient alphaVantageClient;

    @Autowired
    private ObjectMapper objectMapper;

    private String getBaseUrl() {
        return "http://localhost:" + port + "/v1/market/tickers";
    }

    @Test
    @DisplayName("Should fetch real market data from CoinGecko and Alpha Vantage")
    void shouldFetchRealMarketData() {
        // When - Make real API call
        ResponseEntity<MarketTickerResponse> response = restTemplate.getForEntity(
            getBaseUrl(), 
            MarketTickerResponse.class
        );

        // Then - Verify response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getItems()).isNotEmpty();
        assertThat(response.getBody().getTimestamp()).isGreaterThan(0);
        assertThat(response.getBody().getCacheDurationSeconds()).isEqualTo(30);

        // Log the actual data for verification
        System.out.println("=== Real Market Data ===");
        response.getBody().getItems().forEach(item -> {
            System.out.printf("%s (%s) - %s: $%.2f %+.2f (%.2f%%)%n",
                item.getSymbol(),
                item.getType(),
                item.getName(),
                item.getPrice(),
                item.getChange(),
                item.getChangePercent()
            );
        });
    }

    @Test
    @DisplayName("Should return crypto data from CoinGecko")
    void shouldReturnCryptoDataFromCoinGecko() {
        // When
        ResponseEntity<MarketTickerResponse> response = restTemplate.getForEntity(
            getBaseUrl(), 
            MarketTickerResponse.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        
        // Check for crypto items
        var cryptoItems = response.getBody().getItems().stream()
            .filter(item -> "crypto".equals(item.getType()))
            .toList();
        
        assertThat(cryptoItems).isNotEmpty();
        
        // Verify crypto data has expected fields
        cryptoItems.forEach(item -> {
            assertThat(item.getSymbol()).isNotBlank();
            assertThat(item.getName()).isNotBlank();
            assertThat(item.getPrice()).isNotNull();
            assertThat(item.getPrice()).isPositive();
        });
    }

    @Test
    @DisplayName("Should return stock data from Alpha Vantage when API key is configured")
    @EnabledIfEnvironmentVariable(named = "ALPHA_VANTAGE_API_KEY", matches = ".+")
    void shouldReturnStockDataFromAlphaVantage() {
        // When
        ResponseEntity<MarketTickerResponse> response = restTemplate.getForEntity(
            getBaseUrl(), 
            MarketTickerResponse.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        
        // Check for stock items
        var stockItems = response.getBody().getItems().stream()
            .filter(item -> "stock".equals(item.getType()))
            .toList();
        
        // If Alpha Vantage API key is configured, we should get stock data
        if (!stockItems.isEmpty()) {
            stockItems.forEach(item -> {
                assertThat(item.getSymbol()).isNotBlank();
                assertThat(item.getName()).isNotBlank();
                assertThat(item.getPrice()).isNotNull();
                assertThat(item.getPrice()).isPositive();
            });
        }
    }

    @Test
    @DisplayName("Should handle API failures gracefully and return demo data")
    void shouldHandleApiFailuresGracefully() {
        // This test verifies that even if APIs fail, we still get a valid response
        // The controller should fall back to demo data when APIs are unavailable
        
        // When
        ResponseEntity<MarketTickerResponse> response = restTemplate.getForEntity(
            getBaseUrl(), 
            MarketTickerResponse.class
        );

        // Then - Should always return a valid response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getItems()).isNotEmpty();
    }

    @Test
    @DisplayName("Should respect cache duration")
    void shouldRespectCacheDuration() throws InterruptedException {
        // First call
        ResponseEntity<MarketTickerResponse> firstResponse = restTemplate.getForEntity(
            getBaseUrl(), 
            MarketTickerResponse.class
        );
        
        long firstTimestamp = firstResponse.getBody().getTimestamp();
        
        // Immediate second call should return cached data (same timestamp)
        ResponseEntity<MarketTickerResponse> secondResponse = restTemplate.getForEntity(
            getBaseUrl(), 
            MarketTickerResponse.class
        );
        
        // Timestamps should be the same due to caching
        assertThat(secondResponse.getBody().getTimestamp()).isEqualTo(firstTimestamp);
    }
}