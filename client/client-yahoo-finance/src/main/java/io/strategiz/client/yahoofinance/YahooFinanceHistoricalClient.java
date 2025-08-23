package io.strategiz.client.yahoofinance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Yahoo Finance Historical Data Client
 * Downloads bulk historical OHLCV data for backtesting
 * 
 * FREE & UNLIMITED - No API key required!
 * Can download years of daily/weekly/monthly data in one call
 */
@Component
public class YahooFinanceHistoricalClient {
    
    private static final Logger log = LoggerFactory.getLogger(YahooFinanceHistoricalClient.class);
    
    // Yahoo Finance historical data endpoint (v8 API)
    private static final String HISTORY_URL = "https://query1.finance.yahoo.com/v8/finance/chart/{symbol}";
    
    private final RestTemplate restTemplate;
    
    public YahooFinanceHistoricalClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    /**
     * Download historical OHLCV data for a symbol
     * 
     * @param symbol Stock symbol (e.g., "AAPL", "MSFT")
     * @param startDate Start date for historical data
     * @param endDate End date for historical data
     * @param interval "1d" for daily, "1wk" for weekly, "1mo" for monthly
     * @return List of historical data points
     */
    public List<HistoricalDataPoint> getHistoricalData(String symbol, LocalDate startDate, 
                                                       LocalDate endDate, String interval) {
        try {
            log.info("Fetching historical data for {} from {} to {} ({})", 
                     symbol, startDate, endDate, interval);
            
            // Convert dates to Unix timestamps
            long period1 = startDate.atStartOfDay(ZoneOffset.UTC).toEpochSecond();
            long period2 = endDate.atStartOfDay(ZoneOffset.UTC).toEpochSecond();
            
            // Build URL with parameters
            String url = HISTORY_URL.replace("{symbol}", symbol) + 
                        "?period1=" + period1 + 
                        "&period2=" + period2 + 
                        "&interval=" + interval +
                        "&includeAdjustedClose=true";
            
            // Make request
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            headers.set("User-Agent", "Mozilla/5.0 (compatible; Strategiz/1.0)");
            
            HttpEntity<?> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, Map.class
            );
            
            // Parse response
            return parseHistoricalData(response.getBody(), symbol);
            
        } catch (Exception e) {
            log.error("Error fetching historical data for {}", symbol, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Bulk download historical data for multiple symbols
     * Efficient for backtesting - gets years of data in one shot!
     */
    public Map<String, List<HistoricalDataPoint>> getBulkHistoricalData(
            List<String> symbols, LocalDate startDate, LocalDate endDate) {
        
        Map<String, List<HistoricalDataPoint>> results = new HashMap<>();
        
        for (String symbol : symbols) {
            // Small delay to be respectful (Yahoo has no hard rate limit)
            try {
                Thread.sleep(100); // 100ms delay = 10 requests/second max
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            List<HistoricalDataPoint> data = getHistoricalData(symbol, startDate, endDate, "1d");
            if (!data.isEmpty()) {
                results.put(symbol, data);
                log.info("Downloaded {} days of data for {}", data.size(), symbol);
            }
        }
        
        return results;
    }
    
    /**
     * Parse Yahoo Finance response into structured data
     */
    private List<HistoricalDataPoint> parseHistoricalData(Map<String, Object> response, String symbol) {
        try {
            Map<String, Object> chart = (Map<String, Object>) response.get("chart");
            List<Map<String, Object>> results = (List<Map<String, Object>>) chart.get("result");
            
            if (results == null || results.isEmpty()) {
                return Collections.emptyList();
            }
            
            Map<String, Object> result = results.get(0);
            List<Long> timestamps = (List<Long>) result.get("timestamp");
            Map<String, Object> indicators = (Map<String, Object>) result.get("indicators");
            List<Map<String, Object>> quotes = (List<Map<String, Object>>) indicators.get("quote");
            
            if (quotes == null || quotes.isEmpty()) {
                return Collections.emptyList();
            }
            
            Map<String, Object> quote = quotes.get(0);
            List<Number> opens = (List<Number>) quote.get("open");
            List<Number> highs = (List<Number>) quote.get("high");
            List<Number> lows = (List<Number>) quote.get("low");
            List<Number> closes = (List<Number>) quote.get("close");
            List<Number> volumes = (List<Number>) quote.get("volume");
            
            List<HistoricalDataPoint> dataPoints = new ArrayList<>();
            
            for (int i = 0; i < timestamps.size(); i++) {
                HistoricalDataPoint point = new HistoricalDataPoint();
                point.symbol = symbol;
                point.date = LocalDate.ofInstant(
                    Instant.ofEpochSecond(timestamps.get(i)), 
                    ZoneId.of("America/New_York")
                );
                point.open = toBigDecimal(opens.get(i));
                point.high = toBigDecimal(highs.get(i));
                point.low = toBigDecimal(lows.get(i));
                point.close = toBigDecimal(closes.get(i));
                point.volume = toLong(volumes.get(i));
                
                dataPoints.add(point);
            }
            
            return dataPoints;
            
        } catch (Exception e) {
            log.error("Error parsing historical data", e);
            return Collections.emptyList();
        }
    }
    
    private BigDecimal toBigDecimal(Number number) {
        return number != null ? new BigDecimal(number.toString()) : null;
    }
    
    private Long toLong(Number number) {
        return number != null ? number.longValue() : null;
    }
    
    /**
     * Historical data point
     */
    public static class HistoricalDataPoint {
        public String symbol;
        public LocalDate date;
        public BigDecimal open;
        public BigDecimal high;
        public BigDecimal low;
        public BigDecimal close;
        public Long volume;
        
        @Override
        public String toString() {
            return String.format("%s %s: O=%.2f H=%.2f L=%.2f C=%.2f V=%d",
                symbol, date, open, high, low, close, volume);
        }
    }
    
    /**
     * Get maximum available historical data (up to 50 years!)
     */
    public List<HistoricalDataPoint> getAllAvailableHistory(String symbol) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusYears(50); // Yahoo has data going back decades
        return getHistoricalData(symbol, startDate, endDate, "1d");
    }
}