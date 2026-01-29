package io.strategiz.client.fmp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO representing an earnings calendar event from FMP API.
 *
 * <p>
 * FMP Endpoint: GET /api/v3/earning_calendar?from={date}&to={date}&apikey={key}
 * </p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FmpEarningsEvent {

	@JsonProperty("date")
	private String date;

	@JsonProperty("symbol")
	private String symbol;

	@JsonProperty("eps")
	private Double epsActual;

	@JsonProperty("epsEstimated")
	private Double epsEstimated;

	@JsonProperty("time")
	private String time; // "bmo", "amc", "dmh"

	@JsonProperty("revenue")
	private Long revenueActual;

	@JsonProperty("revenueEstimated")
	private Long revenueEstimated;

	@JsonProperty("fiscalDateEnding")
	private String fiscalDateEnding;

	public FmpEarningsEvent() {
	}

	// Getters and setters

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public Double getEpsActual() {
		return epsActual;
	}

	public void setEpsActual(Double epsActual) {
		this.epsActual = epsActual;
	}

	public Double getEpsEstimated() {
		return epsEstimated;
	}

	public void setEpsEstimated(Double epsEstimated) {
		this.epsEstimated = epsEstimated;
	}

	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}

	public Long getRevenueActual() {
		return revenueActual;
	}

	public void setRevenueActual(Long revenueActual) {
		this.revenueActual = revenueActual;
	}

	public Long getRevenueEstimated() {
		return revenueEstimated;
	}

	public void setRevenueEstimated(Long revenueEstimated) {
		this.revenueEstimated = revenueEstimated;
	}

	public String getFiscalDateEnding() {
		return fiscalDateEnding;
	}

	public void setFiscalDateEnding(String fiscalDateEnding) {
		this.fiscalDateEnding = fiscalDateEnding;
	}

	/**
	 * Get human-readable timing description.
	 */
	public String getTimingDescription() {
		if (time == null) {
			return "TBD";
		}
		return switch (time.toLowerCase()) {
			case "bmo" -> "Before Market Open";
			case "amc" -> "After Market Close";
			case "dmh" -> "During Market Hours";
			default -> time;
		};
	}

	/**
	 * Get short timing label for display.
	 */
	public String getTimingShort() {
		if (time == null) {
			return "TBD";
		}
		return switch (time.toLowerCase()) {
			case "bmo" -> "BMO";
			case "amc" -> "AMC";
			case "dmh" -> "DMH";
			default -> time.toUpperCase();
		};
	}

}
