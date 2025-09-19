package io.strategiz.client.yahoofinance.parser;

import io.strategiz.client.yahoofinance.model.PriceQuote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses Yahoo Finance API responses into domain objects.
 * Single Responsibility: Response parsing and data extraction.
 */
@Component
public class ResponseParser {
    
    private static final Logger log = LoggerFactory.getLogger(ResponseParser.class);
    
    /**
     * Parse v10 API response to extract price quotes.
     * 
     * @param response Raw API response
     * @return Map of symbol to PriceQuote
     */
    public Map<String, PriceQuote> parseV10Response(Map<String, Object> response) {
        Map<String, PriceQuote> quotes = new HashMap<>();
        
        try {
            Map<String, Object> quoteSummary = (Map<String, Object>) response.get("quoteSummary");
            if (quoteSummary == null) {
                log.warn("No quoteSummary in v10 response");
                return quotes;
            }
            
            List<Map<String, Object>> results = (List<Map<String, Object>>) quoteSummary.get("result");
            if (results == null || results.isEmpty()) {
                log.warn("No results in v10 quoteSummary");
                return quotes;
            }
            
            for (Map<String, Object> result : results) {
                PriceQuote quote = parseV10Quote(result);
                if (quote != null) {
                    quotes.put(quote.getSymbol(), quote);
                }
            }
            
        } catch (Exception e) {
            log.error("Error parsing v10 response: {}", e.getMessage());
        }
        
        return quotes;
    }
    
    /**
     * Parse v8 API response to extract price quotes.
     * 
     * @param response Raw API response
     * @return Map of symbol to PriceQuote
     */
    public Map<String, PriceQuote> parseV8Response(Map<String, Object> response) {
        Map<String, PriceQuote> quotes = new HashMap<>();
        
        try {
            Map<String, Object> quoteResponse = (Map<String, Object>) response.get("quoteResponse");
            if (quoteResponse == null) {
                log.warn("No quoteResponse in v8 response");
                return quotes;
            }
            
            List<Map<String, Object>> results = (List<Map<String, Object>>) quoteResponse.get("result");
            if (results == null || results.isEmpty()) {
                log.warn("No results in v8 quoteResponse");
                return quotes;
            }
            
            for (Map<String, Object> result : results) {
                PriceQuote quote = parseV8Quote(result);
                if (quote != null) {
                    quotes.put(quote.getSymbol(), quote);
                }
            }
            
        } catch (Exception e) {
            log.error("Error parsing v8 response: {}", e.getMessage());
        }
        
        return quotes;
    }
    
    /**
     * Parse individual v10 quote.
     */
    private PriceQuote parseV10Quote(Map<String, Object> result) {
        try {
            Map<String, Object> price = (Map<String, Object>) result.get("price");
            if (price == null) {
                return null;
            }
            
            String symbol = extractString(price, "symbol");
            if (symbol == null || symbol.isEmpty()) {
                return null;
            }
            
            BigDecimal regularMarketPrice = extractPrice(price, "regularMarketPrice");
            if (regularMarketPrice == null || regularMarketPrice.compareTo(BigDecimal.ZERO) == 0) {
                // Try postMarket or preMarket prices
                regularMarketPrice = extractPrice(price, "postMarketPrice");
                if (regularMarketPrice == null || regularMarketPrice.compareTo(BigDecimal.ZERO) == 0) {
                    regularMarketPrice = extractPrice(price, "preMarketPrice");
                }
            }
            
            if (regularMarketPrice == null) {
                log.debug("No valid price found for symbol: {}", symbol);
                return null;
            }
            
            return new PriceQuote.Builder()
                .symbol(symbol)
                .price(regularMarketPrice)
                .previousClose(extractPrice(price, "regularMarketPreviousClose"))
                .dayChange(extractPrice(price, "regularMarketChange"))
                .dayChangePercent(extractPrice(price, "regularMarketChangePercent"))
                .dayHigh(extractPrice(price, "regularMarketDayHigh"))
                .dayLow(extractPrice(price, "regularMarketDayLow"))
                .volume(extractLong(price, "regularMarketVolume"))
                .build();
                
        } catch (Exception e) {
            log.error("Error parsing v10 quote: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Parse individual v8 quote.
     */
    private PriceQuote parseV8Quote(Map<String, Object> result) {
        try {
            String symbol = extractString(result, "symbol");
            if (symbol == null || symbol.isEmpty()) {
                return null;
            }
            
            BigDecimal regularMarketPrice = extractDirectPrice(result, "regularMarketPrice");
            if (regularMarketPrice == null || regularMarketPrice.compareTo(BigDecimal.ZERO) == 0) {
                regularMarketPrice = extractDirectPrice(result, "price");
            }
            
            if (regularMarketPrice == null) {
                log.debug("No valid price found for symbol: {}", symbol);
                return null;
            }
            
            return new PriceQuote.Builder()
                .symbol(symbol)
                .price(regularMarketPrice)
                .previousClose(extractDirectPrice(result, "regularMarketPreviousClose"))
                .dayChange(extractDirectPrice(result, "regularMarketChange"))
                .dayChangePercent(extractDirectPrice(result, "regularMarketChangePercent"))
                .dayHigh(extractDirectPrice(result, "regularMarketDayHigh"))
                .dayLow(extractDirectPrice(result, "regularMarketDayLow"))
                .volume(extractDirectLong(result, "regularMarketVolume"))
                .build();
                
        } catch (Exception e) {
            log.error("Error parsing v8 quote: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Extract price from nested structure (v10 format).
     */
    private BigDecimal extractPrice(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof Map) {
            Map<String, Object> priceMap = (Map<String, Object>) value;
            Object raw = priceMap.get("raw");
            if (raw != null) {
                return toBigDecimal(raw);
            }
        }
        return null;
    }
    
    /**
     * Extract price directly (v8 format).
     */
    private BigDecimal extractDirectPrice(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return toBigDecimal(value);
    }
    
    /**
     * Extract long from nested structure.
     */
    private Long extractLong(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof Map) {
            Map<String, Object> valueMap = (Map<String, Object>) value;
            Object raw = valueMap.get("raw");
            if (raw != null) {
                return toLong(raw);
            }
        }
        return null;
    }
    
    /**
     * Extract long directly.
     */
    private Long extractDirectLong(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return toLong(value);
    }
    
    /**
     * Extract string value.
     */
    private String extractString(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value != null ? value.toString() : null;
    }
    
    /**
     * Convert to BigDecimal safely.
     */
    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        try {
            if (value instanceof Number) {
                return new BigDecimal(value.toString());
            } else if (value instanceof String) {
                return new BigDecimal((String) value);
            }
        } catch (Exception e) {
            log.debug("Cannot convert {} to BigDecimal", value);
        }
        return null;
    }
    
    /**
     * Convert to Long safely.
     */
    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        try {
            if (value instanceof Number) {
                return ((Number) value).longValue();
            } else if (value instanceof String) {
                return Long.parseLong((String) value);
            }
        } catch (Exception e) {
            log.debug("Cannot convert {} to Long", value);
        }
        return null;
    }
}