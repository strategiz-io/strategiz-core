package io.strategiz.service.marketing.controller;

import io.strategiz.service.marketing.config.MarketingTestConfiguration;
import io.strategiz.service.marketing.controller.MarketTickerController;
import io.strategiz.client.coinbase.CoinbaseClient;
import io.strategiz.client.coingecko.CoinGeckoClient;
import io.strategiz.client.alphavantage.AlphaVantageClient;
import io.strategiz.client.coinbase.model.TickerPrice;
import io.strategiz.client.alphavantage.model.StockData;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.marketing.exception.MarketingErrorDetails;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;

/**
 * Comprehensive integration tests for MarketTickerController
 * Tests market ticker data endpoint for landing page
 */
@WebMvcTest(MarketTickerController.class)
@Import(MarketingTestConfiguration.class)
@ActiveProfiles("test")
@DisplayName("Market Ticker Controller Integration Tests")
class MarketTickerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CoinbaseClient coinbaseClient;
    
    @MockBean
    private CoinGeckoClient coinGeckoClient;
    
    @MockBean
    private AlphaVantageClient alphaVantageClient;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String BASE_URL = "/v1/market/tickers";

    @Nested
    @DisplayName("Get Market Tickers - GET /v1/market/tickers")
    class GetMarketTickersTests {

        @Test
        @DisplayName("Should return market ticker data successfully with real API data")
        void shouldReturnMarketTickerDataSuccessfully() throws Exception {
            // Given - Mock successful API responses
            // Note: CoinbaseClient doesn't have getTickerPrice method implemented yet
            // So we just mock AlphaVantage for stock data
            StockData aaplData = new StockData();
            aaplData.setPrice(new BigDecimal("175.50"));
            aaplData.setName("Apple Inc.");
            aaplData.setChange(new BigDecimal("2.25"));
            aaplData.setChangePercent(new BigDecimal("1.30"));
            when(alphaVantageClient.getStockQuote("AAPL")).thenReturn(aaplData);

            // When & Then
            mockMvc.perform(get(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.items").isArray())
                    .andExpect(jsonPath("$.items").isNotEmpty())
                    .andExpect(jsonPath("$.timestamp").isNumber())
                    .andExpect(jsonPath("$.cacheDurationSeconds").value(30));

            // Verify API clients were called
            verify(alphaVantageClient, atLeastOnce()).getStockQuote(anyString());
        }

        @Test
        @DisplayName("Should return demo data when API clients fail")
        void shouldReturnDemoDataWhenApiClientsFail() throws Exception {
            // Given - Mock API failures
            // Note: CoinbaseClient doesn't have getTickerPrice method implemented yet
            when(alphaVantageClient.getStockQuote(anyString()))
                .thenThrow(new RuntimeException("Alpha Vantage API error"));

            // When & Then
            mockMvc.perform(get(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.items").isArray())
                    .andExpect(jsonPath("$.items").isNotEmpty())
                    .andExpect(jsonPath("$.timestamp").isNumber())
                    .andExpect(jsonPath("$.cacheDurationSeconds").value(30));

            // Verify fallback behavior was triggered
            verify(alphaVantageClient, atLeastOnce()).getStockQuote(anyString());
        }

        @Test
        @DisplayName("Should return demo data structure correctly")
        void shouldReturnDemoDataStructureCorrectly() throws Exception {
            // Given - Mock API failures to trigger demo data
            // Note: CoinbaseClient doesn't have getTickerPrice method implemented yet
            when(alphaVantageClient.getStockQuote(anyString()))
                .thenThrow(new RuntimeException("API unavailable"));

            // When & Then
            mockMvc.perform(get(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.items[0].symbol").exists())
                    .andExpect(jsonPath("$.items[0].name").exists())
                    .andExpect(jsonPath("$.items[0].type").exists())
                    .andExpect(jsonPath("$.items[0].price").exists())
                    .andExpect(jsonPath("$.items[0].change").exists())
                    .andExpect(jsonPath("$.items[0].changePercent").exists())
                    .andExpect(jsonPath("$.items[0].isPositive").exists())
                    // Verify some specific demo data
                    .andExpect(jsonPath("$.items[?(@.symbol == 'BTC')]").exists())
                    .andExpect(jsonPath("$.items[?(@.symbol == 'AAPL')]").exists())
                    .andExpect(jsonPath("$.items[?(@.type == 'crypto')]").exists())
                    .andExpect(jsonPath("$.items[?(@.type == 'stock')]").exists());
        }

        @Test
        @DisplayName("Should handle partial API failures gracefully")
        void shouldHandlePartialApiFailuresGracefully() throws Exception {
            // Given - Stock API fails
            // Note: CoinbaseClient doesn't have getTickerPrice method implemented yet
            when(alphaVantageClient.getStockQuote(anyString()))
                .thenThrow(new RuntimeException("Stock API down"));

            // When & Then
            mockMvc.perform(get(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.items").isArray())
                    .andExpect(jsonPath("$.items").isNotEmpty());

            // Verify some API calls were attempted
            verify(alphaVantageClient, atLeastOnce()).getStockQuote(anyString());
        }

        @Test
        @DisplayName("Should include proper CORS headers for public endpoint")
        void shouldIncludeProperCorsHeaders() throws Exception {
            // Given - Mock API failure to use demo data
            // Note: CoinbaseClient doesn't have getTickerPrice method implemented yet
            when(alphaVantageClient.getStockQuote(anyString()))
                .thenThrow(new RuntimeException("Use demo data"));

            // When & Then
            mockMvc.perform(get(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Origin", "http://localhost:3000"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", "*"));
        }

        @Test
        @DisplayName("Should return consistent response structure")
        void shouldReturnConsistentResponseStructure() throws Exception {
            // Given - Mock successful stock data call
            // Note: CoinbaseClient doesn't have getTickerPrice method implemented yet
            StockData stockData = new StockData();
            stockData.setPrice(new BigDecimal("175.50"));
            stockData.setName("Test Stock");
            stockData.setChange(new BigDecimal("2.25"));
            stockData.setChangePercent(new BigDecimal("1.30"));
            when(alphaVantageClient.getStockQuote(anyString())).thenReturn(stockData);

            // When & Then
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items").exists())
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.cacheDurationSeconds").exists())
                    .andExpect(jsonPath("$.cacheDurationSeconds").value(30));
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle complete API service outage gracefully")
        void shouldHandleCompleteApiServiceOutageGracefully() throws Exception {
            // Given - All APIs are down
            // Note: CoinbaseClient and CoinGeckoClient don't have the expected methods implemented yet
            when(alphaVantageClient.getStockQuote(anyString()))
                .thenThrow(new RuntimeException("Service completely down"));

            // When & Then - Should still return demo data
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items").isArray())
                    .andExpect(jsonPath("$.items").isNotEmpty());
        }

        @Test
        @DisplayName("Should not expose internal API errors to client")
        void shouldNotExposeInternalApiErrorsToClient() throws Exception {
            // Given - API throws detailed internal error
            // Note: CoinbaseClient doesn't have getTickerPrice method implemented yet
            when(alphaVantageClient.getStockQuote(anyString()))
                .thenThrow(new RuntimeException("Internal API key expired: sk-1234567890"));

            // When & Then - Should return generic response without exposing internal details
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items").exists())
                    // Should not contain sensitive information
                    .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("sk-1234567890"))))
                    .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("API key"))));
        }
    }
}