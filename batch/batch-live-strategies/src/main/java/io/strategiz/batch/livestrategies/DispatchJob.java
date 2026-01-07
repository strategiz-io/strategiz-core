package io.strategiz.batch.livestrategies;

import io.strategiz.business.livestrategies.model.SymbolSetGroup;
import io.strategiz.business.marketdata.JobExecutionHistoryBusiness;
import io.strategiz.data.strategy.entity.AlertDeployment;
import io.strategiz.data.strategy.entity.BotDeployment;
import io.strategiz.data.strategy.repository.ReadAlertDeploymentRepository;
import io.strategiz.data.strategy.repository.ReadBotDeploymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Scheduled job for dispatching live strategy alerts and bots.
 *
 * Purpose: Query active deployments by tier and publish to Pub/Sub for processing
 * Execution: Auto-scheduled based on tier (TIER1: 1min, TIER2: 5min, TIER3: 15min)
 *
 * Key Features:
 * - Fan-out by Symbol SET (supports multi-symbol strategies like pairs trading)
 * - Groups deployments by sorted symbol set key (e.g., "AAPL,MSFT")
 * - Batches symbol sets into groups of 100 for efficient gRPC processing
 * - Publishes to Pub/Sub topic: publisher-deployment-processing
 *
 * Architecture: Runs in application-console with "scheduler" profile
 */
@Component
@Profile("scheduler")
public class DispatchJob {

	private static final Logger log = LoggerFactory.getLogger(DispatchJob.class);

	private static final int BATCH_SIZE = 100; // Symbol sets per Pub/Sub message

	private final ReadAlertDeploymentRepository readAlertDeploymentRepository;

	private final ReadBotDeploymentRepository readBotDeploymentRepository;

	private final JobExecutionHistoryBusiness jobExecutionHistoryBusiness;
	// TODO: Add PubSubPublisher for message publishing

	private final AtomicBoolean isRunning = new AtomicBoolean(false);

	@Value("${live-strategies.dispatch.enabled:false}")
	private boolean dispatchEnabled;

	public DispatchJob(ReadAlertDeploymentRepository readAlertDeploymentRepository,
			ReadBotDeploymentRepository readBotDeploymentRepository,
			JobExecutionHistoryBusiness jobExecutionHistoryBusiness) {
		this.readAlertDeploymentRepository = readAlertDeploymentRepository;
		this.readBotDeploymentRepository = readBotDeploymentRepository;
		this.jobExecutionHistoryBusiness = jobExecutionHistoryBusiness;
		log.info("DispatchJob initialized (enabled: {}, profile: scheduler)", dispatchEnabled);
	}

	/**
	 * Execute dispatch for a specific tier.
	 * Called by DynamicJobSchedulerBusiness based on jobs table schedule.
	 *
	 * @param tier The subscription tier: TIER1 (1min), TIER2 (5min), TIER3 (15min)
	 */
	public DispatchResult execute(String tier) {
		if (!dispatchEnabled) {
			log.debug("Live strategies dispatch disabled, skipping");
			return DispatchResult.disabled();
		}

		if (!isRunning.compareAndSet(false, true)) {
			log.warn("Dispatch job for tier {} already running, skipping", tier);
			return DispatchResult.alreadyRunning();
		}

		Instant startTime = Instant.now();
		String jobId = "DISPATCH_" + tier;

		String executionId = null;
		try {
			log.info("Starting dispatch for tier: {}", tier);

			// Record job start (returns executionId for tracking)
			executionId = jobExecutionHistoryBusiness.recordJobStart(jobId, "live-strategies-dispatch", tier);

			// 1. Query all active deployments for this tier
			String subscriptionTier = mapTierToSubscription(tier);
			List<AlertDeployment> alerts = readAlertDeploymentRepository.findActiveAlertsByTier(subscriptionTier);
			List<BotDeployment> bots = readBotDeploymentRepository.findActiveBotsByTier(subscriptionTier);

			log.info("Found {} active alerts and {} active bots for tier {}", alerts.size(), bots.size(), tier);

			if (alerts.isEmpty() && bots.isEmpty()) {
				Duration duration = Duration.between(startTime, Instant.now());
				jobExecutionHistoryBusiness.recordJobCompletion(executionId, "SUCCESS", 0, 0L, 0, null);
				return DispatchResult.empty(tier);
			}

			// 2. Group by symbol SET (sorted, joined)
			Map<String, SymbolSetGroup> groups = groupBySymbolSet(alerts, bots);
			log.info("Grouped into {} unique symbol sets", groups.size());

			// 3. Batch symbol sets into groups of 100
			List<List<SymbolSetGroup>> batches = partitionGroups(new ArrayList<>(groups.values()), BATCH_SIZE);
			log.info("Created {} batches of {} symbol sets each", batches.size(), BATCH_SIZE);

			// 4. Publish each batch to Pub/Sub
			int messagesPublished = publishBatches(tier, batches);

			Duration duration = Duration.between(startTime, Instant.now());
			log.info("Dispatch completed for tier {}: {} alerts, {} bots, {} symbol sets, {} messages in {}ms",
					tier, alerts.size(), bots.size(), groups.size(), messagesPublished, duration.toMillis());

			// Record job completion (symbolsProcessed = alerts + bots)
			int totalDeployments = alerts.size() + bots.size();
			jobExecutionHistoryBusiness.recordJobCompletion(executionId, "SUCCESS", totalDeployments,
					(long) messagesPublished, 0, null);

			return new DispatchResult(true, tier, alerts.size(), bots.size(), groups.size(), messagesPublished,
					duration.toMillis(), null);

		}
		catch (Exception e) {
			Duration duration = Duration.between(startTime, Instant.now());
			log.error("Error during dispatch for tier {}: {}", tier, e.getMessage(), e);

			// Record job failure
			if (executionId != null) {
				jobExecutionHistoryBusiness.recordJobCompletion(executionId, "FAILED", 0, 0L, 1, e.getMessage());
			}

			return DispatchResult.failed(tier, e.getMessage());
		}
		finally {
			isRunning.set(false);
		}
	}

