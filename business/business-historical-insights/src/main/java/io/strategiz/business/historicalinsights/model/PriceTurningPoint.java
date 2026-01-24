package io.strategiz.business.historicalinsights.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Represents a major price turning point (peak or trough) in historical data.
 * Used by Autonomous AI mode to give the AI perfect hindsight about optimal entry/exit points.
 */
public class PriceTurningPoint {

	public enum PointType {
		PEAK,   // Local maximum - optimal SELL point
		TROUGH  // Local minimum - optimal BUY point
	}

	@JsonProperty("type")
	private PointType type;

	@JsonProperty("timestamp")
	private Instant timestamp;

	@JsonProperty("price")
	private double price;

	@JsonProperty("priceChangeFromPrevious")
	private double priceChangeFromPrevious; // % change from previous turning point

	@JsonProperty("daysFromPrevious")
	private int daysFromPrevious; // Days since previous turning point

	// Indicator states at this turning point (what would have signaled it)
	@JsonProperty("rsiAtPoint")
	private double rsiAtPoint;

	@JsonProperty("macdHistogramAtPoint")
	private double macdHistogramAtPoint;

	@JsonProperty("stochasticKAtPoint")
	private double stochasticKAtPoint;

	@JsonProperty("pctFromBollingerLower")
	private double pctFromBollingerLower; // % distance from lower BB

	@JsonProperty("pctFromBollingerUpper")
	private double pctFromBollingerUpper; // % distance from upper BB

	@JsonProperty("volumeRatio")
	private double volumeRatio; // Volume vs 20-day average

	@JsonProperty("volumeConfirmed")
	private boolean volumeConfirmed; // True if volume > 1.5x average

	// Reversal analysis - how the price moved after this turning point
	@JsonProperty("reversal5DayPercent")
	private double reversal5DayPercent;

	@JsonProperty("reversal10DayPercent")
	private double reversal10DayPercent;

	public PriceTurningPoint() {
	}

	public PriceTurningPoint(PointType type, Instant timestamp, double price) {
		this.type = type;
		this.timestamp = timestamp;
		this.price = price;
	}

	// Getters and Setters

	public PointType getType() {
		return type;
	}

	public void setType(PointType type) {
		this.type = type;
	}

	public Instant getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Instant timestamp) {
		this.timestamp = timestamp;
	}

	public double getPrice() {
		return price;
	}

	public void setPrice(double price) {
		this.price = price;
	}

	public double getPriceChangeFromPrevious() {
		return priceChangeFromPrevious;
	}

	public void setPriceChangeFromPrevious(double priceChangeFromPrevious) {
		this.priceChangeFromPrevious = priceChangeFromPrevious;
	}

	public int getDaysFromPrevious() {
		return daysFromPrevious;
	}

	public void setDaysFromPrevious(int daysFromPrevious) {
		this.daysFromPrevious = daysFromPrevious;
	}

	// Getters and Setters for indicator states

	public double getRsiAtPoint() {
		return rsiAtPoint;
	}

	public void setRsiAtPoint(double rsiAtPoint) {
		this.rsiAtPoint = rsiAtPoint;
	}

	public double getMacdHistogramAtPoint() {
		return macdHistogramAtPoint;
	}

	public void setMacdHistogramAtPoint(double macdHistogramAtPoint) {
		this.macdHistogramAtPoint = macdHistogramAtPoint;
	}

	public double getStochasticKAtPoint() {
		return stochasticKAtPoint;
	}

	public void setStochasticKAtPoint(double stochasticKAtPoint) {
		this.stochasticKAtPoint = stochasticKAtPoint;
	}

	public double getPctFromBollingerLower() {
		return pctFromBollingerLower;
	}

	public void setPctFromBollingerLower(double pctFromBollingerLower) {
		this.pctFromBollingerLower = pctFromBollingerLower;
	}

	public double getPctFromBollingerUpper() {
		return pctFromBollingerUpper;
	}

	public void setPctFromBollingerUpper(double pctFromBollingerUpper) {
		this.pctFromBollingerUpper = pctFromBollingerUpper;
	}

	public double getVolumeRatio() {
		return volumeRatio;
	}

	public void setVolumeRatio(double volumeRatio) {
		this.volumeRatio = volumeRatio;
	}

	public boolean isVolumeConfirmed() {
		return volumeConfirmed;
	}

	public void setVolumeConfirmed(boolean volumeConfirmed) {
		this.volumeConfirmed = volumeConfirmed;
	}

	public double getReversal5DayPercent() {
		return reversal5DayPercent;
	}

	public void setReversal5DayPercent(double reversal5DayPercent) {
		this.reversal5DayPercent = reversal5DayPercent;
	}

	public double getReversal10DayPercent() {
		return reversal10DayPercent;
	}

	public void setReversal10DayPercent(double reversal10DayPercent) {
		this.reversal10DayPercent = reversal10DayPercent;
	}

	@Override
	public String toString() {
		return String.format("%s @ %s: $%.2f (%.1f%% from previous, %d days, RSI=%.1f)",
				type, timestamp, price, priceChangeFromPrevious, daysFromPrevious, rsiAtPoint);
	}

}
