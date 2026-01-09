package io.strategiz.client.finnhub.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

/**
 * DTO representing an earnings calendar event from Finnhub API
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EarningsCalendarEvent {

    @JsonProperty("symbol")
    private String symbol;

    @JsonProperty("date")
    private String date;

    @JsonProperty("epsActual")
    private Double epsActual;

    @JsonProperty("epsEstimate")
    private Double epsEstimate;

    @JsonProperty("revenueActual")
    private Long revenueActual;

    @JsonProperty("revenueEstimate")
    private Long revenueEstimate;

    @JsonProperty("hour")
    private String hour; // "bmo" (before market open), "amc" (after market close), "dmh" (during market hours)

    @JsonProperty("quarter")
    private Integer quarter;

    @JsonProperty("year")
    private Integer year;

    // Getters and setters
    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public Double getEpsActual() {
        return epsActual;
    }

    public void setEpsActual(Double epsActual) {
        this.epsActual = epsActual;
    }

    public Double getEpsEstimate() {
        return epsEstimate;
    }

    public void setEpsEstimate(Double epsEstimate) {
        this.epsEstimate = epsEstimate;
    }

    public Long getRevenueActual() {
        return revenueActual;
    }

    public void setRevenueActual(Long revenueActual) {
        this.revenueActual = revenueActual;
    }

    public Long getRevenueEstimate() {
        return revenueEstimate;
    }

    public void setRevenueEstimate(Long revenueEstimate) {
        this.revenueEstimate = revenueEstimate;
    }

    public String getHour() {
        return hour;
    }

    public void setHour(String hour) {
        this.hour = hour;
    }

    public Integer getQuarter() {
        return quarter;
    }

    public void setQuarter(Integer quarter) {
        this.quarter = quarter;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    /**
     * Get date as LocalDate
     */
    public LocalDate getDateAsLocalDate() {
        return date != null ? LocalDate.parse(date) : null;
    }

    /**
     * Calculate EPS surprise percentage
     */
    public Double getEpsSurprisePercent() {
        if (epsActual == null || epsEstimate == null || epsEstimate == 0) {
            return null;
        }
        return ((epsActual - epsEstimate) / Math.abs(epsEstimate)) * 100;
    }

    /**
     * Get human-readable timing
     */
    public String getTimingDescription() {
        if (hour == null) return "Unknown";
        return switch (hour.toLowerCase()) {
            case "bmo" -> "Before Market Open";
            case "amc" -> "After Market Close";
            case "dmh" -> "During Market Hours";
            default -> hour;
        };
    }

    /**
     * Format for AI context injection
     */
    public String toContextString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("| %s | %s | Q%d %d | ",
                date != null ? date : "TBD",
                symbol,
                quarter != null ? quarter : 0,
                year != null ? year : 0));

        if (epsEstimate != null) {
            sb.append(String.format("Est: $%.2f", epsEstimate));
        }
        if (epsActual != null) {
            sb.append(String.format(" | Act: $%.2f", epsActual));
            Double surprise = getEpsSurprisePercent();
            if (surprise != null) {
                sb.append(String.format(" (%+.1f%%)", surprise));
            }
        }
        sb.append(" | ").append(getTimingDescription());

        return sb.toString();
    }

    @Override
    public String toString() {
        return "EarningsCalendarEvent{" +
                "symbol='" + symbol + '\'' +
                ", date='" + date + '\'' +
                ", epsEstimate=" + epsEstimate +
                ", epsActual=" + epsActual +
                ", hour='" + hour + '\'' +
                '}';
    }
}
