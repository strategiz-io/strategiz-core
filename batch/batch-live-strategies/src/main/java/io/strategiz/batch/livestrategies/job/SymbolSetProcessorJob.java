package io.strategiz.batch.livestrategies.job;

import io.strategiz.batch.livestrategies.model.DeploymentBatchMessage;
import io.strategiz.business.livestrategies.adapter.SignalAdapter;
import io.strategiz.business.livestrategies.model.Signal;
import io.strategiz.business.livestrategies.model.SymbolSetGroup;
import io.strategiz.client.execution.ExecutionServiceClient;
import io.strategiz.client.execution.ExecutionServiceClient.DeploymentExecutionData;
import io.strategiz.client.execution.ExecutionServiceClient.SymbolSetExecutionData;
import io.strategiz.client.execution.model.DeploymentResult;
import io.strategiz.client.execution.model.ExecuteListResponse;
import io.strategiz.client.execution.model.LiveSignal;
import io.strategiz.execution.grpc.MarketDataBar;
import io.strategiz.data.marketdata.entity.MarketDataEntity;
import io.strategiz.data.marketdata.clickhouse.repository.MarketDataClickHouseRepository;
import io.strategiz.data.strategy.entity.AlertDeployment;
import io.strategiz.data.strategy.entity.BotDeployment;
import io.strategiz.data.strategy.entity.Strategy;
import io.strategiz.data.strategy.repository.ReadAlertDeploymentRepository;
import io.strategiz.data.strategy.repository.ReadBotDeploymentRepository;
import io.strategiz.data.strategy.repository.ReadStrategyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Processes deployment batches received from Pub/Sub.
 *
 * This job:
 * 1. Receives a batch of symbol sets with alert/bot IDs
 * 2. Fetches market data for all unique symbols
 * 3. Loads strategy code for each deployment
 * 4. Calls gRPC ExecuteList to evaluate all strategies
 * 5. Routes signals to appropriate adapters (Alert or Bot)
 *
 * Requires ClickHouse to be enabled for market data access.
 */
@Component
@ConditionalOnProperty(name = "strategiz.clickhouse.enabled", havingValue = "true")
public class SymbolSetProcessorJob {

	private static final Logger log = LoggerFactory.getLogger(SymbolSetProcessorJob.class);

	private final ReadAlertDeploymentRepository alertRepository;

	private final ReadBotDeploymentRepository botRepository;

	private final ReadStrategyRepository strategyRepository;

	private final List<SignalAdapter> signalAdapters;

	private final ExecutionServiceClient executionServiceClient;

	private final MarketDataClickHouseRepository marketDataRepository;

	@Value("${live-strategies.market-data.lookback-days:365}")
	private int marketDataLookbackDays;

	@Value("${live-strategies.execution.timeout-seconds:30}")
	private int executionTimeoutSeconds;

	private final ExecutorService signalExecutor = Executors.newFixedThreadPool(10);

	@Autowired
	public SymbolSetProcessorJob(ReadAlertDeploymentRepository alertRepository,
			ReadBotDeploymentRepository botRepository, ReadStrategyRepository strategyRepository,
			List<SignalAdapter> signalAdapters, ExecutionServiceClient executionServiceClient,
			MarketDataClickHouseRepository marketDataRepository) {
		this.alertRepository = alertRepository;
		this.botRepository = botRepository;
		this.strategyRepository = strategyRepository;
		this.signalAdapters = signalAdapters != null ? signalAdapters : new ArrayList<>();
		this.executionServiceClient = executionServiceClient;
		this.marketDataRepository = marketDataRepository;
		log.info("SymbolSetProcessorJob initialized with {} signal adapters, gRPC client={}", this.signalAdapters.size(),
				executionServiceClient != null);
	}

