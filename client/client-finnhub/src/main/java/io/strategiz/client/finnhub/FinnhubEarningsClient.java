package io.strategiz.client.finnhub;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bucket;
import io.strategiz.client.finnhub.config.FinnhubConfig;
import io.strategiz.client.finnhub.dto.EarningsCalendarEvent;
import io.strategiz.client.finnhub.dto.EarningsCalendarResponse;
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
import java.util.stream.Collectors;

/**
 * Finnhub client for earnings calendar endpoints
 */
@Component
public class FinnhubEarningsClient extends FinnhubClient {

    private static final Logger log = LoggerFactory.getLogger(FinnhubEarningsClient.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    public FinnhubEarningsClient(
            FinnhubConfig config,
            @Qualifier("finnhubRateLimiter") Bucket rateLimiter,
            ObjectMapper objectMapper) {
        super(config, rateLimiter, objectMapper);
    }

    /**
     * Get earnings calendar for a date range
     * @param fromDate Start date
     * @param toDate End date
     * @return List of earnings events
     */
    @Cacheable(value = "finnhubEarningsCalendar", key = "#fromDate + '-' + #toDate", unless = "#result.isEmpty()")
    public List<EarningsCalendarEvent> getEarningsCalendar(LocalDate fromDate, LocalDate toDate) {
        log.debug("Fetching earnings calendar from {} to {}", fromDate, toDate);

        Map<String, String> params = Map.of(
                "from", fromDate.format(DATE_FORMATTER),
                "to", toDate.format(DATE_FORMATTER)
        );

        return get("/calendar/earnings", params, EarningsCalendarResponse.class)
                .map(EarningsCalendarResponse::getEarningsCalendar)
                .orElse(Collections.emptyList());
    }

    /**
     * Get upcoming earnings for the next N days
     * @param days Number of days to look ahead
     * @return List of upcoming earnings events
     */
    public List<EarningsCalendarEvent> getUpcomingEarnings(int days) {
        LocalDate today = LocalDate.now();
        return getEarningsCalendar(today, today.plusDays(days));
    }

    /**
     * Get earnings calendar for specific symbols
     * @param symbols List of symbols to filter
     * @param days Number of days to look ahead
     * @return Filtered list of earnings events
     */
    public List<EarningsCalendarEvent> getEarningsForSymbols(List<String> symbols, int days) {
        if (symbols == null || symbols.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> upperSymbols = symbols.stream()
                .map(String::toUpperCase)
                .toList();

        return getUpcomingEarnings(days).stream()
                .filter(event -> upperSymbols.contains(event.getSymbol()))
                .toList();
    }

    /**
     * Get past earnings (for surprise analysis)
     * @param days Number of days to look back
     * @return List of past earnings events
     */
    public List<EarningsCalendarEvent> getRecentEarnings(int days) {
        LocalDate today = LocalDate.now();
        return getEarningsCalendar(today.minusDays(days), today);
    }

    /**
     * Get earnings with results (has actual EPS)
     */
    public List<EarningsCalendarEvent> getEarningsWithResults(int daysBack) {
        return getRecentEarnings(daysBack).stream()
                .filter(e -> e.getEpsActual() != null)
                .sorted(Comparator.comparing(EarningsCalendarEvent::getDate).reversed())
                .toList();
    }

    /**
     * Get earnings surprises (beat/miss)
     */
    public List<EarningsCalendarEvent> getEarningsSurprises(int daysBack, boolean beatsOnly) {
        return getEarningsWithResults(daysBack).stream()
                .filter(e -> {
                    Double surprise = e.getEpsSurprisePercent();
                    if (surprise == null) return false;
                    return beatsOnly ? surprise > 0 : surprise < 0;
                })
                .toList();
    }

    /**
     * Format earnings calendar for AI context injection
     */
    public String formatEarningsForContext(List<EarningsCalendarEvent> events, boolean isUpcoming) {
        if (events == null || events.isEmpty()) {
            return isUpcoming
                    ? "No upcoming earnings scheduled."
                    : "No recent earnings data available.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(isUpcoming ? "UPCOMING EARNINGS CALENDAR:\n" : "RECENT EARNINGS RESULTS:\n");
        sb.append("| Date | Symbol | Quarter | EPS Est | EPS Act | Surprise | Timing |\n");
        sb.append("|------|--------|---------|---------|---------|----------|--------|\n");

        events.stream()
                .sorted(Comparator.comparing(e -> e.getDate() != null ? e.getDate() : ""))
                .limit(25)
                .forEach(event -> {
                    sb.append(String.format("| %s | %s | Q%d %d | ",
                            event.getDate() != null ? event.getDate() : "TBD",
                            event.getSymbol(),
                            event.getQuarter() != null ? event.getQuarter() : 0,
                            event.getYear() != null ? event.getYear() : 0));

                    if (event.getEpsEstimate() != null) {
                        sb.append(String.format("$%.2f | ", event.getEpsEstimate()));
                    } else {
                        sb.append("- | ");
                    }

                    if (event.getEpsActual() != null) {
                        sb.append(String.format("$%.2f | ", event.getEpsActual()));
                        Double surprise = event.getEpsSurprisePercent();
                        if (surprise != null) {
                            sb.append(String.format("%+.1f%% | ", surprise));
                        } else {
                            sb.append("- | ");
                        }
                    } else {
                        sb.append("- | - | ");
                    }

                    sb.append(event.getTimingDescription());
                    sb.append(" |\n");
                });

        return sb.toString();
    }

    /**
     * Get summary statistics for earnings
     */
    public String getEarningsSummaryStats(int daysBack) {
        List<EarningsCalendarEvent> withResults = getEarningsWithResults(daysBack);

        if (withResults.isEmpty()) {
            return "No earnings data available for analysis.";
        }

        long beats = withResults.stream()
                .filter(e -> e.getEpsSurprisePercent() != null && e.getEpsSurprisePercent() > 0)
                .count();
        long misses = withResults.stream()
                .filter(e -> e.getEpsSurprisePercent() != null && e.getEpsSurprisePercent() < 0)
                .count();
        long inline = withResults.size() - beats - misses;

        double avgSurprise = withResults.stream()
                .map(EarningsCalendarEvent::getEpsSurprisePercent)
                .filter(s -> s != null)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        return String.format("""
                EARNINGS SUMMARY (Last %d days):
                • Total Reports: %d
                • Beats: %d (%.1f%%)
                • Misses: %d (%.1f%%)
                • In-line: %d
                • Avg Surprise: %+.2f%%
                """,
                daysBack,
                withResults.size(),
                beats, (beats * 100.0 / withResults.size()),
                misses, (misses * 100.0 / withResults.size()),
                inline,
                avgSurprise);
    }
}
