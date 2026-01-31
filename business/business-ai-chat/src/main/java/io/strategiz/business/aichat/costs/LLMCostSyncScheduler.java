package io.strategiz.business.aichat.costs;

import io.strategiz.business.aichat.costs.model.LLMCostSummary;
import io.strategiz.business.aichat.costs.model.ProviderCostReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Scheduled job that syncs LLM cost data from provider billing APIs. Runs periodically to
 * keep cost data up-to-date for the console dashboard.
 *
 * Schedule: - Daily at 2 AM UTC: Full sync of previous day's costs - Hourly: Quick sync
 * for current day costs
 *
 * Enable with: llm.billing.enabled=true
 */
@Service
@ConditionalOnProperty(name = "llm.billing.enabled", havingValue = "true", matchIfMissing = false)
public class LLMCostSyncScheduler {

	private static final Logger logger = LoggerFactory.getLogger(LLMCostSyncScheduler.class);

	private final LLMCostAggregator costAggregator;

	// Cache for last sync results (simple in-memory cache for dashboard)
	private volatile LLMCostSummary cachedCurrentMonthSummary;

	private volatile long lastSyncTimestamp;

	public LLMCostSyncScheduler(LLMCostAggregator costAggregator) {
		this.costAggregator = costAggregator;
		logger.info("LLMCostSyncScheduler initialized");

		// Trigger initial sync on startup if any providers are enabled
		if (costAggregator.hasBillingEnabled()) {
			syncCurrentDayCosts();
		}
	}

	/**
	 * Daily sync at 2 AM UTC - aggregates previous day's complete costs
	 */
	@Scheduled(cron = "0 0 2 * * *")
	public void syncPreviousDayCosts() {
		logger.info("Starting daily LLM cost sync for previous day");

		if (!costAggregator.hasBillingEnabled()) {
			logger.debug("No billing providers enabled, skipping sync");
			return;
		}

		try {
			LocalDate yesterday = LocalDate.now().minusDays(1);
			LocalDate startOfMonth = yesterday.withDayOfMonth(1);

			// Sync full month-to-date for accurate totals
			List<ProviderCostReport> reports = costAggregator.getCostsByProvider(startOfMonth, yesterday).block();

			if (reports != null) {
				logger.info("Daily sync complete: {} providers synced", reports.size());
				for (ProviderCostReport report : reports) {
					logger.info("  {} - ${} ({} requests, {} tokens)", report.getProvider(), report.getTotalCost(),
							report.getRequestCount(), report.getTotalTokens());
				}
			}

			// Update cached summary
			refreshCachedSummary();
		}
		catch (Exception e) {
			logger.error("Error during daily LLM cost sync", e);
		}
	}

	/**
	 * Hourly sync - quick refresh of current day costs
	 */
	@Scheduled(cron = "0 0 * * * *")
	public void syncCurrentDayCosts() {
		logger.debug("Starting hourly LLM cost sync");

		if (!costAggregator.hasBillingEnabled()) {
			return;
		}

		try {
			refreshCachedSummary();
		}
		catch (Exception e) {
			logger.error("Error during hourly LLM cost sync", e);
		}
	}

	/**
	 * Refresh the cached cost summary
	 */
	private void refreshCachedSummary() {
		try {
			LLMCostSummary summary = costAggregator.getCurrentMonthCosts().block();
			if (summary != null) {
				this.cachedCurrentMonthSummary = summary;
				this.lastSyncTimestamp = System.currentTimeMillis();

				logger.info("LLM cost cache refreshed: ${} total, {} requests, {} providers", summary.getTotalCost(),
						summary.getTotalRequests(), costAggregator.getEnabledProviders().size());
			}
		}
		catch (Exception e) {
			logger.error("Error refreshing LLM cost cache", e);
		}
	}

	/**
	 * Get cached current month summary (for fast dashboard access)
	 * @return Cached summary or null if not yet synced
	 */
	public LLMCostSummary getCachedCurrentMonthSummary() {
		return cachedCurrentMonthSummary;
	}

	/**
	 * Get timestamp of last successful sync
	 * @return Epoch milliseconds of last sync
	 */
	public long getLastSyncTimestamp() {
		return lastSyncTimestamp;
	}

	/**
	 * Force a manual sync (for admin refresh button)
	 * @return Fresh cost summary
	 */
	public LLMCostSummary forceSync() {
		logger.info("Forcing manual LLM cost sync");
		refreshCachedSummary();
		return cachedCurrentMonthSummary;
	}

}
