package io.strategiz.service.exchange.trading.agent.gemini;

import io.strategiz.client.coinbase.CoinbaseClient;
import io.strategiz.client.coinbase.exception.CoinbaseApiException;
import io.strategiz.service.exchange.trading.agent.model.HistoricalPriceData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service to fetch real Coinbase historical data for the Gemini AI agent
 */
@Service
public class CoinbaseDataFetcher {

    private final CoinbaseClient coinbaseClient;
    private static final Logger log = LoggerFactory.getLogger(CoinbaseDataFetcher.class);
    
    @Autowired
    public CoinbaseDataFetcher(CoinbaseClient coinbaseClient) {
        this.coinbaseClient = coinbaseClient;
    }
    
    /**
     * Fetch real historical BTC price data from Coinbase
     * 
     * @param apiKey Coinbase API key
     * @param privateKey Coinbase private key
     * @param timeframe Timeframe for analysis (e.g., "1d", "6h", "1h", "15m")
     * @param limit Number of data points to fetch
     * @return List of historical price data
     */
    public List<HistoricalPriceData> fetchBitcoinHistoricalData(
            String apiKey, String privateKey, String timeframe, int limit) {
        
        log.info("Fetching real historical BTC price data from Coinbase API");
        
        try {
            // Prepare parameters for Coinbase API request
            String endpoint = "/products/BTC-USD/candles";
            Map<String, String> params = new HashMap<>();
            params.put("granularity", convertTimeframeToSeconds(timeframe));
            
            // Make signed request to Coinbase API
            List<Object[]> candleData = coinbaseClient.signedRequest(
                HttpMethod.GET,
                endpoint,
                params,
                apiKey,
                privateKey,
                new ParameterizedTypeReference<List<Object[]>>() {}
            );
            
            // Convert candle data to HistoricalPriceData objects
            List<HistoricalPriceData> priceData = candleData.stream()
                .map(this::convertCandleToHistoricalData)
                .limit(limit)
                .collect(Collectors.toList());
                
            log.info("Successfully fetched {} historical BTC price points from Coinbase", priceData.size());
            return priceData;
            
        } catch (CoinbaseApiException e) {
            log.error("Error fetching historical BTC data from Coinbase: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Convert Coinbase candle data to HistoricalPriceData
     */
    private HistoricalPriceData convertCandleToHistoricalData(Object[] candle) {
        // Coinbase API returns candles in format [timestamp, low, high, open, close, volume]
        long timestamp = ((Number) candle[0]).longValue();
        BigDecimal low = BigDecimal.valueOf(((Number) candle[1]).doubleValue());
        BigDecimal high = BigDecimal.valueOf(((Number) candle[2]).doubleValue());
        BigDecimal open = BigDecimal.valueOf(((Number) candle[3]).doubleValue());
        BigDecimal close = BigDecimal.valueOf(((Number) candle[4]).doubleValue());
        BigDecimal volume = BigDecimal.valueOf(((Number) candle[5]).doubleValue());
        
        return HistoricalPriceData.builder()
            .assetSymbol("BTC")
            .timestamp(LocalDateTime.ofInstant(new Date(timestamp * 1000).toInstant(), ZoneId.systemDefault()))
            .low(low)
            .high(high)
            .open(open)
            .close(close)
            .volume(volume)
            .quoteVolume(volume.multiply(close)) // Approximate quote volume
            .build();
    }
    
    /**
     * Convert timeframe string to granularity in seconds for Coinbase API
     */
    private String convertTimeframeToSeconds(String timeframe) {
        switch (timeframe) {
            case "1d": return "86400";  // 60 * 60 * 24
            case "6h": return "21600";  // 60 * 60 * 6
            case "1h": return "3600";   // 60 * 60
            case "15m": return "900";   // 60 * 15
            default: return "3600";     // Default to 1 hour
        }
    }
    
    /**
     * Get default data points limit based on timeframe
     */
    public int getDefaultLimitForTimeframe(String timeframe) {
        switch (timeframe) {
            case "1d": return 90;   // 90 days
            case "6h": return 100;  // 100 6-hour candles
            case "1h": return 168;  // 168 hours (1 week)
            case "15m": return 192; // 192 15-minute candles (2 days)
            default: return 100;
        }
    }
}
