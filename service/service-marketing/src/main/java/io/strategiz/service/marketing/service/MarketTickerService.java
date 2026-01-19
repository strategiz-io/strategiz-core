package io.strategiz.service.marketing.service;

import io.strategiz.client.fmp.client.FmpFundamentalsClient;
import io.strategiz.client.fmp.client.FmpFundamentalsClient.FmpQuote;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.marketing.exception.ServiceMarketingErrorDetails;
import io.strategiz.service.marketing.model.response.MarketTickerResponse;
import io.strategiz.service.marketing.model.response.TickerItem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * Service for fetching market ticker data.
 * Contains business logic for which symbols to display.
 * Delegates to FmpFundamentalsClient for actual data fetching.
 *
 * Note: FmpFundamentalsClient is optional. If not configured,
 * the ticker endpoint will return an error.
 */
@Service
public class MarketTickerService {

    private static final Logger log = LoggerFactory.getLogger(MarketTickerService.class);

    // Popular crypto symbols to display (FMP uses USD pairs)
    private static final List<String> CRYPTO_SYMBOLS = Arrays.asList("BTCUSD", "ETHUSD", "SOLUSD", "DOGEUSD");

    // Popular stock symbols to display
    // Note: Using GOOGL (Class A shares) instead of GOOG (Class C) due to FMP subscription
    private static final List<String> STOCK_SYMBOLS = Arrays.asList(
        "AAPL", "MSFT", "GOOGL", "AMZN", "NVDA", "META", "TSLA",
        "JPM", "V", "MA", "DIS", "NFLX", "AMD", "CRM", "ORCL"
    );

    // Symbol to company name mapping
    private static final Map<String, String> SYMBOL_NAMES = Map.ofEntries(
        Map.entry("AAPL", "Apple Inc."),
        Map.entry("MSFT", "Microsoft Corp."),
        Map.entry("GOOGL", "Alphabet Inc."),
        Map.entry("AMZN", "Amazon.com Inc."),
        Map.entry("NVDA", "NVIDIA Corp."),
        Map.entry("META", "Meta Platforms Inc."),
        Map.entry("TSLA", "Tesla Inc."),
        Map.entry("JPM", "JPMorgan Chase"),
        Map.entry("V", "Visa Inc."),
        Map.entry("MA", "Mastercard Inc."),
        Map.entry("DIS", "Walt Disney Co."),
        Map.entry("NFLX", "Netflix Inc."),
        Map.entry("AMD", "AMD Inc."),
        Map.entry("CRM", "Salesforce Inc."),
        Map.entry("ORCL", "Oracle Corp."),
        Map.entry("BTCUSD", "Bitcoin"),
        Map.entry("ETHUSD", "Ethereum"),
        Map.entry("SOLUSD", "Solana"),
        Map.entry("DOGEUSD", "Dogecoin")
    );

    // Display symbol mapping (for crypto to show BTC instead of BTCUSD)
    private static final Map<String, String> DISPLAY_SYMBOLS = Map.of(
        "BTCUSD", "BTC",
        "ETHUSD", "ETH",
        "SOLUSD", "SOL",
        "DOGEUSD", "DOGE"
    );

    private final FmpFundamentalsClient fmpClient;

    @Autowired
    public MarketTickerService(@Autowired(required = false) FmpFundamentalsClient fmpClient) {
        this.fmpClient = fmpClient;
        if (fmpClient == null) {
            log.warn("FmpFundamentalsClient not available - market ticker will not function. " +
                    "Ensure strategiz.fmp.enabled=true and fmp.api-key is configured.");
        }
    }

    /**
     * Check if the FMP client is available.
     */
    public boolean isFmpAvailable() {
        return fmpClient != null;
    }

    /**
     * Fetch market ticker data for popular assets from FMP.
     * Throws exception if data cannot be fetched - no demo data fallback.
     *
     * @return MarketTickerResponse with stock and crypto prices
     * @throws StrategizException if FMP is unavailable or returns no data
     */
    public MarketTickerResponse getMarketTicker() {
        log.info("Fetching market ticker data from FMP");

        if (!isFmpAvailable()) {
            log.error("FMP client not available");
            throw new StrategizException(
                ServiceMarketingErrorDetails.MARKET_DATA_UNAVAILABLE,
                "service-marketing",
                "Market data service is not available"
            );
        }

        // Fetch data from FMP
        List<TickerItem> stockItems = fetchStockData();
        List<TickerItem> cryptoItems = fetchCryptoData();

        // Combine results - stocks first, then crypto
        List<TickerItem> allItems = new ArrayList<>();
        allItems.addAll(stockItems);
        allItems.addAll(cryptoItems);

        // If no data returned, throw error
        if (allItems.isEmpty()) {
            log.error("No data returned from FMP");
            throw new StrategizException(
                ServiceMarketingErrorDetails.MARKET_DATA_UNAVAILABLE,
                "service-marketing",
                "No market data available"
            );
        }

        return new MarketTickerResponse(
            allItems,
            System.currentTimeMillis(),
            30 // cache duration in seconds
        );
    }

    /**
     * Fetch stock data from FMP API
     */
    private List<TickerItem> fetchStockData() {
        List<TickerItem> items = new ArrayList<>();

        try {
            log.info("Fetching stock data from FMP for {} symbols", STOCK_SYMBOLS.size());
            List<FmpQuote> quotes = fmpClient.getQuotes(STOCK_SYMBOLS);

            for (FmpQuote quote : quotes) {
                String symbol = quote.getSymbol();

                items.add(new TickerItem(
                    symbol,
                    quote.getName() != null ? quote.getName() : SYMBOL_NAMES.getOrDefault(symbol, symbol),
                    "stock",
                    quote.getPrice() != null ? quote.getPrice() : BigDecimal.ZERO,
                    quote.getChange() != null ? quote.getChange() : BigDecimal.ZERO,
                    quote.getChangePercent() != null ? quote.getChangePercent() : BigDecimal.ZERO,
                    quote.isPositive()
                ));
            }

            log.info("Fetched {} stock quotes from FMP", items.size());
        } catch (Exception e) {
            log.warn("Failed to fetch stock data from FMP: {}", e.getMessage());
        }

        return items;
    }

    /**
     * Fetch crypto data from FMP API
     */
    private List<TickerItem> fetchCryptoData() {
        List<TickerItem> items = new ArrayList<>();

        try {
            log.info("Fetching crypto data from FMP for {} symbols", CRYPTO_SYMBOLS.size());
            List<FmpQuote> quotes = fmpClient.getQuotes(CRYPTO_SYMBOLS);

            for (FmpQuote quote : quotes) {
                String symbol = quote.getSymbol();
                String displaySymbol = DISPLAY_SYMBOLS.getOrDefault(symbol, symbol);

                items.add(new TickerItem(
                    displaySymbol,
                    SYMBOL_NAMES.getOrDefault(symbol, displaySymbol),
                    "crypto",
                    quote.getPrice() != null ? quote.getPrice() : BigDecimal.ZERO,
                    quote.getChange() != null ? quote.getChange() : BigDecimal.ZERO,
                    quote.getChangePercent() != null ? quote.getChangePercent() : BigDecimal.ZERO,
                    quote.isPositive()
                ));
            }

            log.info("Fetched {} crypto quotes from FMP", items.size());
        } catch (Exception e) {
            log.warn("Failed to fetch crypto data from FMP: {}", e.getMessage());
        }

        return items;
    }
}