	/**
	 * Process a deployment batch message from Pub/Sub.
	 * @param message The batch message containing symbol sets with alert/bot IDs
	 * @return Processing result with statistics
	 */
	public ProcessingResult process(DeploymentBatchMessage message) {
		Instant startTime = Instant.now();
		String messageId = message.getMessageId();

		log.info("Processing batch {}: tier={}, symbolSets={}, alerts={}, bots={}", messageId, message.getTier(),
				message.getSymbolSets().size(), message.getTotalAlerts(), message.getTotalBots());

		int signalsGenerated = 0;
		int alertsTriggered = 0;
		int botsTriggered = 0;
		int errors = 0;

		try {
			// 1. Collect all unique symbols for market data fetch
			Set<String> allSymbols = collectAllSymbols(message.getSymbolSets());
			log.debug("Collected {} unique symbols from {} symbol sets", allSymbols.size(),
					message.getSymbolSets().size());

			// 2. Fetch market data for all symbols (from ClickHouse)
			Map<String, List<MarketDataBar>> marketData = fetchMarketData(allSymbols);
			log.debug("Fetched market data for {} symbols", marketData.size());

			// 3. Load all deployments and strategies
			Map<String, AlertDeployment> alerts = loadAlerts(message.getSymbolSets());
			Map<String, BotDeployment> bots = loadBots(message.getSymbolSets());
			Map<String, Strategy> strategies = loadStrategies(alerts, bots);
			log.debug("Loaded {} alerts, {} bots, {} strategies", alerts.size(), bots.size(), strategies.size());

			// 4. Call gRPC ExecuteList to evaluate all strategies
			List<SignalResult> results = executeStrategies(message.getSymbolSets(), alerts, bots, strategies,
					marketData, message.getTier());
			signalsGenerated = results.size();

			// 5. Route signals to adapters (async for better throughput)
			List<CompletableFuture<Void>> futures = new ArrayList<>();
			for (SignalResult result : results) {
				if (result.signal() != null && result.signal().getType() != Signal.Type.HOLD) {
					CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
						try {
							routeSignal(result);
						}
						catch (Exception e) {
							log.error("Error routing signal for deployment {}: {}", result.deploymentId(),
									e.getMessage());
						}
					}, signalExecutor);
					futures.add(future);
				}
			}

			// Wait for all signal routing to complete
			CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

