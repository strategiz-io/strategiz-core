package io.strategiz.service.agents.dto;

/**
 * DTO for earnings events shown in the Earnings Agent insights panel.
 */
public class EarningsInsightDto {

	private String symbol;

	private String date; // yyyy-MM-dd

	private String timing; // "Before Market Open", "After Market Close", etc.

	private Double epsEstimate;

	private Double epsActual;

	private Long revenueEstimate;

	private Long revenueActual;

	private Integer quarter;

	private Integer year;

	public EarningsInsightDto() {
	}

	public EarningsInsightDto(String symbol, String date, String timing, Double epsEstimate, Double epsActual,
			Long revenueEstimate, Long revenueActual, Integer quarter, Integer year) {
		this.symbol = symbol;
		this.date = date;
		this.timing = timing;
		this.epsEstimate = epsEstimate;
		this.epsActual = epsActual;
		this.revenueEstimate = revenueEstimate;
		this.revenueActual = revenueActual;
		this.quarter = quarter;
		this.year = year;
	}

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

	public String getTiming() {
		return timing;
	}

	public void setTiming(String timing) {
		this.timing = timing;
	}

	public Double getEpsEstimate() {
		return epsEstimate;
	}

	public void setEpsEstimate(Double epsEstimate) {
		this.epsEstimate = epsEstimate;
	}

	public Double getEpsActual() {
		return epsActual;
	}

	public void setEpsActual(Double epsActual) {
		this.epsActual = epsActual;
	}

	public Long getRevenueEstimate() {
		return revenueEstimate;
	}

	public void setRevenueEstimate(Long revenueEstimate) {
		this.revenueEstimate = revenueEstimate;
	}

	public Long getRevenueActual() {
		return revenueActual;
	}

	public void setRevenueActual(Long revenueActual) {
		this.revenueActual = revenueActual;
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

}
