package io.strategiz.client.execution.model;

public class DataPoint {

	private String timestamp;

	private double value;

	public static DataPointBuilder builder() {
		return new DataPointBuilder();
	}

	public static class DataPointBuilder {

		private String timestamp;

		private double value;

		public DataPointBuilder timestamp(String timestamp) {
			this.timestamp = timestamp;
			return this;
		}

		public DataPointBuilder value(double value) {
			this.value = value;
			return this;
		}

		public DataPoint build() {
			DataPoint point = new DataPoint();
			point.timestamp = this.timestamp;
			point.value = this.value;
			return point;
		}

	}

	public String getTimestamp() {
		return timestamp;
	}

	public double getValue() {
		return value;
	}

}
