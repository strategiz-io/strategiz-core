package io.strategiz.business.aichat.costs.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for LLM billing/cost tracking. Admin API keys are loaded from Vault.
 */
@Configuration
@ConfigurationProperties(prefix = "llm.billing")
public class BillingConfig {

	private boolean enabled = false;

	private OpenAIBillingConfig openai = new OpenAIBillingConfig();

	private AnthropicBillingConfig anthropic = new AnthropicBillingConfig();

	private GcpBillingConfig gcp = new GcpBillingConfig();

	private BudgetConfig budget = new BudgetConfig();

	// Getters and setters

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public OpenAIBillingConfig getOpenai() {
		return openai;
	}

	public void setOpenai(OpenAIBillingConfig openai) {
		this.openai = openai;
	}

	public AnthropicBillingConfig getAnthropic() {
		return anthropic;
	}

	public void setAnthropic(AnthropicBillingConfig anthropic) {
		this.anthropic = anthropic;
	}

	public GcpBillingConfig getGcp() {
		return gcp;
	}

	public void setGcp(GcpBillingConfig gcp) {
		this.gcp = gcp;
	}

	public BudgetConfig getBudget() {
		return budget;
	}

	public void setBudget(BudgetConfig budget) {
		this.budget = budget;
	}

	/**
	 * OpenAI billing configuration
	 */
	public static class OpenAIBillingConfig {

		private boolean enabled = false;

		private String apiUrl = "https://api.openai.com";

		private String adminApiKey; // Loaded from Vault

		private int timeoutSeconds = 30;

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public String getApiUrl() {
			return apiUrl;
		}

		public void setApiUrl(String apiUrl) {
			this.apiUrl = apiUrl;
		}

		public String getAdminApiKey() {
			return adminApiKey;
		}

		public void setAdminApiKey(String adminApiKey) {
			this.adminApiKey = adminApiKey;
		}

		public int getTimeoutSeconds() {
			return timeoutSeconds;
		}

		public void setTimeoutSeconds(int timeoutSeconds) {
			this.timeoutSeconds = timeoutSeconds;
		}

	}

	/**
	 * Anthropic billing configuration
	 */
	public static class AnthropicBillingConfig {

		private boolean enabled = false;

		private String apiUrl = "https://api.anthropic.com";

		private String adminApiKey; // Loaded from Vault

		private int timeoutSeconds = 30;

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public String getApiUrl() {
			return apiUrl;
		}

		public void setApiUrl(String apiUrl) {
			this.apiUrl = apiUrl;
		}

		public String getAdminApiKey() {
			return adminApiKey;
		}

		public void setAdminApiKey(String adminApiKey) {
			this.adminApiKey = adminApiKey;
		}

		public int getTimeoutSeconds() {
			return timeoutSeconds;
		}

		public void setTimeoutSeconds(int timeoutSeconds) {
			this.timeoutSeconds = timeoutSeconds;
		}

	}

	/**
	 * GCP billing configuration (BigQuery)
	 */
	public static class GcpBillingConfig {

		private boolean enabled = false;

		private String billingDataset; // e.g., "project.billing_export.gcp_billing_export_v1"

		private int timeoutSeconds = 60;

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public String getBillingDataset() {
			return billingDataset;
		}

		public void setBillingDataset(String billingDataset) {
			this.billingDataset = billingDataset;
		}

		public int getTimeoutSeconds() {
			return timeoutSeconds;
		}

		public void setTimeoutSeconds(int timeoutSeconds) {
			this.timeoutSeconds = timeoutSeconds;
		}

	}

	/**
	 * Budget and alerting configuration
	 */
	public static class BudgetConfig {

		private boolean alertsEnabled = true;

		private double monthlyBudget = 500.0; // USD

		private double dailyAlertThreshold = 50.0; // USD - alert if daily spend exceeds

		private double anomalyMultiplier = 2.0; // Alert if spend is 2x the 7-day average

		private int budgetWarningPercent = 80; // Warn at 80% of monthly budget

		private int budgetCriticalPercent = 95; // Critical at 95% of monthly budget

		public boolean isAlertsEnabled() {
			return alertsEnabled;
		}

		public void setAlertsEnabled(boolean alertsEnabled) {
			this.alertsEnabled = alertsEnabled;
		}

		public double getMonthlyBudget() {
			return monthlyBudget;
		}

		public void setMonthlyBudget(double monthlyBudget) {
			this.monthlyBudget = monthlyBudget;
		}

		public double getDailyAlertThreshold() {
			return dailyAlertThreshold;
		}

		public void setDailyAlertThreshold(double dailyAlertThreshold) {
			this.dailyAlertThreshold = dailyAlertThreshold;
		}

		public double getAnomalyMultiplier() {
			return anomalyMultiplier;
		}

		public void setAnomalyMultiplier(double anomalyMultiplier) {
			this.anomalyMultiplier = anomalyMultiplier;
		}

		public int getBudgetWarningPercent() {
			return budgetWarningPercent;
		}

		public void setBudgetWarningPercent(int budgetWarningPercent) {
			this.budgetWarningPercent = budgetWarningPercent;
		}

		public int getBudgetCriticalPercent() {
			return budgetCriticalPercent;
		}

		public void setBudgetCriticalPercent(int budgetCriticalPercent) {
			this.budgetCriticalPercent = budgetCriticalPercent;
		}

	}

}
