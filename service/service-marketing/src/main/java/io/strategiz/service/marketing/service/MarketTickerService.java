package io.strategiz.service.marketing.service;

import io.strategiz.client.alpaca.client.AlpacaHistoricalClient;
import io.strategiz.client.alpaca.client.AlpacaHistoricalClient.LatestQuote;
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
 * Delegates to AlpacaHistoricalClient for actual data fetching.
 *
 * Note: AlpacaHistoricalClient is optional. If not configured,
 * the ticker endpoint will return an error.
 */
@Service
public class MarketTickerService {

    private static final Logger log = LoggerFactory.getLogger(MarketTickerService.class);

    // Popular crypto symbols to display (Alpaca uses BTC, ETH format)
    private static final List<String> CRYPTO_SYMBOLS = Arrays.asList("BTC", "ETH", "SOL", "DOGE");

    // Popular stock symbols to display
    // Note: Using GOOGL (Class A shares) instead of GOOG (Class C) for better IEX liquidity
    private static final List<String> STOCK_SYMBOLS = Arrays.asList(
        "AAPL", "MSFT", "GOOGL", "AMZN", "NVDA", "META", "TSLA",
        "JPM", "V", "MA", "DIS", "NFLX", "AMD", "CRM", "ORCL"
    );

    // Symbol to company name mapping (fallback if API doesn't return name)
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
        Map.entry("BTC", "Bitcoin"),
        Map.entry("ETH", "Ethereum"),
        Map.entry("SOL", "Solana"),
        Map.entry("DOGE", "Dogecoin")
    );

    private final AlpacaHistoricalClient alpacaClient;

    @Autowired
    public MarketTickerService(@Autowired(required = false) AlpacaHistoricalClient alpacaClient) {
        this.alpacaClient = alpacaClient;
        if (alpacaClient == null) {
            log.warn("AlpacaHistoricalClient not available - market ticker will not function. " +
                    "Ensure Alpaca credentials are configured in Vault.");
        }
    }

    /**
     * Check if the Alpaca client is available.
     */
    public boolean isAlpacaAvailable() {
        return alpacaClient != null && alpacaClient.isAvailable();
    }

    /**
     * Fetch market ticker data for popular assets from Alpaca.
     * Throws exception if data cannot be fetched - no demo data fallback.
     *
     * @return MarketTickerResponse with stock and crypto prices
     * @throws StrategizException if Alpaca is unavailable or returns no data
     */
    public MarketTickerResponse getMarketTicker() {
        log.info("Fetching market ticker data from Alpaca");

        if (!isAlpacaAvailable()) {
            log.error("Alpaca client not available");
            throw new StrategizException(
                ServiceMarketingErrorDetails.MARKET_DATA_UNAVAILABLE,
                "service-marketing",
                "Market data service is not available"
            );
        }

        // Fetch data from Alpaca
        List<TickerItem> stockItems = fetchStockData();
        List<TickerItem> cryptoItems = fetchCryptoData();

        // Combine results - stocks first, then crypto
        List<TickerItem> allItems = new ArrayList<>();
        allItems.addAll(stockItems);
        allItems.addAll(cryptoItems);

        // If no data returned, throw error
        if (allItems.isEmpty()) {
            log.error("No data returned from Alpaca");
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
     * Fetch stock data from Alpaca API
     */
    private List<TickerItem> fetchStockData() {
        List<TickerItem> items = new ArrayList<>();

        try {
            log.info("Fetching stock data from Alpaca for {} symbols", STOCK_SYMBOLS.size());
            Map<String, LatestQuote> quotes = alpacaClient.getLatestStockQuotes(STOCK_SYMBOLS);

            for (Map.Entry<String, LatestQuote> entry : quotes.entrySet()) {
                String symbol = entry.getKey();
                LatestQuote quote = entry.getValue();

                items.add(new TickerItem(
                    symbol,
                    SYMBOL_NAMES.getOrDefault(symbol, symbol),
                    "stock",
                    quote.getPrice() != null ? quote.getPrice() : BigDecimal.ZERO,
                    quote.getChange() != null ? quote.getChange() : BigDecimal.ZERO,
                    quote.getChangePercent() != null ? quote.getChangePercent() : BigDecimal.ZERO,
                    quote.isPositive()
                ));
            }

            log.info("Fetched {} stock quotes from Alpaca", items.size());
        } catch (Exception e) {
            log.warn("Failed to fetch stock data from Alpaca: {}", e.getMessage());
        }

        return items;
    }

    /**
     * Fetch crypto data from Alpaca API
     */
    private List<TickerItem> fetchCryptoData() {
        List<TickerItem> items = new ArrayList<>();

        try {
            log.info("Fetching crypto data from Alpaca for {} symbols", CRYPTO_SYMBOLS.size());
            Map<String, LatestQuote> quotes = alpacaClient.getLatestCryptoQuotes(CRYPTO_SYMBOLS);

            for (Map.Entry<String, LatestQuote> entry : quotes.entrySet()) {
                String symbol = entry.getKey();
                LatestQuote quote = entry.getValue();

                items.add(new TickerItem(
                    symbol,
                    SYMBOL_NAMES.getOrDefault(symbol, symbol),
                    "crypto",
                    quote.getPrice() != null ? quote.getPrice() : BigDecimal.ZERO,
                    quote.getChange() != null ? quote.getChange() : BigDecimal.ZERO,
                    quote.getChangePercent() != null ? quote.getChangePercent() : BigDecimal.ZERO,
                    quote.isPositive()
                ));
            }

            log.info("Fetched {} crypto quotes from Alpaca", items.size());
        } catch (Exception e) {
            log.warn("Failed to fetch crypto data from Alpaca: {}", e.getMessage());
        }

        return items;
    }
}
