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
import io.strategiz.service.mystrategies.model.response.StrategyWithDeploymentsResponse;
import io.strategiz.service.mystrategies.model.response.DeploymentsDTO;
import io.strategiz.service.mystrategies.model.response.DeploymentStatsDTO;
import io.strategiz.service.mystrategies.model.response.AlertDeploymentDTO;
import io.strategiz.service.mystrategies.model.response.BotDeploymentDTO;
import io.strategiz.service.mystrategies.model.response.AggregatedAlertPerformanceDTO;
import io.strategiz.service.mystrategies.model.response.AggregatedBotPerformanceDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

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

	/**
	 * GET /v1/my-strategies/strategies-with-deployments - Get all strategies with their deployments
	 *
	 * Returns a strategy-centric view where each strategy includes:
	 * - Core strategy information
	 * - Embedded alerts and bots (only user's own deployments)
	 * - Aggregated deployment statistics
	 * - Performance metrics
	 *
	 * Privacy: Deployments are filtered by userId - users only see their own alerts/bots,
	 * never those belonging to other users who may have deployed the same strategy.
	 */
	@RequireAuth
	@GetMapping("/strategies-with-deployments")
	@Operation(summary = "Get strategies with deployments",
			description = "Returns all user strategies with embedded deployment information for strategy-centric My Strategies page")
	public ResponseEntity<List<StrategyWithDeploymentsResponse>> getStrategiesWithDeployments(@AuthUser String userId) {

		logger.info("Fetching strategies with deployments for user: {}", userId);

		try {
			// Fetch all user data
			List<Strategy> strategies = readStrategyRepository.findByUserId(userId);
			List<AlertDeployment> allAlerts = readAlertRepository.findByUserId(userId);
			List<BotDeployment> allBots = readBotRepository.findByUserId(userId);

			// Build response list
			List<StrategyWithDeploymentsResponse> responses = new ArrayList<>();

			for (Strategy strategy : strategies) {
				StrategyWithDeploymentsResponse response = new StrategyWithDeploymentsResponse();

				// Core strategy info
				response.setId(strategy.getId());
				response.setName(strategy.getName());
				response.setDescription(strategy.getDescription());
				response.setCreatedAt(strategy.getCreatedDate() != null ? strategy.getCreatedDate().toString() : null);
				response.setUpdatedAt(strategy.getModifiedDate() != null ? strategy.getModifiedDate().toString() : null);

				// Ownership info
				response.setUserId(userId);
				response.setOwnerId(strategy.getOwnerId());
				response.setCreatorId(strategy.getCreatorId());
				response.setIsOwner(userId.equals(strategy.getOwnerId()));
				response.setIsPurchased(!userId.equals(strategy.getCreatorId()) && !userId.equals(strategy.getOwnerId()));

				// Publishing info
				response.setIsPublished(strategy.isPublished());
				if (strategy.getPricing() != null) {
					response.setPricingType(strategy.getPricing().getPricingType().name());
					response.setPrice(strategy.getPricing().getEffectivePrice() != null ?
							strategy.getPricing().getEffectivePrice().doubleValue() : null);
				}

				// Performance
				response.setPerformance(strategy.getPerformance());

				// Metadata
				response.setTags(strategy.getTags());
				response.setLanguage(strategy.getLanguage());

				// Filter alerts and bots for this strategy (privacy: only user's deployments)
				List<AlertDeployment> strategyAlerts = allAlerts.stream()
						.filter(a -> strategy.getId().equals(a.getStrategyId()))
						.collect(Collectors.toList());

				List<BotDeployment> strategyBots = allBots.stream()
						.filter(b -> strategy.getId().equals(b.getStrategyId()))
						.collect(Collectors.toList());

				// Convert to DTOs
				DeploymentsDTO deployments = new DeploymentsDTO();
				deployments.setAlerts(convertToAlertDTOs(strategyAlerts));
				deployments.setBots(convertToBotDTOs(strategyBots));
				response.setDeployments(deployments);

				// Calculate deployment stats
				DeploymentStatsDTO stats = calculateDeploymentStats(strategyAlerts, strategyBots);
				response.setDeploymentStats(stats);

				responses.add(response);
			}

			logger.info("Retrieved {} strategies with deployments for user: {}", responses.size(), userId);
			return ResponseEntity.ok(responses);

		} catch (Exception e) {
			logger.error("Failed to fetch strategies with deployments for user: {}", userId, e);
			// Return empty list on error rather than failing the entire request
			return ResponseEntity.ok(new ArrayList<>());
		}
	}

	// Helper methods for conversion and aggregation

	private List<AlertDeploymentDTO> convertToAlertDTOs(List<AlertDeployment> alerts) {
		return alerts.stream().map(alert -> {
			AlertDeploymentDTO dto = new AlertDeploymentDTO();
			dto.setId(alert.getId());
			dto.setStrategyId(alert.getStrategyId());
			dto.setAlertName(alert.getAlertName());
			dto.setSymbols(alert.getSymbols());
			dto.setStatus(alert.getStatus());
			dto.setTriggerCount(alert.getTriggerCount());
			dto.setLastTriggeredAt(alert.getLastTriggeredAt());
			dto.setDeployedAt(alert.getCreatedDate());
			dto.setNotificationChannels(alert.getNotificationChannels());
			dto.setLivePerformance(alert.getLivePerformance());
			return dto;
		}).collect(Collectors.toList());
	}

	private List<BotDeploymentDTO> convertToBotDTOs(List<BotDeployment> bots) {
		return bots.stream().map(bot -> {
			BotDeploymentDTO dto = new BotDeploymentDTO();
			dto.setId(bot.getId());
			dto.setStrategyId(bot.getStrategyId());
			dto.setBotName(bot.getBotName());
			dto.setSymbols(bot.getSymbols());
			dto.setStatus(bot.getStatus());
			dto.setEnvironment(bot.getEnvironment());
			dto.setDeployedAt(bot.getCreatedDate());
			dto.setLivePerformance(bot.getLivePerformance());
			return dto;
		}).collect(Collectors.toList());
	}

	private DeploymentStatsDTO calculateDeploymentStats(List<AlertDeployment> alerts, List<BotDeployment> bots) {
		DeploymentStatsDTO stats = new DeploymentStatsDTO();

		// Alert counts
		stats.setTotalAlerts(alerts.size());
		long activeAlerts = alerts.stream().filter(a -> "ACTIVE".equals(a.getStatus())).count();
		stats.setActiveAlerts((int) activeAlerts);

		// Bot counts
		stats.setTotalBots(bots.size());
		long activeBots = bots.stream().filter(b -> "ACTIVE".equals(b.getStatus())).count();
		stats.setActiveBots((int) activeBots);

		// Aggregated alert performance
		if (!alerts.isEmpty()) {
			AggregatedAlertPerformanceDTO alertPerf = new AggregatedAlertPerformanceDTO();

			int totalSignals = alerts.stream()
					.map(AlertDeployment::getLivePerformance)
					.filter(Objects::nonNull)
					.mapToInt(p -> p.getTotalSignals() != null ? p.getTotalSignals() : 0)
					.sum();

			int signalsThisMonth = alerts.stream()
					.map(AlertDeployment::getLivePerformance)
					.filter(Objects::nonNull)
					.mapToInt(p -> p.getSignalsThisMonth() != null ? p.getSignalsThisMonth() : 0)
					.sum();

			alertPerf.setTotalSignals(totalSignals);
			alertPerf.setSignalsThisMonth(signalsThisMonth);
			stats.setAggregatedAlertPerformance(alertPerf);
		}

		// Aggregated bot performance
		if (!bots.isEmpty()) {
			AggregatedBotPerformanceDTO botPerf = new AggregatedBotPerformanceDTO();

			// Average return
			OptionalDouble avgReturn = bots.stream()
					.map(BotDeployment::getLivePerformance)
					.filter(Objects::nonNull)
					.mapToDouble(p -> p.getTotalReturn() != null ? p.getTotalReturn() : 0.0)
					.average();

			// Sum of P&L
			double totalPnL = bots.stream()
					.map(BotDeployment::getLivePerformance)
					.filter(Objects::nonNull)
					.mapToDouble(p -> p.getTotalPnL() != null ? p.getTotalPnL() : 0.0)
					.sum();

			// Average win rate
			OptionalDouble avgWinRate = bots.stream()
					.map(BotDeployment::getLivePerformance)
					.filter(Objects::nonNull)
					.mapToDouble(p -> p.getWinRate() != null ? p.getWinRate() : 0.0)
					.average();

			// Sum of total trades
			int totalTrades = bots.stream()
					.map(BotDeployment::getLivePerformance)
					.filter(Objects::nonNull)
					.mapToInt(p -> p.getTotalTrades() != null ? p.getTotalTrades() : 0)
					.sum();

			botPerf.setTotalReturn(avgReturn.isPresent() ? avgReturn.getAsDouble() : null);
			botPerf.setTotalPnL(totalPnL);
			botPerf.setAvgWinRate(avgWinRate.isPresent() ? avgWinRate.getAsDouble() : null);
			botPerf.setTotalTrades(totalTrades);
			stats.setAggregatedBotPerformance(botPerf);
		}

		return stats;
	}

}
