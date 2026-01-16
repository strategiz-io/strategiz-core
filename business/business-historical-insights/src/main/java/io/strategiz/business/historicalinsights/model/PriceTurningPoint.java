package io.strategiz.business.historicalinsights.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Represents a major price turning point (peak or trough) in historical data.
 * Used by Feeling Lucky mode to give the AI perfect hindsight about optimal entry/exit points.
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

	@Override
	public String toString() {
		return String.format("%s @ %s: $%.2f (%.1f%% from previous, %d days)",
				type, timestamp, price, priceChangeFromPrevious, daysFromPrevious);
	}

}
