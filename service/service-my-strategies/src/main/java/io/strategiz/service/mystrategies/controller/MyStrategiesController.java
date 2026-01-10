package io.strategiz.service.mystrategies.controller;

import io.strategiz.data.strategy.entity.AlertDeployment;
import io.strategiz.data.strategy.entity.BotDeployment;
import io.strategiz.data.strategy.entity.Strategy;
import io.strategiz.data.strategy.repository.ReadAlertDeploymentRepository;
import io.strategiz.data.strategy.repository.ReadBotDeploymentRepository;
import io.strategiz.data.strategy.repository.ReadStrategyRepository;
import io.strategiz.framework.authorization.annotation.AuthUser;
import io.strategiz.framework.authorization.annotation.RequireAuth;
import io.strategiz.service.mystrategies.model.response.DashboardStatsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;

/**
 * REST controller for My Strategies dashboard.
 * Provides comprehensive statistics across strategies, alerts, and bots.
 */
@RestController
@RequestMapping("/v1/my-strategies")
@Tag(name = "My Strategies", description = "Dashboard statistics and overview for My Strategies page")
public class MyStrategiesController {

	private static final Logger logger = LoggerFactory.getLogger(MyStrategiesController.class);

	private final ReadStrategyRepository readStrategyRepository;

	private final ReadAlertDeploymentRepository readAlertRepository;

	private final ReadBotDeploymentRepository readBotRepository;

	@Autowired
	public MyStrategiesController(ReadStrategyRepository readStrategyRepository,
			ReadAlertDeploymentRepository readAlertRepository, ReadBotDeploymentRepository readBotRepository) {
		this.readStrategyRepository = readStrategyRepository;
		this.readAlertRepository = readAlertRepository;
		this.readBotRepository = readBotRepository;
	}

	/**
	 * GET /v1/my-strategies/dashboard-stats - Get comprehensive dashboard statistics
	 *
	 * Returns overview metrics for the My Strategies page including:
	 * - Strategy counts (created, purchased, published)
	 * - Deployment counts (alerts, bots)
	 * - Signal activity (monthly, weekly, daily)
	 * - Average performance (backtest and live)
	 * - Revenue information
	 * - Subscription tier details
	 */
	@RequireAuth
	@GetMapping("/dashboard-stats")
	@Operation(summary = "Get dashboard statistics",
			description = "Returns comprehensive overview metrics for My Strategies page")
	public ResponseEntity<DashboardStatsResponse> getDashboardStats(@AuthUser String userId) {

		logger.info("Fetching dashboard stats for user: {}", userId);

		DashboardStatsResponse stats = new DashboardStatsResponse();

		// Fetch all user data
		List<Strategy> strategies = readStrategyRepository.findByUserId(userId);
		List<AlertDeployment> alerts = readAlertRepository.findByUserId(userId);
		List<BotDeployment> bots = readBotRepository.findByUserId(userId);

		// Strategy counts
		long created = strategies.stream().filter(s -> s.getOwnerId().equals(userId)).count();
		long purchased = strategies.stream().filter(s -> !s.getOwnerId().equals(userId)).count();
		long published = strategies.stream().filter(Strategy::isPublished).count();

		stats.setTotalStrategies(strategies.size());
		stats.setCreatedStrategies((int) created);
		stats.setPurchasedStrategies((int) purchased);
		stats.setPublishedStrategies((int) published);

		// Deployment counts
		long activeAlerts = alerts.stream().filter(a -> "ACTIVE".equals(a.getStatus())).count();
		long activeBots = bots.stream().filter(b -> "ACTIVE".equals(b.getStatus())).count();

		stats.setTotalAlerts(alerts.size());
		stats.setActiveAlerts((int) activeAlerts);
		stats.setTotalBots(bots.size());
		stats.setActiveBots((int) activeBots);

		// Signal activity (from live performance)
		int alertSignalsMonth = alerts.stream().map(AlertDeployment::getLivePerformance).filter(Objects::nonNull)
				.mapToInt(p -> p.getSignalsThisMonth() != null ? p.getSignalsThisMonth() : 0).sum();

		int alertSignalsWeek = alerts.stream().map(AlertDeployment::getLivePerformance).filter(Objects::nonNull)
				.mapToInt(p -> p.getSignalsThisWeek() != null ? p.getSignalsThisWeek() : 0).sum();

		int alertSignalsToday = alerts.stream().map(AlertDeployment::getLivePerformance).filter(Objects::nonNull)
				.mapToInt(p -> p.getSignalsToday() != null ? p.getSignalsToday() : 0).sum();

		// Bot trades (from existing fields for now, will use livePerformance once
		// populated)
		int botTradesTotal = bots.stream()
				.mapToInt(b -> b.getTotalTrades() != null ? b.getTotalTrades() : 0)
				.sum();

		stats.setTotalSignalsThisMonth(alertSignalsMonth);
		stats.setAlertSignalsThisMonth(alertSignalsMonth);
		stats.setBotTradesThisMonth(0); // TODO: Calculate monthly trades when available
		stats.setSignalsThisWeek(alertSignalsWeek);
		stats.setSignalsToday(alertSignalsToday);

		// Average backtest performance (from Strategy.performance)
		OptionalDouble avgReturn = strategies.stream().map(Strategy::getPerformance).filter(Objects::nonNull)
				.mapToDouble(p -> p.getTotalReturn() != null ? p.getTotalReturn() : 0.0).average();

		OptionalDouble avgWinRate = strategies.stream().map(Strategy::getPerformance).filter(Objects::nonNull)
				.mapToDouble(p -> p.getWinRate() != null ? p.getWinRate() : 0.0).average();

		if (avgReturn.isPresent()) {
			stats.setAvgBacktestReturn(avgReturn.getAsDouble());
		}
		if (avgWinRate.isPresent()) {
			stats.setAvgBacktestWinRate(avgWinRate.getAsDouble());
		}

		// Live bot performance (from BotDeployment.livePerformance)
		OptionalDouble avgLiveReturn = bots.stream().map(BotDeployment::getLivePerformance).filter(Objects::nonNull)
				.mapToDouble(p -> p.getTotalReturn() != null ? p.getTotalReturn() : 0.0).average();

		OptionalDouble avgLiveWinRate = bots.stream().map(BotDeployment::getLivePerformance).filter(Objects::nonNull)
				.mapToDouble(p -> p.getWinRate() != null ? p.getWinRate() : 0.0).average();

		if (avgLiveReturn.isPresent()) {
			stats.setAvgLiveBotReturn(avgLiveReturn.getAsDouble());
		}
		if (avgLiveWinRate.isPresent()) {
			stats.setAvgLiveBotWinRate(avgLiveWinRate.getAsDouble());
		}

		stats.setTotalLiveTrades(botTradesTotal);

		// Revenue calculation
		// TODO: Implement when StrategySubscription repository is available
		stats.setTotalSubscribers(0);
		stats.setMonthlyRevenue(0.0);

		// Subscription tier info
		// TODO: Get from UserSubscription entity
		stats.setSubscriptionTier("FREE");
		stats.setRemainingAlerts(null); // TODO: Calculate based on tier limits
		stats.setRemainingBots(null);

		logger.info("Dashboard stats retrieved successfully for user: {}", userId);

		return ResponseEntity.ok(stats);
	}

}
