package io.strategiz.client.yahoofinance;

import io.strategiz.client.yahoofinance.model.YahooStockData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Client for Yahoo Finance API (unofficial but reliable)
 * Provides unlimited real-time stock data for landing page use
 */
@Component
public class YahooFinanceClient {
    
    private static final Logger log = LoggerFactory.getLogger(YahooFinanceClient.class);
    private static final String BASE_URL = "https://query1.finance.yahoo.com";
    private static final String CHART_ENDPOINT = "/v8/finance/chart/{symbol}";
    
    private final RestTemplate restTemplate;
    
    public YahooFinanceClient(RestTemplate yahooFinanceRestTemplate) {
        this.restTemplate = yahooFinanceRestTemplate;
        log.info("YahooFinanceClient initialized with base URL: {}", BASE_URL);
    }
    
    /**
     * Get stock quote data for a single symbol
     * Cached for 30 seconds to improve performance
     */
    @Cacheable(value = "yahooStockQuotes", key = "#symbol")
    public YahooStockData getStockQuote(String symbol) {
        log.debug("Fetching Yahoo Finance data for symbol: {}", symbol);
        
        try {
            String url = BASE_URL + CHART_ENDPOINT;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            headers.set("User-Agent", "Mozilla/5.0 (compatible; StrategizBot/1.0)");
            
            HttpEntity<?> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {},
                    symbol
            );
            
            if (response.getBody() == null) {
                log.error("Empty response from Yahoo Finance for symbol: {}", symbol);
                return null;
            }
            
            return parseYahooResponse(response.getBody(), symbol);
            
        } catch (RestClientException e) {
            log.error("Error fetching stock data from Yahoo Finance for {}: {}", symbol, e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Unexpected error parsing Yahoo Finance response for {}: {}", symbol, e.getMessage());
            return null;
        }
    }
    
    /**
     * Get stock data for multiple symbols in batch
     * Uses parallel processing for better performance
     */
    public Map<String, YahooStockData> getBatchStockQuotes(List<String> symbols) {
        log.info("Fetching Yahoo Finance data for {} symbols", symbols.size());
        
        Map<String, YahooStockData> result = new HashMap<>();
        
        // Process symbols in parallel for faster loading
        symbols.parallelStream().forEach(symbol -> {
            try {
                YahooStockData data = getStockQuote(symbol);
                if (data != null) {
                    result.put(symbol, data);
                }
            } catch (Exception e) {
                log.warn("Failed to fetch Yahoo Finance data for symbol {}: {}", symbol, e.getMessage());
            }
        });
        
        log.info("Successfully fetched Yahoo Finance data for {}/{} symbols", result.size(), symbols.size());
        return result;
    }
    
    /**
     * Parse Yahoo Finance API response
     */
    @SuppressWarnings("unchecked")
    private YahooStockData parseYahooResponse(Map<String, Object> responseMap, String symbol) {
        try {
            Map<String, Object> chart = (Map<String, Object>) responseMap.get("chart");
            if (chart == null) {
                log.error("No chart data in Yahoo Finance response for {}", symbol);
                return null;
            }
            
            List<Map<String, Object>> result = (List<Map<String, Object>>) chart.get("result");
            if (result == null || result.isEmpty()) {
                log.error("No result data in Yahoo Finance response for {}", symbol);
                return null;
            }
            
            Map<String, Object> stockData = result.get(0);
            Map<String, Object> meta = (Map<String, Object>) stockData.get("meta");
            
            if (meta == null) {
                log.error("No meta data in Yahoo Finance response for {}", symbol);
                return null;
            }
            
            YahooStockData yahooStock = new YahooStockData();
            yahooStock.setSymbol(symbol);
            yahooStock.setName((String) meta.get("longName"));
            
            // Parse current price
            Object currentPriceObj = meta.get("regularMarketPrice");
            if (currentPriceObj != null) {
                yahooStock.setCurrentPrice(new BigDecimal(currentPriceObj.toString()));
            }
            
            // Parse previous close for change calculation
            Object previousCloseObj = meta.get("previousClose");
            if (previousCloseObj != null && currentPriceObj != null) {
                BigDecimal currentPrice = new BigDecimal(currentPriceObj.toString());
                BigDecimal previousClose = new BigDecimal(previousCloseObj.toString());
                
                BigDecimal change = currentPrice.subtract(previousClose);
                yahooStock.setChange(change);
                
                if (previousClose.compareTo(BigDecimal.ZERO) != 0) {
                    BigDecimal changePercent = change.divide(previousClose, 4, BigDecimal.ROUND_HALF_UP)
                            .multiply(new BigDecimal("100"));
                    yahooStock.setChangePercent(changePercent);
                }
            }
            
            // Parse additional fields
            Object volumeObj = meta.get("regularMarketVolume");
            if (volumeObj != null) {
                yahooStock.setVolume(new BigDecimal(volumeObj.toString()));
            }
            
            Object marketStateObj = meta.get("marketState");
            if (marketStateObj != null) {
                yahooStock.setMarketState(marketStateObj.toString());
            }
            
            return yahooStock;
            
        } catch (Exception e) {
            log.error("Error parsing Yahoo Finance response for {}: {}", symbol, e.getMessage(), e);
            return null;
        }
    }
}