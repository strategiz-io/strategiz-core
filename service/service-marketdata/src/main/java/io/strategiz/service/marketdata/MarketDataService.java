package io.strategiz.service.marketdata;

import io.strategiz.data.marketdata.constants.Timeframe;
import io.strategiz.data.marketdata.entity.MarketDataEntity;
import io.strategiz.data.marketdata.repository.MarketDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for fetching market data from Firestore
 * Provides methods to retrieve historical OHLCV bars for charting and analysis
 */
@Service
public class MarketDataService {

    private static final Logger log = LoggerFactory.getLogger(MarketDataService.class);

    private final MarketDataRepository marketDataRepository;

    @Autowired
    public MarketDataService(MarketDataRepository marketDataRepository) {
        this.marketDataRepository = marketDataRepository;
    }

    /**
     * Fetch market data bars for a symbol within a date range
     * Cached for 5 minutes to reduce Firestore reads
     *
     * @param symbol The stock symbol (e.g., "AAPL")
     * @param timeframe The timeframe (e.g., "1Day", "1Hour", "1Min") - accepts both canonical and legacy formats
     * @param startDate Start date in ISO format (e.g., "2024-01-01T00:00:00Z")
     * @param endDate End date in ISO format (e.g., "2024-12-31T23:59:59Z")
     * @return List of market data entities sorted by timestamp ascending
     */
    @Cacheable(value = "marketDataBars", key = "#symbol.toUpperCase() + '-' + #timeframe + '-' + #startDate + '-' + #endDate")
    public List<MarketDataEntity> getMarketDataBars(String symbol, String timeframe, String startDate, String endDate) {
        log.info("Fetching market data for symbol={}, timeframe={}, start={}, end={}",
            symbol, timeframe, startDate, endDate);

        try {
            // Convert ISO date strings to LocalDate
            LocalDate startLocalDate = parseIsoDateToLocalDate(startDate);
            LocalDate endLocalDate = parseIsoDateToLocalDate(endDate);

            // Get all possible timeframe aliases for querying (handles legacy formats)
            String normalizedTimeframe = Timeframe.normalize(timeframe);
            Set<String> timeframeAliases = Timeframe.getAliases(normalizedTimeframe);
            log.debug("Timeframe {} normalized to {}, querying with aliases: {}",
                timeframe, normalizedTimeframe, timeframeAliases);

            // Query repository
            List<MarketDataEntity> results;

            if (startLocalDate != null && endLocalDate != null) {
                // Query with date range
                results = marketDataRepository.findBySymbolAndDateRange(symbol.toUpperCase(), startLocalDate, endLocalDate);

                // Filter by timeframe if specified (match any alias)
                if (timeframe != null && !timeframe.isEmpty()) {
                    results = results.stream()
                        .filter(entity -> entity.getTimeframe() != null &&
                                          timeframeAliases.contains(entity.getTimeframe()))
                        .collect(Collectors.toList());
                }
            } else if (timeframe != null && !timeframe.isEmpty()) {
                // Query by symbol and timeframe only
                // Try normalized first, then fall back to original if empty
                results = marketDataRepository.findBySymbolAndTimeframe(symbol.toUpperCase(), normalizedTimeframe);
                if (results.isEmpty() && !normalizedTimeframe.equals(timeframe)) {
                    log.debug("No results for normalized timeframe {}, trying original: {}", normalizedTimeframe, timeframe);
                    results = marketDataRepository.findBySymbolAndTimeframe(symbol.toUpperCase(), timeframe);
                }
            } else {
                // Query by symbol only
                results = marketDataRepository.findBySymbol(symbol.toUpperCase());
            }

            // Sort by timestamp ascending (oldest first)
            results.sort(Comparator.comparing(MarketDataEntity::getTimestamp));

            log.info("Retrieved {} market data bars for {} with timeframe {}", results.size(), symbol, timeframe);
            return results;

        } catch (Exception e) {
            log.error("Error fetching market data for symbol={}", symbol, e);
            return Collections.emptyList();
        }
    }

    /**
     * Get the latest market data for a symbol
     * Cached for 1 minute for near-real-time updates
     *
     * @param symbol The stock symbol
     * @return Latest market data entity or null if not found
     */
    @Cacheable(value = "latestMarketData", key = "#symbol.toUpperCase()")
    public MarketDataEntity getLatestMarketData(String symbol) {
        log.info("Fetching latest market data for symbol={}", symbol);

        try {
            return marketDataRepository.findLatestBySymbol(symbol.toUpperCase()).orElse(null);
        } catch (Exception e) {
            log.error("Error fetching latest market data for symbol={}", symbol, e);
            return null;
        }
    }

    /**
     * Get all available symbols in the database
     * Cached for 1 hour since symbols rarely change
     *
     * @return List of distinct symbols
     */
    @Cacheable(value = "availableSymbols", key = "'all'")
    public List<String> getAvailableSymbols() {
        log.info("Fetching available symbols");

        try {
            return marketDataRepository.findDistinctSymbols();
        } catch (Exception e) {
            log.error("Error fetching available symbols", e);
            return Collections.emptyList();
        }
    }

    /**
     * Parse ISO date string to LocalDate
     * Supports formats like "2024-01-01T00:00:00Z" or "2024-01-01"
     */
    private LocalDate parseIsoDateToLocalDate(String isoDateString) {
        if (isoDateString == null || isoDateString.isEmpty()) {
            return null;
        }

        try {
            // Try parsing as ISO instant first
            if (isoDateString.contains("T")) {
                Instant instant = Instant.parse(isoDateString);
                return instant.atZone(ZoneId.of("UTC")).toLocalDate();
            } else {
                // Parse as simple date
                return LocalDate.parse(isoDateString, DateTimeFormatter.ISO_LOCAL_DATE);
            }
        } catch (DateTimeParseException e) {
            log.warn("Failed to parse date string: {}", isoDateString, e);
            return null;
        }
    }
}
