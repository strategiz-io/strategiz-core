package io.strategiz.client.alpaca.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * DTO representing the paginated response from Alpaca's Bars API
 *
 * Alpaca API Response Format:
 * {
 *   "bars": [ {...}, {...}, ... ],
 *   "symbol": "AAPL",
 *   "next_page_token": "MTYwNTExNj..."  // null if no more pages
 * }
 */
public class AlpacaBarsResponse {

    /**
     * List of bar data points
     */
    @JsonProperty("bars")
    private List<AlpacaBar> bars;

    /**
     * Symbol for these bars
     */
    @JsonProperty("symbol")
    private String symbol;

    /**
     * Pagination token for next page (null if no more data)
     */
    @JsonProperty("next_page_token")
    private String nextPageToken;

    // Constructors
    public AlpacaBarsResponse() {
    }

    public AlpacaBarsResponse(List<AlpacaBar> bars, String symbol, String nextPageToken) {
        this.bars = bars;
        this.symbol = symbol;
        this.nextPageToken = nextPageToken;
    }

    // Getters and Setters
    public List<AlpacaBar> getBars() {
        return bars;
    }

    public void setBars(List<AlpacaBar> bars) {
        this.bars = bars;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getNextPageToken() {
        return nextPageToken;
    }

    public void setNextPageToken(String nextPageToken) {
        this.nextPageToken = nextPageToken;
    }

    /**
     * Check if there are more pages to fetch
     */
    public boolean hasNextPage() {
        return nextPageToken != null && !nextPageToken.isEmpty();
    }

    @Override
    public String toString() {
        return String.format("AlpacaBarsResponse[symbol=%s, bars=%d, hasNextPage=%b]",
            symbol, bars != null ? bars.size() : 0, hasNextPage());
    }
}
