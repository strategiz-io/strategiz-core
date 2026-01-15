package io.strategiz.service.console.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * Response model for company fundamentals coverage statistics. Contains aggregate
 * information about fundamentals data completeness.
 */
public class FundamentalsCoverageResponse {

	@JsonProperty("totalRecords")
	private long totalRecords;

	@JsonProperty("uniqueSymbols")
	private int uniqueSymbols;

	@JsonProperty("earliestPeriod")
	private String earliestPeriod; // ISO date string (YYYY-MM-DD)

	@JsonProperty("latestPeriod")
	private String latestPeriod; // ISO date string (YYYY-MM-DD)

	@JsonProperty("yearsOfData")
	private int yearsOfData;

	@JsonProperty("byPeriodType")
	private Map<String, Long> byPeriodType; // QUARTERLY: count, ANNUAL: count, TTM: count

	@JsonProperty("avgRecordsPerSymbol")
	private double avgRecordsPerSymbol;

	@JsonProperty("calculatedAt")
	private String calculatedAt; // ISO 8601 timestamp

	// Constructor
	public FundamentalsCoverageResponse() {
		this.byPeriodType = new HashMap<>();
	}

	// Getters and Setters
	public long getTotalRecords() {
		return totalRecords;
	}

	public void setTotalRecords(long totalRecords) {
		this.totalRecords = totalRecords;
	}

	public int getUniqueSymbols() {
		return uniqueSymbols;
	}

	public void setUniqueSymbols(int uniqueSymbols) {
		this.uniqueSymbols = uniqueSymbols;
	}

	public String getEarliestPeriod() {
		return earliestPeriod;
	}

	public void setEarliestPeriod(String earliestPeriod) {
		this.earliestPeriod = earliestPeriod;
	}

	public String getLatestPeriod() {
		return latestPeriod;
	}

	public void setLatestPeriod(String latestPeriod) {
		this.latestPeriod = latestPeriod;
	}

	public int getYearsOfData() {
		return yearsOfData;
	}

	public void setYearsOfData(int yearsOfData) {
		this.yearsOfData = yearsOfData;
	}

	public Map<String, Long> getByPeriodType() {
		return byPeriodType;
	}

	public void setByPeriodType(Map<String, Long> byPeriodType) {
		this.byPeriodType = byPeriodType;
	}

	public double getAvgRecordsPerSymbol() {
		return avgRecordsPerSymbol;
	}

	public void setAvgRecordsPerSymbol(double avgRecordsPerSymbol) {
		this.avgRecordsPerSymbol = avgRecordsPerSymbol;
	}

	public String getCalculatedAt() {
		return calculatedAt;
	}

	public void setCalculatedAt(String calculatedAt) {
		this.calculatedAt = calculatedAt;
	}

}
