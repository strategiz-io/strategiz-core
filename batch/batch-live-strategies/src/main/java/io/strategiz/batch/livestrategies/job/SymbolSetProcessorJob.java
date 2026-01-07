package io.strategiz.batch.livestrategies.job;

import io.strategiz.batch.livestrategies.model.DeploymentBatchMessage;
import io.strategiz.business.livestrategies.adapter.SignalAdapter;
import io.strategiz.business.livestrategies.model.Signal;
import io.strategiz.business.livestrategies.model.SymbolSetGroup;
import io.strategiz.data.strategy.entity.AlertDeployment;
import io.strategiz.data.strategy.entity.BotDeployment;
import io.strategiz.data.strategy.entity.Strategy;
import io.strategiz.data.strategy.repository.ReadAlertDeploymentRepository;
import io.strategiz.data.strategy.repository.ReadBotDeploymentRepository;
import io.strategiz.data.strategy.repository.ReadStrategyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Processes deployment batches received from Pub/Sub.
 *
 * This job:
 * 1. Receives a batch of symbol sets with alert/bot IDs
 * 2. Fetches market data for all unique symbols
 * 3. Loads strategy code for each deployment
 * 4. Calls gRPC ExecuteList to evaluate all strategies
 * 5. Routes signals to appropriate adapters (Alert or Bot)
 */
@Component
public class SymbolSetProcessorJob {

	private static final Logger log = LoggerFactory.getLogger(SymbolSetProcessorJob.class);

	private final ReadAlertDeploymentRepository alertRepository;

	private final ReadBotDeploymentRepository botRepository;

	private final ReadStrategyRepository strategyRepository;

	private final List<SignalAdapter> signalAdapters;

	// TODO: Inject ExecutionServiceClient for gRPC calls
	// TODO: Inject MarketDataRepository for fetching OHLCV data

	private final ExecutorService signalExecutor = Executors.newFixedThreadPool(10);

	@Autowired
	public SymbolSetProcessorJob(ReadAlertDeploymentRepository alertRepository,
			ReadBotDeploymentRepository botRepository, ReadStrategyRepository strategyRepository,
			List<SignalAdapter> signalAdapters) {
		this.alertRepository = alertRepository;
		this.botRepository = botRepository;
		this.strategyRepository = strategyRepository;
		this.signalAdapters = signalAdapters != null ? signalAdapters : new ArrayList<>();
		log.info("SymbolSetProcessorJob initialized with {} signal adapters", this.signalAdapters.size());
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

			// 2. Fetch market data for all symbols (from TimescaleDB)
			Map<String, List<Object>> marketData = fetchMarketData(allSymbols);
			log.debug("Fetched market data for {} symbols", marketData.size());

			// 3. Load all deployments and strategies
			Map<String, AlertDeployment> alerts = loadAlerts(message.getSymbolSets());
			Map<String, BotDeployment> bots = loadBots(message.getSymbolSets());
			Map<String, Strategy> strategies = loadStrategies(alerts, bots);
			log.debug("Loaded {} alerts, {} bots, {} strategies", alerts.size(), bots.size(), strategies.size());

			// 4. Call gRPC ExecuteList to evaluate all strategies
			// TODO: Implement gRPC call when ExecutionServiceClient is ready
			// For now, we'll simulate signal generation
			List<SignalResult> results = executeStrategies(message.getSymbolSets(), alerts, bots, strategies,
					marketData);
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
	 * Fetch market data for all symbols.
	 * TODO: Implement using MarketDataTimescaleRepository
	 */
	private Map<String, List<Object>> fetchMarketData(Set<String> symbols) {
		// Placeholder - will be implemented with TimescaleDB
		Map<String, List<Object>> marketData = new HashMap<>();
		for (String symbol : symbols) {
			marketData.put(symbol, new ArrayList<>()); // Empty placeholder
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
	 * Execute strategies via gRPC.
	 * TODO: Implement using ExecutionServiceClient.executeList()
	 */
	private List<SignalResult> executeStrategies(List<SymbolSetGroup> symbolSets,
			Map<String, AlertDeployment> alerts, Map<String, BotDeployment> bots, Map<String, Strategy> strategies,
			Map<String, List<Object>> marketData) {
		List<SignalResult> results = new ArrayList<>();

		// TODO: Build ExecuteListRequest with all symbol sets and strategies
		// TODO: Call executionServiceClient.executeList(request)
		// TODO: Map results back to alerts/bots

		// For now, return empty results (HOLD signals)
		for (SymbolSetGroup group : symbolSets) {
			String symbol = group.getSymbols().isEmpty() ? "UNKNOWN" : group.getSymbols().get(0);

			for (String alertId : group.getAlertIds()) {
				AlertDeployment alert = alerts.get(alertId);
				if (alert != null) {
					// Placeholder signal - actual signal comes from gRPC
					Signal holdSignal = Signal.builder()
							.deploymentId(alertId)
							.deploymentType("ALERT")
							.type(Signal.Type.HOLD)
							.symbol(symbol)
							.price(0.0)
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
							.price(0.0)
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
