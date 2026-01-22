package io.strategiz.business.aichat.costs.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a cost-related alert for LLM spending.
 */
public class CostAlert {

	public enum AlertSeverity {

		INFO, WARNING, CRITICAL

	}

	public enum AlertType {

		BUDGET_WARNING, // Approaching monthly budget
		BUDGET_EXCEEDED, // Monthly budget exceeded
		DAILY_THRESHOLD, // Daily spend exceeded threshold
		SPEND_ANOMALY, // Unusual spending pattern
		PROVIDER_ERROR // Failed to fetch costs from provider

	}

	private AlertType type;

	private AlertSeverity severity;

	private String title;

	private String message;

	private BigDecimal currentValue;

	private BigDecimal thresholdValue;

	private String provider; // Optional - specific provider if relevant

	private LocalDateTime timestamp;

	private boolean acknowledged;

	public CostAlert() {
		this.timestamp = LocalDateTime.now();
		this.acknowledged = false;
	}

	public CostAlert(AlertType type, AlertSeverity severity, String title, String message) {
		this();
		this.type = type;
		this.severity = severity;
		this.title = title;
		this.message = message;
	}

	// Static factory methods for common alerts

	public static CostAlert budgetWarning(BigDecimal current, BigDecimal budget, int percent) {
		CostAlert alert = new CostAlert();
		alert.setType(AlertType.BUDGET_WARNING);
		alert.setSeverity(AlertSeverity.WARNING);
		alert.setTitle("Budget Warning");
		alert.setMessage(
				String.format("LLM spending has reached %d%% of monthly budget ($%.2f of $%.2f)", percent, current, budget));
		alert.setCurrentValue(current);
		alert.setThresholdValue(budget);
		return alert;
	}

	public static CostAlert budgetExceeded(BigDecimal current, BigDecimal budget) {
		CostAlert alert = new CostAlert();
		alert.setType(AlertType.BUDGET_EXCEEDED);
		alert.setSeverity(AlertSeverity.CRITICAL);
		alert.setTitle("Budget Exceeded");
		alert.setMessage(String.format("LLM spending ($%.2f) has exceeded monthly budget ($%.2f)", current, budget));
		alert.setCurrentValue(current);
		alert.setThresholdValue(budget);
		return alert;
	}

	public static CostAlert dailyThresholdExceeded(BigDecimal dailySpend, BigDecimal threshold) {
		CostAlert alert = new CostAlert();
		alert.setType(AlertType.DAILY_THRESHOLD);
		alert.setSeverity(AlertSeverity.WARNING);
		alert.setTitle("Daily Spend Alert");
		alert.setMessage(String.format("Today's LLM spending ($%.2f) exceeded daily threshold ($%.2f)", dailySpend, threshold));
		alert.setCurrentValue(dailySpend);
		alert.setThresholdValue(threshold);
		return alert;
	}

	public static CostAlert spendAnomaly(BigDecimal current, BigDecimal average, double multiplier) {
		CostAlert alert = new CostAlert();
		alert.setType(AlertType.SPEND_ANOMALY);
		alert.setSeverity(AlertSeverity.WARNING);
		alert.setTitle("Spending Anomaly Detected");
		alert.setMessage(String.format("Today's spending ($%.2f) is %.1fx higher than the 7-day average ($%.2f)", current,
				multiplier, average));
		alert.setCurrentValue(current);
		alert.setThresholdValue(average);
		return alert;
	}

	public static CostAlert providerError(String provider, String errorMessage) {
		CostAlert alert = new CostAlert();
		alert.setType(AlertType.PROVIDER_ERROR);
		alert.setSeverity(AlertSeverity.INFO);
		alert.setTitle("Provider Billing Error");
		alert.setMessage(String.format("Failed to fetch billing data from %s: %s", provider, errorMessage));
		alert.setProvider(provider);
		return alert;
	}

	// Getters and setters

	public AlertType getType() {
		return type;
	}

	public void setType(AlertType type) {
		this.type = type;
	}

	public AlertSeverity getSeverity() {
		return severity;
	}

	public void setSeverity(AlertSeverity severity) {
		this.severity = severity;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public BigDecimal getCurrentValue() {
		return currentValue;
	}

	public void setCurrentValue(BigDecimal currentValue) {
		this.currentValue = currentValue;
	}

	public BigDecimal getThresholdValue() {
		return thresholdValue;
	}

	public void setThresholdValue(BigDecimal thresholdValue) {
		this.thresholdValue = thresholdValue;
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(LocalDateTime timestamp) {
		this.timestamp = timestamp;
	}

	public boolean isAcknowledged() {
		return acknowledged;
	}

	public void setAcknowledged(boolean acknowledged) {
		this.acknowledged = acknowledged;
	}

}
