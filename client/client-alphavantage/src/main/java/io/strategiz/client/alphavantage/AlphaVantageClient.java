package io.strategiz.client.alphavantage;

import io.strategiz.framework.exception.ApplicationClientException;
import io.strategiz.client.alphavantage.model.StockData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

/**
 * Client for interacting with the AlphaVantage API.
 * This class handles all low-level API communication with AlphaVantage following Synapse patterns.
 */
@Component
public class AlphaVantageClient {
    
    private static final Logger log = LoggerFactory.getLogger(AlphaVantageClient.class);
    private static final String QUOTE_ENDPOINT = "/query";
    private static final String SEARCH_ENDPOINT = "/query";
    
    @Value("${alphavantage.api.url:https://www.alphavantage.co}")
    private String baseUrl;
    
    @Value("${alphavantage.api.key:demo}")
    private String apiKey;
    
    private final RestTemplate restTemplate;
    
    /**
     * Constructor with dependency injection for RestTemplate
     * 
     * @param alphaVantageRestTemplate RestTemplate configured for AlphaVantage API
     */
    public AlphaVantageClient(@Qualifier("alphaVantageRestTemplate") RestTemplate alphaVantageRestTemplate) {
        this.restTemplate = alphaVantageRestTemplate;
        log.info("AlphaVantageClient initialized with base URL: {}", baseUrl);
    }
    
    /**
     * Get stock quote data for a given symbol
     * 
     * @param symbol Stock symbol (e.g. "MSFT", "AAPL")
     * @return StockData object with quote data
     */
    @Cacheable(value = "stockQuotes", key = "#symbol")
    public StockData getStockQuote(String symbol) {
        log.info("Fetching stock quote for symbol: {}", symbol);
        
        try {
            // Build URL with query parameters
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl + QUOTE_ENDPOINT)
                    .queryParam("function", "GLOBAL_QUOTE")
                    .queryParam("symbol", symbol)
                    .queryParam("apikey", apiKey);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            
            HttpEntity<?> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    builder.toUriString(),
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            if (response.getBody() == null || !response.getBody().containsKey("Global Quote")) {
                log.error("Invalid response format from AlphaVantage API for symbol: {}", symbol);
                throw new ApplicationClientException("Invalid response format from AlphaVantage API");
            }
            
            return parseGlobalQuoteResponse(response.getBody(), symbol);
        } catch (RestClientException e) {
            log.error("Error fetching stock quote from AlphaVantage API: {}", e.getMessage(), e);
            throw new ApplicationClientException("Failed to retrieve stock quote data", e);
        }
    }
    
    /**
     * Get stock data for multiple symbols in a batch request
     * 
     * @param symbols List of stock symbols (e.g. ["MSFT", "AAPL"])
     * @return Map of symbol to StockData
     */
    @Cacheable(value = "batchStockQuotes", key = "#symbols.toString()")
    public Map<String, StockData> getBatchStockQuotes(List<String> symbols) {
        log.info("Fetching batch stock quotes for symbols: {}", symbols);
        
        Map<String, StockData> result = new HashMap<>();
        
        // AlphaVantage doesn't have a true batch API, so we need to make multiple requests
        // Using parallel streams to speed up the process
        symbols.parallelStream().forEach(symbol -> {
            try {
                StockData stockData = getStockQuote(symbol);
                result.put(symbol, stockData);
            } catch (Exception e) {
                log.error("Error fetching stock quote for symbol {}: {}", symbol, e.getMessage());
                // Continue with other symbols even if one fails
            }
        });
        
        return result;
    }
    
    /**
     * Search for stocks by keywords
     * 
     * @param keywords Search keywords (e.g. "Microsoft")
     * @return List of search results
     */
    @Cacheable(value = "stockSearch", key = "#keywords")
    public List<Map<String, Object>> searchStocks(String keywords) {
        log.info("Searching for stocks with keywords: {}", keywords);
        
        try {
            // Build URL with query parameters
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl + SEARCH_ENDPOINT)
                    .queryParam("function", "SYMBOL_SEARCH")
                    .queryParam("keywords", keywords)
                    .queryParam("apikey", apiKey);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            
            HttpEntity<?> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    builder.toUriString(),
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            if (response.getBody() == null || !response.getBody().containsKey("bestMatches")) {
                log.error("Invalid response format from AlphaVantage search API");
                return Collections.emptyList();
            }
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> matches = (List<Map<String, Object>>) response.getBody().get("bestMatches");
            
            // Return the results (already limited by the API)
            return matches;
        } catch (RestClientException e) {
            log.error("Error searching stocks from AlphaVantage API: {}", e.getMessage(), e);
            throw new ApplicationClientException("Failed to search stocks", e);
        }
    }
    
    /**
     * Helper method to parse the Global Quote response from AlphaVantage
     * 
     * @param responseMap Response map from AlphaVantage API
     * @param symbol Stock symbol
     * @return Parsed StockData object
     */
    @SuppressWarnings("unchecked")
    private StockData parseGlobalQuoteResponse(Map<String, Object> responseMap, String symbol) {
        Map<String, String> quote = (Map<String, String>) responseMap.get("Global Quote");
        
        StockData stockData = new StockData();
        stockData.setSymbol(symbol);
        
        // Get the company name from a different API call or set it to the symbol for now
        stockData.setName(symbol);
        
        // Parse price and changes
        try {
            stockData.setPrice(parseDecimal(quote.get("05. price")));
            stockData.setChange(parseDecimal(quote.get("09. change")));
            
            String changePercentStr = quote.get("10. change percent");
            if (changePercentStr != null && changePercentStr.endsWith("%")) {
                changePercentStr = changePercentStr.substring(0, changePercentStr.length() - 1);
            }
            stockData.setChangePercent(parseDecimal(changePercentStr));
            
            stockData.setVolume(parseDecimal(quote.get("06. volume")));
            stockData.setLastUpdated(quote.get("07. latest trading day"));
        } catch (Exception e) {
            log.error("Error parsing AlphaVantage response: {}", e.getMessage(), e);
        }
        
        return stockData;
    }
    
    /**
     * Helper method to safely parse a decimal value
     * 
     * @param value String value to parse
     * @return Parsed BigDecimal or null if parsing fails
     */
    private java.math.BigDecimal parseDecimal(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        
        try {
            return new java.math.BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            log.error("Error parsing decimal value: {}", value);
            return null;
        }
    }
}
