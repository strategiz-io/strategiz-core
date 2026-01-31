package io.strategiz.client.execution.model;

import java.util.List;

public class Indicator {

	private String name;

	private List<DataPoint> data;

	public static IndicatorBuilder builder() {
		return new IndicatorBuilder();
	}

	public static class IndicatorBuilder {

		private String name;

		private List<DataPoint> data;

		public IndicatorBuilder name(String name) {
			this.name = name;
			return this;
		}

		public IndicatorBuilder data(List<DataPoint> data) {
			this.data = data;
			return this;
		}

		public Indicator build() {
			Indicator indicator = new Indicator();
			indicator.name = this.name;
			indicator.data = this.data;
			return indicator;
		}

	}

	public String getName() {
		return name;
	}

	public List<DataPoint> getData() {
		return data;
	}

}
