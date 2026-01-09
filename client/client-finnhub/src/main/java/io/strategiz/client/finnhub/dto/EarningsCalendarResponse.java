package io.strategiz.client.finnhub.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Wrapper for Finnhub earnings calendar API response
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EarningsCalendarResponse {

    @JsonProperty("earningsCalendar")
    private List<EarningsCalendarEvent> earningsCalendar;

    public List<EarningsCalendarEvent> getEarningsCalendar() {
        return earningsCalendar;
    }

    public void setEarningsCalendar(List<EarningsCalendarEvent> earningsCalendar) {
        this.earningsCalendar = earningsCalendar;
    }
}
