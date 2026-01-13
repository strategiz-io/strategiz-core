package io.strategiz.business.strategyanalysis.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Diagnostic information about strategy code structure.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StrategyDiagnostic {

	@JsonProperty("hasEntryRules")
	private boolean hasEntryRules;

	@JsonProperty("hasExitRules")
	private boolean hasExitRules;

	@JsonProperty("hasIndicators")
	private boolean hasIndicators;

	@JsonProperty("hasComplexConditions")
	private boolean hasComplexConditions;

	@JsonProperty("conditionCount")
	private int conditionCount;

	@JsonProperty("crossoverDetected")
	private boolean crossoverDetected;

	@JsonProperty("thresholdComparisons")
	private boolean thresholdComparisons;

	public StrategyDiagnostic() {
	}

	// Getters and setters

	public boolean isHasEntryRules() {
		return hasEntryRules;
	}

	public void setHasEntryRules(boolean hasEntryRules) {
		this.hasEntryRules = hasEntryRules;
	}

	public boolean isHasExitRules() {
		return hasExitRules;
	}

	public void setHasExitRules(boolean hasExitRules) {
		this.hasExitRules = hasExitRules;
	}

	public boolean isHasIndicators() {
		return hasIndicators;
	}

	public void setHasIndicators(boolean hasIndicators) {
		this.hasIndicators = hasIndicators;
	}

	public boolean isHasComplexConditions() {
		return hasComplexConditions;
	}

	public void setHasComplexConditions(boolean hasComplexConditions) {
		this.hasComplexConditions = hasComplexConditions;
	}

	public int getConditionCount() {
		return conditionCount;
	}

	public void setConditionCount(int conditionCount) {
		this.conditionCount = conditionCount;
	}

	public boolean isCrossoverDetected() {
		return crossoverDetected;
	}

	public void setCrossoverDetected(boolean crossoverDetected) {
		this.crossoverDetected = crossoverDetected;
	}

	public boolean isThresholdComparisons() {
		return thresholdComparisons;
	}

	public void setThresholdComparisons(boolean thresholdComparisons) {
		this.thresholdComparisons = thresholdComparisons;
	}

}