	/**
	 * Manual trigger from console app.
	 */
	public DispatchResult triggerManualExecution(String tier) {
		log.info("Manual dispatch triggered for tier: {}", tier);
		return execute(tier);
	}

	/**
	 * Check if job is currently running
	 */
	public boolean isRunning() {
		return isRunning.get();
	}

	/**
	 * Group deployments by symbol SET.
	 * Multi-symbol strategies get grouped together (e.g., pairs trading AAPL+MSFT).
	 * Both alerts and bots are grouped into the same map for unified processing.
	 */
	private Map<String, SymbolSetGroup> groupBySymbolSet(List<AlertDeployment> alerts, List<BotDeployment> bots) {
		Map<String, SymbolSetGroup> groups = new HashMap<>();

		// Group alerts by symbol set
		for (AlertDeployment alert : alerts) {
			List<String> symbols = alert.getSymbols();
			if (symbols == null || symbols.isEmpty()) {
				log.warn("Alert {} has no symbols, skipping", alert.getId());
				continue;
			}

			String key = getSymbolSetKey(symbols);
			groups.computeIfAbsent(key, k -> new SymbolSetGroup(symbols))
					.addAlert(alert.getId());
		}

		// Group bots by symbol set
		for (BotDeployment bot : bots) {
			List<String> symbols = bot.getSymbols();
			if (symbols == null || symbols.isEmpty()) {
				log.warn("Bot {} has no symbols, skipping", bot.getId());
				continue;
			}

			String key = getSymbolSetKey(symbols);
			groups.computeIfAbsent(key, k -> new SymbolSetGroup(symbols))
					.addBot(bot.getId());
		}

		return groups;
	}

	/**
	 * Create a canonical key for a symbol set (sorted, comma-joined).
	 * "MSFT,AAPL" and "AAPL,MSFT" both become "AAPL,MSFT"
	 */
	private String getSymbolSetKey(List<String> symbols) {
		return symbols.stream()
				.map(String::toUpperCase)
				.sorted()
				.collect(Collectors.joining(","));
	}

	/**
	 * Partition groups into batches of specified size.
	 */
	private List<List<SymbolSetGroup>> partitionGroups(List<SymbolSetGroup> groups, int batchSize) {
		List<List<SymbolSetGroup>> batches = new ArrayList<>();
		for (int i = 0; i < groups.size(); i += batchSize) {
			batches.add(groups.subList(i, Math.min(i + batchSize, groups.size())));
		}
		return batches;
	}

	/**
	 * Publish batches to Pub/Sub.
	 * TODO: Implement actual Pub/Sub publishing
	 */
	private int publishBatches(String tier, List<List<SymbolSetGroup>> batches) {
		int messagesPublished = 0;

		for (List<SymbolSetGroup> batch : batches) {
			// TODO: Create DeploymentBatchMessage and publish to Pub/Sub
			// pubSubPublisher.publish("publisher-deployment-processing",
			//     DeploymentBatchMessage.builder()
			//         .tier(tier)
			//         .symbolSets(batch)
			//         .build());

			log.debug("Would publish batch with {} symbol sets for tier {}", batch.size(), tier);
			messagesPublished++;
		}

		return messagesPublished;
	}

	/**
	 * Map tier name to subscription tier for repository query.
	 */
	private String mapTierToSubscription(String tier) {
		return switch (tier) {
			case "TIER1" -> "PRO";
			case "TIER2" -> "STARTER";
			case "TIER3" -> "FREE";
			default -> tier;
		};
	}

	/**
	 * Result of a dispatch operation.
	 */
	public record DispatchResult(
			boolean success,
			String tier,
			int alertsProcessed,
			int botsProcessed,
			int symbolSetsCreated,
			int messagesPublished,
			long durationMs,
			String errorMessage
	) {
		public static DispatchResult disabled() {
			return new DispatchResult(false, null, 0, 0, 0, 0, 0, "Dispatch disabled");
		}

		public static DispatchResult alreadyRunning() {
			return new DispatchResult(false, null, 0, 0, 0, 0, 0, "Already running");
		}

		public static DispatchResult empty(String tier) {
			return new DispatchResult(true, tier, 0, 0, 0, 0, 0, null);
		}

		public static DispatchResult failed(String tier, String error) {
			return new DispatchResult(false, tier, 0, 0, 0, 0, 0, error);
		}
	}

}
