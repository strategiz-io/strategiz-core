package io.strategiz.business.provider.kraken.helper;

import io.strategiz.client.yahoofinance.client.YahooFinanceClient;
import io.strategiz.client.yahoofinance.mapper.SymbolMapper;
import io.strategiz.client.yahoofinance.model.PriceQuote;
import io.strategiz.client.yahoofinance.parser.ResponseParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for fetching and processing prices from Yahoo Finance.
 * Contains the business logic for transforming raw API data into usable price information.
 */
@Service
public class YahooFinancePriceService {

    private static final Logger log = LoggerFactory.getLogger(YahooFinancePriceService.class);

    private final YahooFinanceClient yahooFinanceClient;
    private final SymbolMapper symbolMapper;
    private final ResponseParser responseParser;

    @Autowired
    public YahooFinancePriceService(@Autowired(required = false) YahooFinanceClient yahooFinanceClient,
                                   SymbolMapper symbolMapper,
                                   ResponseParser responseParser) {
        this.yahooFinanceClient = yahooFinanceClient;
        this.symbolMapper = symbolMapper;
        this.responseParser = responseParser;
    }

    /**
     * Get bulk prices for multiple symbols.
     * Handles symbol conversion and price extraction from raw API responses.
     *
     * @param symbols List of symbols (can be in various formats)
     * @return Map of original symbol to price in USD
     */
    public Map<String, Double> getBulkPrices(List<String> symbols) {
        if (yahooFinanceClient == null || symbols == null || symbols.isEmpty()) {
            return new HashMap<>();
        }

        // Convert symbols to Yahoo Finance format
        List<String> yahooSymbols = symbols.stream()
            .map(symbolMapper::toYahooSymbol)
            .collect(Collectors.toList());

        String symbolsParam = String.join(",", yahooSymbols);
        log.debug("Fetching bulk prices for symbols: {}", symbolsParam);

        try {
            // Fetch raw data from client
            Map<String, Object> rawResponse = yahooFinanceClient.fetchBulkQuotes(symbolsParam);

            // Parse response based on API version
            Map<String, PriceQuote> quotes;
            if (rawResponse.containsKey("quoteSummary")) {
                quotes = responseParser.parseV10Response(rawResponse);
            } else if (rawResponse.containsKey("quoteResponse")) {
                quotes = responseParser.parseV8Response(rawResponse);
            } else {
                log.warn("Unknown response format from Yahoo Finance");
                return new HashMap<>();
            }

            // Convert PriceQuote to simple price map
            Map<String, Double> prices = new HashMap<>();
            for (Map.Entry<String, PriceQuote> entry : quotes.entrySet()) {
                PriceQuote quote = entry.getValue();
                if (quote != null && quote.getPrice() != null) {
                    // Map back to original symbol
                    String originalSymbol = findOriginalSymbol(symbols, entry.getKey());
                    prices.put(originalSymbol, quote.getPrice().doubleValue());
                }
            }

            log.info("Successfully fetched {} prices out of {} requested", prices.size(), symbols.size());
            return prices;

        } catch (Exception e) {
            log.error("Error fetching prices from Yahoo Finance: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Find the original symbol from the list that corresponds to the Yahoo symbol.
     */
    private String findOriginalSymbol(List<String> originalSymbols, String yahooSymbol) {
        for (String original : originalSymbols) {
            String converted = symbolMapper.toYahooSymbol(original);
            if (converted.equalsIgnoreCase(yahooSymbol)) {
                return original;
            }
        }
        // If no match found, strip -USD suffix if present
        if (yahooSymbol.endsWith("-USD")) {
            String base = yahooSymbol.substring(0, yahooSymbol.length() - 4);
            for (String original : originalSymbols) {
                if (original.equalsIgnoreCase(base)) {
                    return original;
                }
            }
        }
        return yahooSymbol;
    }
}