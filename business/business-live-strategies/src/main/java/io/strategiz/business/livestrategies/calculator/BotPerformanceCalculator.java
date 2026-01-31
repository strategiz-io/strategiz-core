package io.strategiz.business.livestrategies.calculator;

import io.strategiz.data.strategy.entity.BotDeployment;
import io.strategiz.data.strategy.entity.BotLivePerformance;
import com.google.cloud.Timestamp;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Calculator for bot live performance metrics. Computes comprehensive trading performance
 * statistics from bot execution data.
 *
 * NOTE: Initial implementation uses aggregated data from BotDeployment. Future
 * enhancement will calculate from detailed trade history when implemented.
 */
@Component
public class BotPerformanceCalculator {

	/**
	 * Calculate live performance from trade execution data
	 *
	 * NOTE: Trade history implementation deferred - this uses aggregated data for now
	 * @param bot The bot deployment entity
	 * @param initialEquity Initial capital allocated to the bot
	 * @return Calculated live performance metrics
	 */
	public BotLivePerformance calculatePerformance(BotDeployment bot, Double initialEquity) {

		BotLivePerformance perf = new BotLivePerformance();

		// Use existing fields from BotDeployment for now
		Integer totalTrades = bot.getTotalTrades();
		Integer profitableTrades = bot.getProfitableTrades();
		Double totalPnL = bot.getTotalPnL();

		if (totalTrades == null || totalTrades == 0) {
			return perf; // No trades yet
		}

		perf.setTotalTrades(totalTrades);
		perf.setProfitableTrades(profitableTrades != null ? profitableTrades : 0);
		perf.setLosingTrades(totalTrades - (profitableTrades != null ? profitableTrades : 0));

		// Calculate return percentage
		if (initialEquity != null && initialEquity > 0 && totalPnL != null) {
			double returnPercent = (totalPnL / initialEquity) * 100.0;
			perf.setTotalReturn(returnPercent);
		}

		perf.setTotalPnL(totalPnL);

		// Win rate
		if (profitableTrades != null && totalTrades > 0) {
			double winRate = ((double) profitableTrades / totalTrades) * 100.0;
			perf.setWinRate(winRate);
		}

		// Deployment timeline
		Timestamp createdDate = bot.getCreatedDate();
		if (createdDate != null) {
			perf.setDeploymentStartDate(Instant.ofEpochSecond(createdDate.getSeconds(), createdDate.getNanos()));
		}

		Timestamp lastExecuted = bot.getLastExecutedAt();
		if (lastExecuted != null) {
			perf.setLastTradeDate(Instant.ofEpochSecond(lastExecuted.getSeconds(), lastExecuted.getNanos()));
		}

		if (createdDate != null) {
			Instant startDate = Instant.ofEpochSecond(createdDate.getSeconds(), createdDate.getNanos());
			long days = ChronoUnit.DAYS.between(startDate, Instant.now());
			perf.setDaysSinceDeployment((int) days);
		}

		// TODO: When trade history is implemented, calculate:
		// - sharpeRatio (requires returns time series)
		// - maxDrawdown (requires equity curve)
		// - profitFactor (requires sum of wins / sum of losses)
		// - avgWin, avgLoss (requires individual trade P&L)
		// - slippage and fees (from trade execution records)

		return perf;
	}

	/**
	 * Calculate advanced metrics from trade history FUTURE IMPLEMENTATION - when
	 * BotTradeHistory entity exists
	 * @param trades List of individual trade records
	 * @param initialEquity Initial capital
	 * @return Comprehensive performance metrics including Sharpe, max DD, etc.
	 */
	public BotLivePerformance calculateFromTradeHistory(Object trades, Double initialEquity) {
		// To be implemented when trade history storage is ready
		throw new UnsupportedOperationException("Trade history analysis not yet implemented");
	}

}
