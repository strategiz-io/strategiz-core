package io.strategiz.client.execution.model;

import java.util.Map;

/**
 * Signal generated during live strategy execution.
 */
public class LiveSignal {

	private final String signalType;

	private final String symbol;

	private final double price;

	private final int quantity;

	private final String reason;

	private final Map<String, Double> indicators;

	private final String timestamp;

	private LiveSignal(Builder builder) {
		this.signalType = builder.signalType;
		this.symbol = builder.symbol;
		this.price = builder.price;
		this.quantity = builder.quantity;
		this.reason = builder.reason;
		this.indicators = builder.indicators;
		this.timestamp = builder.timestamp;
	}

	public static Builder builder() {
		return new Builder();
	}

	public String getSignalType() {
		return signalType;
	}

	public String getSymbol() {
		return symbol;
	}

	public double getPrice() {
		return price;
	}

	public int getQuantity() {
		return quantity;
	}

	public String getReason() {
		return reason;
	}

	public Map<String, Double> getIndicators() {
		return indicators;
	}

	public String getTimestamp() {
		return timestamp;
	}

	public boolean isBuy() {
		return "BUY".equalsIgnoreCase(signalType);
	}

	public boolean isSell() {
		return "SELL".equalsIgnoreCase(signalType);
	}

	public boolean isHold() {
		return "HOLD".equalsIgnoreCase(signalType);
	}

	public boolean isActionable() {
		return isBuy() || isSell();
	}

	public static class Builder {

		private String signalType;

		private String symbol;

		private double price;

		private int quantity;

		private String reason;

		private Map<String, Double> indicators;

		private String timestamp;

		public Builder signalType(String signalType) {
			this.signalType = signalType;
			return this;
		}

		public Builder symbol(String symbol) {
			this.symbol = symbol;
			return this;
		}

		public Builder price(double price) {
			this.price = price;
			return this;
		}

		public Builder quantity(int quantity) {
			this.quantity = quantity;
			return this;
		}

		public Builder reason(String reason) {
			this.reason = reason;
			return this;
		}

		public Builder indicators(Map<String, Double> indicators) {
			this.indicators = indicators;
			return this;
		}

		public Builder timestamp(String timestamp) {
			this.timestamp = timestamp;
			return this;
		}

		public LiveSignal build() {
			return new LiveSignal(this);
		}

	}

}
