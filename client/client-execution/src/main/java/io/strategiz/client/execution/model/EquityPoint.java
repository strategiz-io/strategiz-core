package io.strategiz.client.execution.model;

/**
 * Represents a point on the equity curve (portfolio value over time).
 */
public class EquityPoint {

	private String timestamp;

	private double portfolioValue;

	private String type; // "initial", "buy", "sell", "final"

	public static EquityPointBuilder builder() {
		return new EquityPointBuilder();
	}

	public static class EquityPointBuilder {

		private String timestamp;

		private double portfolioValue;

		private String type;

		public EquityPointBuilder timestamp(String timestamp) {
			this.timestamp = timestamp;
			return this;
		}

		public EquityPointBuilder portfolioValue(double portfolioValue) {
			this.portfolioValue = portfolioValue;
			return this;
		}

		public EquityPointBuilder type(String type) {
			this.type = type;
			return this;
		}

		public EquityPoint build() {
			EquityPoint point = new EquityPoint();
			point.timestamp = this.timestamp;
			point.portfolioValue = this.portfolioValue;
			point.type = this.type;
			return point;
		}

	}

	public String getTimestamp() {
		return timestamp;
	}

	public double getPortfolioValue() {
		return portfolioValue;
	}

	public String getType() {
		return type;
	}

}
