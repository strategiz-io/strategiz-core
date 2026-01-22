package io.strategiz.business.aichat.costs;

import io.strategiz.business.aichat.costs.config.BillingConfig;
import io.strategiz.business.aichat.costs.model.CostAlert;
import io.strategiz.business.aichat.costs.model.LLMCostSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Service that generates cost alerts based on LLM spending patterns and configured thresholds.
 */
@Service
@ConditionalOnProperty(name = "llm.billing.enabled", havingValue = "true", matchIfMissing = false)
public class LLMCostAlertService {

	private static final Logger logger = LoggerFactory.getLogger(LLMCostAlertService.class);

	private final LLMCostAggregator costAggregator;

	private final BillingConfig.BudgetConfig budgetConfig;

	public LLMCostAlertService(LLMCostAggregator costAggregator, BillingConfig billingConfig) {
		this.costAggregator = costAggregator;
		this.budgetConfig = billingConfig.getBudget();
		logger.info("LLMCostAlertService initialized (alerts enabled: {}, monthly budget: ${})",
				budgetConfig.isAlertsEnabled(), budgetConfig.getMonthlyBudget());
	}

	/**
	 * Generate alerts based on current cost data
	 * @return List of active alerts
	 */
	public Mono<List<CostAlert>> generateAlerts() {
		if (!budgetConfig.isAlertsEnabled()) {
			return Mono.just(new ArrayList<>());
		}

		return costAggregator.getCurrentMonthCosts().map(this::evaluateAlerts).onErrorResume(error -> {
			logger.error("Error generating cost alerts", error);
			List<CostAlert> alerts = new ArrayList<>();
			alerts.add(CostAlert.providerError("all", error.getMessage()));
			return Mono.just(alerts);
		});
	}

	/**
	 * Evaluate the cost summary and generate appropriate alerts
	 */
	private List<CostAlert> evaluateAlerts(LLMCostSummary summary) {
		List<CostAlert> alerts = new ArrayList<>();

		BigDecimal currentSpend = summary.getTotalCost();
		BigDecimal monthlyBudget = BigDecimal.valueOf(budgetConfig.getMonthlyBudget());
		BigDecimal dailyThreshold = BigDecimal.valueOf(budgetConfig.getDailyAlertThreshold());

		// Check monthly budget
		if (monthlyBudget.compareTo(BigDecimal.ZERO) > 0 && currentSpend != null) {
			int percentUsed = currentSpend.multiply(BigDecimal.valueOf(100))
				.divide(monthlyBudget, 0, RoundingMode.HALF_UP)
				.intValue();

			// Budget exceeded
			if (currentSpend.compareTo(monthlyBudget) >= 0) {
				alerts.add(CostAlert.budgetExceeded(currentSpend, monthlyBudget));
				logger.warn("ALERT: Monthly LLM budget exceeded (${} of ${})", currentSpend, monthlyBudget);
			}
			// Critical threshold (95% by default)
			else if (percentUsed >= budgetConfig.getBudgetCriticalPercent()) {
				CostAlert alert = CostAlert.budgetWarning(currentSpend, monthlyBudget, percentUsed);
				alert.setSeverity(CostAlert.AlertSeverity.CRITICAL);
				alert.setTitle("Budget Critical");
				alerts.add(alert);
				logger.warn("ALERT: LLM budget at critical level ({}%)", percentUsed);
			}
			// Warning threshold (80% by default)
			else if (percentUsed >= budgetConfig.getBudgetWarningPercent()) {
				alerts.add(CostAlert.budgetWarning(currentSpend, monthlyBudget, percentUsed));
				logger.info("ALERT: LLM budget warning ({}%)", percentUsed);
			}
		}

		// Check for spending anomaly (comparing to previous period)
		if (summary.getPreviousPeriodCost() != null && summary.getPreviousPeriodCost().compareTo(BigDecimal.ZERO) > 0) {
			BigDecimal previousCost = summary.getPreviousPeriodCost();
			double ratio = currentSpend.divide(previousCost, 2, RoundingMode.HALF_UP).doubleValue();

			if (ratio >= budgetConfig.getAnomalyMultiplier()) {
				alerts.add(CostAlert.spendAnomaly(currentSpend, previousCost, ratio));
				logger.warn("ALERT: Spending anomaly detected ({}x increase)", ratio);
			}
		}

		// Check average cost per request if unusually high
		if (summary.getAverageCostPerRequest() != null) {
			BigDecimal avgCostPerRequest = summary.getAverageCostPerRequest();
			// Alert if average cost per request is over $0.10 (configurable threshold)
			BigDecimal highCostThreshold = BigDecimal.valueOf(0.10);
			if (avgCostPerRequest.compareTo(highCostThreshold) > 0) {
				CostAlert alert = new CostAlert(CostAlert.AlertType.SPEND_ANOMALY, CostAlert.AlertSeverity.INFO,
						"High Cost Per Request",
						String.format("Average cost per request ($%.4f) is higher than expected", avgCostPerRequest));
				alert.setCurrentValue(avgCostPerRequest);
				alert.setThresholdValue(highCostThreshold);
				alerts.add(alert);
			}
		}

		logger.info("Generated {} cost alerts", alerts.size());
		return alerts;
	}

	/**
	 * Get the current budget configuration
	 */
	public BudgetInfo getBudgetInfo() {
		return new BudgetInfo(budgetConfig.getMonthlyBudget(), budgetConfig.getDailyAlertThreshold(),
				budgetConfig.getBudgetWarningPercent(), budgetConfig.getBudgetCriticalPercent(),
				budgetConfig.isAlertsEnabled());
	}

	/**
	 * DTO for budget information
	 */
	public static class BudgetInfo {

		private final double monthlyBudget;

		private final double dailyThreshold;

		private final int warningPercent;

		private final int criticalPercent;

		private final boolean alertsEnabled;

		public BudgetInfo(double monthlyBudget, double dailyThreshold, int warningPercent, int criticalPercent,
				boolean alertsEnabled) {
			this.monthlyBudget = monthlyBudget;
			this.dailyThreshold = dailyThreshold;
			this.warningPercent = warningPercent;
			this.criticalPercent = criticalPercent;
			this.alertsEnabled = alertsEnabled;
		}

		public double getMonthlyBudget() {
			return monthlyBudget;
		}

		public double getDailyThreshold() {
			return dailyThreshold;
		}

		public int getWarningPercent() {
			return warningPercent;
		}

		public int getCriticalPercent() {
			return criticalPercent;
		}

		public boolean isAlertsEnabled() {
			return alertsEnabled;
		}

	}

}