			// Count triggered alerts/bots
			for (SignalResult result : results) {
				if (result.signal() != null && result.signal().getType() != Signal.Type.HOLD) {
					if ("ALERT".equals(result.deploymentType())) {
						alertsTriggered++;
					}
					else {
						botsTriggered++;
					}
				}
			}

		}
		catch (Exception e) {
			log.error("Error processing batch {}: {}", messageId, e.getMessage(), e);
			errors++;
		}

		Duration duration = Duration.between(startTime, Instant.now());
		log.info("Completed batch {} in {}ms: signals={}, alertsTriggered={}, botsTriggered={}, errors={}", messageId,
				duration.toMillis(), signalsGenerated, alertsTriggered, botsTriggered, errors);

		return new ProcessingResult(messageId, message.getTier(), message.getSymbolSets().size(), signalsGenerated,
				alertsTriggered, botsTriggered, errors, duration.toMillis());
	}

	/**
	 * Collect all unique symbols from symbol sets.
	 */
	private Set<String> collectAllSymbols(List<SymbolSetGroup> symbolSets) {
		Set<String> symbols = new HashSet<>();
		for (SymbolSetGroup group : symbolSets) {
			symbols.addAll(group.getSymbols());
		}
		return symbols;
	}

	/**
	 * Fetch market data for all symbols from ClickHouse.
	 * Returns OHLCV bars for each symbol, converted to gRPC MarketDataBar format.
	 */
	private Map<String, List<MarketDataBar>> fetchMarketData(Set<String> symbols) {
		Map<String, List<MarketDataBar>> marketData = new HashMap<>();
		Instant endTime = Instant.now();
		Instant startTime = endTime.minus(marketDataLookbackDays, ChronoUnit.DAYS);

		for (String symbol : symbols) {
			try {
				// Use 1D timeframe for live strategy execution
				List<MarketDataEntity> bars = marketDataRepository.findBySymbolAndTimeRange(symbol,
						startTime, endTime, "1D");

				List<MarketDataBar> grpcBars = new ArrayList<>();
				for (MarketDataEntity bar : bars) {
					grpcBars.add(MarketDataBar.newBuilder()
							.setTimestamp(bar.getTimestamp().toString())
							.setOpen(bar.getOpen().doubleValue())
							.setHigh(bar.getHigh().doubleValue())
							.setLow(bar.getLow().doubleValue())
							.setClose(bar.getClose().doubleValue())
							.setVolume(bar.getVolume().longValue())
							.build());
				}

				marketData.put(symbol, grpcBars);
				log.debug("Fetched {} bars for symbol {}", grpcBars.size(), symbol);
			}
			catch (Exception e) {
				log.warn("Failed to fetch market data for symbol {}: {}", symbol, e.getMessage());
				marketData.put(symbol, new ArrayList<>()); // Empty list on failure
			}
		}
		return marketData;
	}

	/**
	 * Load all alerts from symbol sets.
	 */
	private Map<String, AlertDeployment> loadAlerts(List<SymbolSetGroup> symbolSets) {
		Map<String, AlertDeployment> alerts = new HashMap<>();
		Set<String> alertIds = new HashSet<>();

		for (SymbolSetGroup group : symbolSets) {
			alertIds.addAll(group.getAlertIds());
		}

		// Load alerts one by one (batch method not available)
		for (String alertId : alertIds) {
			alertRepository.findById(alertId).ifPresent(alert -> alerts.put(alert.getId(), alert));
		}

		return alerts;
	}

	/**
	 * Load all bots from symbol sets.
	 */
	private Map<String, BotDeployment> loadBots(List<SymbolSetGroup> symbolSets) {
		Map<String, BotDeployment> bots = new HashMap<>();
		Set<String> botIds = new HashSet<>();

		for (SymbolSetGroup group : symbolSets) {
			botIds.addAll(group.getBotIds());
		}

		// Load bots one by one (batch method not available)
		for (String botId : botIds) {
			botRepository.findById(botId).ifPresent(bot -> bots.put(bot.getId(), bot));
		}

		return bots;
	}

	/**
	 * Load strategies for all deployments.
	 */
	private Map<String, Strategy> loadStrategies(Map<String, AlertDeployment> alerts,
			Map<String, BotDeployment> bots) {
		Map<String, Strategy> strategies = new HashMap<>();
		Set<String> strategyIds = new HashSet<>();

		for (AlertDeployment alert : alerts.values()) {
			if (alert.getStrategyId() != null) {
				strategyIds.add(alert.getStrategyId());
			}
		}
		for (BotDeployment bot : bots.values()) {
			if (bot.getStrategyId() != null) {
				strategyIds.add(bot.getStrategyId());
			}
		}

		if (!strategyIds.isEmpty()) {
			for (String strategyId : strategyIds) {
				strategyRepository.findById(strategyId).ifPresent(strategy -> strategies.put(strategyId, strategy));
			}
		}

		return strategies;
	}

	/**
	 * Execute strategies via gRPC ExecutionServiceClient.
	 * Builds batch request for all deployments and maps results back to signals.
	 */
	private List<SignalResult> executeStrategies(List<SymbolSetGroup> symbolSets,
			Map<String, AlertDeployment> alerts, Map<String, BotDeployment> bots, Map<String, Strategy> strategies,
			Map<String, List<MarketDataBar>> marketData, String tier) {
		List<SignalResult> results = new ArrayList<>();

		// Build symbol set execution data for gRPC batch call
		List<SymbolSetExecutionData> symbolSetDataList = new ArrayList<>();

		for (SymbolSetGroup group : symbolSets) {
			List<DeploymentExecutionData> deployments = new ArrayList<>();

			// Add alert deployments
			for (String alertId : group.getAlertIds()) {
				AlertDeployment alert = alerts.get(alertId);
				if (alert != null && alert.getStrategyId() != null) {
					Strategy strategy = strategies.get(alert.getStrategyId());
					if (strategy != null && strategy.getCode() != null) {
						deployments.add(new DeploymentExecutionData(alertId, "ALERT", alert.getStrategyId(),
								strategy.getCode(), alert.getUserId(), null));
					}
				}
			}

			// Add bot deployments
			for (String botId : group.getBotIds()) {
				BotDeployment bot = bots.get(botId);
				if (bot != null && bot.getStrategyId() != null) {
					Strategy strategy = strategies.get(bot.getStrategyId());
					if (strategy != null && strategy.getCode() != null) {
						deployments.add(new DeploymentExecutionData(botId, "BOT", bot.getStrategyId(),
								strategy.getCode(), bot.getUserId(), null));
					}
				}
			}

			if (!deployments.isEmpty()) {
				// Build market data map for this symbol set
				Map<String, List<MarketDataBar>> symbolMarketData = new HashMap<>();
				for (String symbol : group.getSymbols()) {
					List<MarketDataBar> symbolBars = marketData.get(symbol);
					if (symbolBars != null && !symbolBars.isEmpty()) {
						symbolMarketData.put(symbol, symbolBars);
					}
				}

				symbolSetDataList.add(new SymbolSetExecutionData(group.getSymbols(), symbolMarketData, deployments));
			}
		}

		if (symbolSetDataList.isEmpty()) {
			log.warn("No valid deployments to execute");
			return results;
		}

		// Call gRPC ExecuteList
		try {
			log.info("Calling gRPC executeList with {} symbol sets, tier={}", symbolSetDataList.size(), tier);
			ExecuteListResponse response = executionServiceClient.executeList(symbolSetDataList, tier,
					executionTimeoutSeconds);

			if (response == null) {
				log.error("gRPC executeList returned null response");
				return results;
			}

			// Map gRPC results back to SignalResult objects
			for (DeploymentResult deploymentResult : response.getResults()) {
				String deploymentId = deploymentResult.getDeploymentId();
				String deploymentType = deploymentResult.getDeploymentType();
				LiveSignal liveSignal = deploymentResult.getSignal();

				// Convert LiveSignal to Signal
				Signal signal = convertLiveSignalToSignal(liveSignal, deploymentId, deploymentType);

				// Get the deployment object
				Object deployment = "ALERT".equals(deploymentType) ? alerts.get(deploymentId) : bots.get(deploymentId);

				results.add(new SignalResult(deploymentId, deploymentType, signal, deployment));
			}

			log.info("gRPC executeList returned {} results", results.size());
		}
		catch (Exception e) {
			log.error("gRPC executeList failed: {}", e.getMessage(), e);
			// Return HOLD signals for all deployments on failure
			results.addAll(createHoldSignalsOnFailure(symbolSets, alerts, bots));
		}

		return results;
	}

	/**
	 * Convert gRPC LiveSignal to internal Signal model.
	 */
	private Signal convertLiveSignalToSignal(LiveSignal liveSignal, String deploymentId, String deploymentType) {
		if (liveSignal == null) {
			return Signal.builder().deploymentId(deploymentId).deploymentType(deploymentType).type(Signal.Type.HOLD)
					.build();
		}

		Signal.Type signalType;
		String signalTypeStr = liveSignal.getSignalType();
		if (signalTypeStr != null) {
			switch (signalTypeStr.toUpperCase()) {
				case "BUY":
					signalType = Signal.Type.BUY;
					break;
				case "SELL":
					signalType = Signal.Type.SELL;
					break;
				default:
					signalType = Signal.Type.HOLD;
			}
		}
		else {
			signalType = Signal.Type.HOLD;
		}

		String symbol = liveSignal.getSymbol() != null ? liveSignal.getSymbol() : "UNKNOWN";

		return Signal.builder()
				.deploymentId(deploymentId)
				.deploymentType(deploymentType)
				.type(signalType)
				.symbol(symbol)
				.price(liveSignal.getPrice())
				.build();
	}

	/**
	 * Create HOLD signals for all deployments when gRPC fails.
	 */
	private List<SignalResult> createHoldSignalsOnFailure(List<SymbolSetGroup> symbolSets,
			Map<String, AlertDeployment> alerts, Map<String, BotDeployment> bots) {
		List<SignalResult> results = new ArrayList<>();

		for (SymbolSetGroup group : symbolSets) {
			String symbol = group.getSymbols().isEmpty() ? "UNKNOWN" : group.getSymbols().get(0);

			for (String alertId : group.getAlertIds()) {
				AlertDeployment alert = alerts.get(alertId);
				if (alert != null) {
					Signal holdSignal = Signal.builder()
							.deploymentId(alertId)
							.deploymentType("ALERT")
							.type(Signal.Type.HOLD)
							.symbol(symbol)
							.strategyId(alert.getStrategyId())
							.build();
					results.add(new SignalResult(alertId, "ALERT", holdSignal, alert));
				}
			}
			for (String botId : group.getBotIds()) {
				BotDeployment bot = bots.get(botId);
				if (bot != null) {
					Signal holdSignal = Signal.builder()
							.deploymentId(botId)
							.deploymentType("BOT")
							.type(Signal.Type.HOLD)
							.symbol(symbol)
							.strategyId(bot.getStrategyId())
							.build();
					results.add(new SignalResult(botId, "BOT", holdSignal, bot));
				}
			}
		}

		return results;
	}

	/**
	 * Route signal to appropriate adapter.
	 */
	private void routeSignal(SignalResult result) {
		Signal signal = result.signal();
		if (signal == null) {
			log.warn("No signal to route for deployment {}", result.deploymentId());
			return;
		}

		// Find adapter that can handle this signal
		for (SignalAdapter adapter : signalAdapters) {
			if (adapter.canHandle(signal)) {
				SignalAdapter.SignalResult adapterResult = adapter.process(signal);
				if (!adapterResult.success()) {
					log.warn("Signal processing failed for {}: {}", result.deploymentId(), adapterResult.message());
				}
				return;
			}
		}

		log.warn("No adapter found for signal: deploymentType={}, deploymentId={}",
				result.deploymentType(), result.deploymentId());
	}

	/**
	 * Result of processing a single deployment.
	 */
	public record SignalResult(String deploymentId, String deploymentType, Signal signal, Object deployment) {
	}

	/**
	 * Result of processing a batch.
	 */
	public record ProcessingResult(String messageId, String tier, int symbolSetsProcessed, int signalsGenerated,
			int alertsTriggered, int botsTriggered, int errors, long durationMs) {

		public boolean isSuccess() {
			return errors == 0;
		}
	}

}
