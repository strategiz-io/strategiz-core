package io.strategiz.client.yahoofinance;

import io.strategiz.client.yahoofinance.model.YahooStockData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for Yahoo Finance Client
 * Note: These tests hit the real Yahoo Finance API
 */
class YahooFinanceClientTest {
    
    private YahooFinanceClient yahooFinanceClient;
    private RestTemplate restTemplate;
    
    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        yahooFinanceClient = new YahooFinanceClient(restTemplate);
    }
    
    @Test
    void testGetStockQuote_AAPL() {
        // Test getting Apple stock data
        YahooStockData result = yahooFinanceClient.getStockQuote("AAPL");
        
        assertNotNull(result, "Result should not be null");
        assertEquals("AAPL", result.getSymbol());
        assertNotNull(result.getCurrentPrice(), "Current price should not be null");
        assertTrue(result.getCurrentPrice().compareTo(BigDecimal.ZERO) > 0, "Price should be positive");
        
        System.out.println("AAPL Data: " + result);
    }
    
    @Test
    void testGetBatchStockQuotes() {
        // Test getting all the ticker symbols
        List<String> symbols = Arrays.asList("TSLA", "GOOG", "AAPL", "AMZN", "META", "NVDA", "NFLX", "AXP");
        Map<String, YahooStockData> results = yahooFinanceClient.getBatchStockQuotes(symbols);
        
        assertFalse(results.isEmpty(), "Should return some results");
        
        // Print results for verification
        results.forEach((symbol, data) -> {
            System.out.println(symbol + ": " + data);
        });
    }
    
    @Test 
    void testGetStockQuote_InvalidSymbol() {
        // Test with invalid symbol
        YahooStockData result = yahooFinanceClient.getStockQuote("INVALID123");
        
        // Should return null for invalid symbols
        assertNull(result, "Invalid symbol should return null");
    }
}