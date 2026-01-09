package io.strategiz.client.finnhub.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

/**
 * DTO representing an SEC filing from Finnhub API
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SECFiling {

    @JsonProperty("accessNumber")
    private String accessNumber;

    @JsonProperty("symbol")
    private String symbol;

    @JsonProperty("cik")
    private String cik;

    @JsonProperty("form")
    private String form; // 10-K, 10-Q, 8-K, etc.

    @JsonProperty("filedDate")
    private String filedDate;

    @JsonProperty("acceptedDate")
    private String acceptedDate;

    @JsonProperty("reportUrl")
    private String reportUrl;

    @JsonProperty("filingUrl")
    private String filingUrl;

    // Getters and setters
    public String getAccessNumber() {
        return accessNumber;
    }

    public void setAccessNumber(String accessNumber) {
        this.accessNumber = accessNumber;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getCik() {
        return cik;
    }

    public void setCik(String cik) {
        this.cik = cik;
    }

    public String getForm() {
        return form;
    }

    public void setForm(String form) {
        this.form = form;
    }

    public String getFiledDate() {
        return filedDate;
    }

    public void setFiledDate(String filedDate) {
        this.filedDate = filedDate;
    }

    public String getAcceptedDate() {
        return acceptedDate;
    }

    public void setAcceptedDate(String acceptedDate) {
        this.acceptedDate = acceptedDate;
    }

    public String getReportUrl() {
        return reportUrl;
    }

    public void setReportUrl(String reportUrl) {
        this.reportUrl = reportUrl;
    }

    public String getFilingUrl() {
        return filingUrl;
    }

    public void setFilingUrl(String filingUrl) {
        this.filingUrl = filingUrl;
    }

    /**
     * Get filed date as LocalDate
     */
    public LocalDate getFiledDateAsLocalDate() {
        return filedDate != null ? LocalDate.parse(filedDate) : null;
    }

    /**
     * Get human-readable form description
     */
    public String getFormDescription() {
        if (form == null) return "Unknown Filing";
        return switch (form.toUpperCase()) {
            case "10-K" -> "Annual Report (10-K)";
            case "10-Q" -> "Quarterly Report (10-Q)";
            case "8-K" -> "Current Report (8-K)";
            case "4" -> "Insider Trading (Form 4)";
            case "SC 13G" -> "Beneficial Ownership (13G)";
            case "SC 13D" -> "Activist Ownership (13D)";
            case "DEF 14A" -> "Proxy Statement";
            case "S-1" -> "IPO Registration";
            case "424B4" -> "Prospectus";
            default -> form;
        };
    }

    /**
     * Check if this is a major filing type
     */
    public boolean isMajorFiling() {
        if (form == null) return false;
        String upperForm = form.toUpperCase();
        return upperForm.equals("10-K") ||
               upperForm.equals("10-Q") ||
               upperForm.equals("8-K") ||
               upperForm.equals("DEF 14A");
    }

    /**
     * Format for AI context injection
     */
    public String toContextString() {
        return String.format("[%s] %s - %s\nFiled: %s | URL: %s",
                symbol != null ? symbol : "N/A",
                getFormDescription(),
                form,
                filedDate != null ? filedDate : "Unknown",
                reportUrl != null ? reportUrl : filingUrl);
    }

    @Override
    public String toString() {
        return "SECFiling{" +
                "symbol='" + symbol + '\'' +
                ", form='" + form + '\'' +
                ", filedDate='" + filedDate + '\'' +
                '}';
    }
}
