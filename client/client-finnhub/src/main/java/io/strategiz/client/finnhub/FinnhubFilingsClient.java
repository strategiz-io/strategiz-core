package io.strategiz.client.finnhub;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bucket;
import io.strategiz.client.finnhub.config.FinnhubConfig;
import io.strategiz.client.finnhub.dto.SECFiling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Finnhub client for SEC filings endpoints
 */
@Component
public class FinnhubFilingsClient extends FinnhubClient {

    private static final Logger log = LoggerFactory.getLogger(FinnhubFilingsClient.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    public FinnhubFilingsClient(
            FinnhubConfig config,
            @Qualifier("finnhubRateLimiter") Bucket rateLimiter,
            ObjectMapper objectMapper) {
        super(config, rateLimiter, objectMapper);
    }

    /**
     * Get SEC filings for a company
     * @param symbol Stock symbol
     * @param fromDate Start date
     * @param toDate End date
     * @return List of SEC filings
     */
    @Cacheable(value = "finnhubFilings", key = "#symbol + '-' + #fromDate + '-' + #toDate", unless = "#result.isEmpty()")
    public List<SECFiling> getFilings(String symbol, LocalDate fromDate, LocalDate toDate) {
        log.debug("Fetching SEC filings for {} from {} to {}", symbol, fromDate, toDate);

        Map<String, String> params = Map.of(
                "symbol", symbol.toUpperCase(),
                "from", fromDate.format(DATE_FORMATTER),
                "to", toDate.format(DATE_FORMATTER)
        );

        return get("/stock/filings", params, SECFiling[].class)
                .map(List::of)
                .orElse(Collections.emptyList());
    }

    /**
     * Get recent SEC filings for a company (last 90 days)
     */
    public List<SECFiling> getRecentFilings(String symbol) {
        LocalDate today = LocalDate.now();
        return getFilings(symbol, today.minusDays(90), today);
    }

    /**
     * Get major SEC filings only (10-K, 10-Q, 8-K)
     */
    public List<SECFiling> getMajorFilings(String symbol, int days) {
        LocalDate today = LocalDate.now();
        return getFilings(symbol, today.minusDays(days), today).stream()
                .filter(SECFiling::isMajorFiling)
                .sorted(Comparator.comparing(SECFiling::getFiledDate).reversed())
                .toList();
    }

    /**
     * Get filings for multiple symbols
     */
    public List<SECFiling> getFilingsForSymbols(List<String> symbols, int days) {
        if (symbols == null || symbols.isEmpty()) {
            return Collections.emptyList();
        }

        LocalDate today = LocalDate.now();
        LocalDate fromDate = today.minusDays(days);

        return symbols.stream()
                .flatMap(symbol -> getFilings(symbol, fromDate, today).stream())
                .filter(SECFiling::isMajorFiling)
                .sorted(Comparator.comparing(SECFiling::getFiledDate).reversed())
                .limit(20)
                .toList();
    }

    /**
     * Get specific filing type
     */
    public List<SECFiling> getFilingsByType(String symbol, String formType, int days) {
        LocalDate today = LocalDate.now();
        return getFilings(symbol, today.minusDays(days), today).stream()
                .filter(f -> f.getForm() != null && f.getForm().equalsIgnoreCase(formType))
                .sorted(Comparator.comparing(SECFiling::getFiledDate).reversed())
                .toList();
    }

    /**
     * Format SEC filings for AI context injection
     */
    public String formatFilingsForContext(List<SECFiling> filings) {
        if (filings == null || filings.isEmpty()) {
            return "No recent SEC filings available.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("RECENT SEC FILINGS:\n");
        sb.append("| Symbol | Form | Description | Filed Date |\n");
        sb.append("|--------|------|-------------|------------|\n");

        filings.stream()
                .limit(15)
                .forEach(filing -> {
                    sb.append(String.format("| %s | %s | %s | %s |\n",
                            filing.getSymbol() != null ? filing.getSymbol() : "N/A",
                            filing.getForm() != null ? filing.getForm() : "N/A",
                            filing.getFormDescription(),
                            filing.getFiledDate() != null ? filing.getFiledDate() : "N/A"));
                });

        return sb.toString();
    }
}
