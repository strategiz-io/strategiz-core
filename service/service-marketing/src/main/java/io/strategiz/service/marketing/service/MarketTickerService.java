package io.strategiz.service.marketing.service;

import io.strategiz.client.alpaca.client.AlpacaHistoricalClient;
import io.strategiz.client.alpaca.client.AlpacaHistoricalClient.LatestQuote;
import io.strategiz.service.marketing.model.response.MarketTickerResponse;
import io.strategiz.service.marketing.model.response.TickerItem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * Service for fetching market ticker data.
 * Contains business logic for which symbols to display.
 * Delegates to AlpacaHistoricalClient for actual data fetching.
 */
@Service
public class MarketTickerService {

    private static final Logger log = LoggerFactory.getLogger(MarketTickerService.class);

    // Popular crypto symbols to display
    private static final List<String> CRYPTO_SYMBOLS = Arrays.asList("BTC", "ETH", "SOL", "DOGE");

    // Popular stock symbols to display
    private static final List<String> STOCK_SYMBOLS = Arrays.asList(
        "AAPL", "MSFT", "GOOG", "AMZN", "NVDA", "META", "TSLA",
        "JPM", "V", "MA", "DIS", "NFLX", "AMD", "CRM", "ORCL"
    );

    // Symbol to company name mapping
    private static final Map<String, String> SYMBOL_NAMES = Map.ofEntries(
        Map.entry("AAPL", "Apple Inc."),
        Map.entry("MSFT", "Microsoft Corp."),
        Map.entry("GOOG", "Alphabet Inc."),
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

    public MarketTickerService(AlpacaHistoricalClient alpacaClient) {
        this.alpacaClient = alpacaClient;
    }

    /**
     * Check if the Alpaca client is available.
     */
    public boolean isAlpacaAvailable() {
        return alpacaClient.isAvailable();
    }

    /**
     * Fetch market ticker data for popular assets from Alpaca.
     *
     * @return MarketTickerResponse with stock and crypto prices
     */
    public MarketTickerResponse getMarketTicker() {
        log.info("Fetching market ticker data from Alpaca");

        try {
            if (!alpacaClient.isAvailable()) {
                log.warn("Alpaca client not available, returning demo data");
                return getDemoTickerData();
            }

            // Fetch data from Alpaca
            List<TickerItem> stockItems = fetchStockData();
            List<TickerItem> cryptoItems = fetchCryptoData();

            // Combine results - stocks first, then crypto
            List<TickerItem> allItems = new ArrayList<>();
            allItems.addAll(stockItems);
            allItems.addAll(cryptoItems);

            // If no data returned, fall back to demo
            if (allItems.isEmpty()) {
                log.warn("No data from Alpaca, returning demo data");
                return getDemoTickerData();
            }

            return new MarketTickerResponse(
                allItems,
                System.currentTimeMillis(),
                30 // cache duration in seconds
            );

        } catch (Exception e) {
            log.error("Error fetching market ticker data from Alpaca: {}", e.getMessage());
            return getDemoTickerData();
        }
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
                    quote.getPrice(),
                    quote.getChange(),
                    quote.getChangePercent(),
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
                    quote.getPrice(),
                    quote.getChange(),
                    quote.getChangePercent(),
                    quote.isPositive()
                ));
            }

            log.info("Fetched {} crypto quotes from Alpaca", items.size());
        } catch (Exception e) {
            log.warn("Failed to fetch crypto data from Alpaca: {}", e.getMessage());
        }

        return items;
    }

    /**
     * Demo data fallback when Alpaca is unavailable
     */
    private MarketTickerResponse getDemoTickerData() {
        List<TickerItem> items = new ArrayList<>();
        items.addAll(getDemoStockData());
        items.addAll(getDemoCryptoData());

        return new MarketTickerResponse(
            items,
            System.currentTimeMillis(),
            30
        );
    }

    private List<TickerItem> getDemoCryptoData() {
        return Arrays.asList(
            new TickerItem("BTC", "Bitcoin", "crypto", new BigDecimal("43250.00"),
                new BigDecimal("1250.00"), new BigDecimal("2.95"), true),
            new TickerItem("ETH", "Ethereum", "crypto", new BigDecimal("2650.00"),
                new BigDecimal("-85.00"), new BigDecimal("-3.10"), false),
            new TickerItem("SOL", "Solana", "crypto", new BigDecimal("98.50"),
                new BigDecimal("3.25"), new BigDecimal("3.42"), true),
            new TickerItem("DOGE", "Dogecoin", "crypto", new BigDecimal("0.082"),
                new BigDecimal("0.003"), new BigDecimal("3.80"), true)
        );
    }

    private List<TickerItem> getDemoStockData() {
        return Arrays.asList(
            new TickerItem("AAPL", "Apple Inc.", "stock", new BigDecimal("257.50"),
                new BigDecimal("2.25"), new BigDecimal("0.88"), true),
            new TickerItem("MSFT", "Microsoft Corp.", "stock", new BigDecimal("438.20"),
                new BigDecimal("5.80"), new BigDecimal("1.34"), true),
            new TickerItem("GOOG", "Alphabet Inc.", "stock", new BigDecimal("192.75"),
                new BigDecimal("2.85"), new BigDecimal("1.50"), true),
            new TickerItem("AMZN", "Amazon.com Inc.", "stock", new BigDecimal("227.80"),
                new BigDecimal("-1.90"), new BigDecimal("-0.83"), false),
            new TickerItem("NVDA", "NVIDIA Corp.", "stock", new BigDecimal("134.30"),
                new BigDecimal("3.60"), new BigDecimal("2.75"), true),
            new TickerItem("META", "Meta Platforms Inc.", "stock", new BigDecimal("612.20"),
                new BigDecimal("8.75"), new BigDecimal("1.45"), true),
            new TickerItem("TSLA", "Tesla Inc.", "stock", new BigDecimal("421.50"),
                new BigDecimal("-5.20"), new BigDecimal("-1.22"), false),
            new TickerItem("JPM", "JPMorgan Chase", "stock", new BigDecimal("243.80"),
                new BigDecimal("1.95"), new BigDecimal("0.81"), true),
            new TickerItem("V", "Visa Inc.", "stock", new BigDecimal("317.40"),
                new BigDecimal("3.20"), new BigDecimal("1.02"), true),
            new TickerItem("MA", "Mastercard Inc.", "stock", new BigDecimal("528.60"),
                new BigDecimal("4.50"), new BigDecimal("0.86"), true),
            new TickerItem("DIS", "Walt Disney Co.", "stock", new BigDecimal("112.80"),
                new BigDecimal("-0.85"), new BigDecimal("-0.75"), false),
            new TickerItem("NFLX", "Netflix Inc.", "stock", new BigDecimal("925.40"),
                new BigDecimal("12.30"), new BigDecimal("1.35"), true),
            new TickerItem("AMD", "AMD Inc.", "stock", new BigDecimal("124.60"),
                new BigDecimal("2.40"), new BigDecimal("1.96"), true),
            new TickerItem("CRM", "Salesforce Inc.", "stock", new BigDecimal("342.20"),
                new BigDecimal("-2.80"), new BigDecimal("-0.81"), false),
            new TickerItem("ORCL", "Oracle Corp.", "stock", new BigDecimal("172.90"),
                new BigDecimal("1.65"), new BigDecimal("0.96"), true)
        );
    }
}
