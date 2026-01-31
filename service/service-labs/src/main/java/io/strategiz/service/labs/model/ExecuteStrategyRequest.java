package io.strategiz.service.labs.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

public class ExecuteStrategyRequest {

	@NotBlank(message = "Symbol is required")
	@JsonProperty("symbol")
	private String symbol;

	@JsonProperty("code")
	private String code; // For direct code execution

	@JsonProperty("language")
	private String language; // For direct code execution

	@JsonProperty("timeframe")
	private String timeframe = "1D"; // 1m, 30m, 1h, 4h, 1D, 1W, 1M

	@JsonProperty("period")
	private String period = "2y"; // 6mo, 1y, 2y, 5y, 7y, max

	@JsonProperty("startDate")
	private String startDate;

	@JsonProperty("endDate")
	private String endDate;

	@JsonProperty("marketData")
	private List<Map<String, Object>> marketData; // OHLCV data

	@JsonProperty("parameters")
	private Map<String, Object> parameters;

	@JsonProperty("providerId")
	private String providerId;

	// Getters and Setters
	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public String getTimeframe() {
		return timeframe;
	}

	public void setTimeframe(String timeframe) {
		this.timeframe = timeframe;
	}

	public String getPeriod() {
		return period;
	}

	public void setPeriod(String period) {
		this.period = period;
	}

	public String getStartDate() {
		return startDate;
	}

	public void setStartDate(String startDate) {
		this.startDate = startDate;
	}

	public String getEndDate() {
		return endDate;
	}

	public void setEndDate(String endDate) {
		this.endDate = endDate;
	}

	public List<Map<String, Object>> getMarketData() {
		return marketData;
	}

	public void setMarketData(List<Map<String, Object>> marketData) {
		this.marketData = marketData;
	}

	public Map<String, Object> getParameters() {
		return parameters;
	}

	public void setParameters(Map<String, Object> parameters) {
		this.parameters = parameters;
	}

	public String getProviderId() {
		return providerId;
	}

	public void setProviderId(String providerId) {
		this.providerId = providerId;
	}

}