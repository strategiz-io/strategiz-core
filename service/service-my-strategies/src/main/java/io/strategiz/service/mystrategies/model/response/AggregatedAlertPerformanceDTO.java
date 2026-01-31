package io.strategiz.service.mystrategies.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Aggregated performance metrics across all alert deployments of a strategy.
 */
public class AggregatedAlertPerformanceDTO {

	@JsonProperty("totalSignals")
	private Integer totalSignals;

	@JsonProperty("signalsThisMonth")
	private Integer signalsThisMonth;

	// Getters and Setters
	public Integer getTotalSignals() {
		return totalSignals;
	}

	public void setTotalSignals(Integer totalSignals) {
		this.totalSignals = totalSignals;
	}

	public Integer getSignalsThisMonth() {
		return signalsThisMonth;
	}

	public void setSignalsThisMonth(Integer signalsThisMonth) {
		this.signalsThisMonth = signalsThisMonth;
	}

}
