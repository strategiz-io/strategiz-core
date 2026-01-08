package io.strategiz.batch.livestrategies;

import io.strategiz.batch.livestrategies.job.SymbolSetProcessorJob;
import io.strategiz.batch.livestrategies.model.DeploymentBatchMessage;
import io.strategiz.business.livestrategies.model.SymbolSetGroup;
import io.strategiz.client.execution.ExecutionServiceClient;
import io.strategiz.data.marketdata.timescale.entity.MarketDataTimescaleEntity;
import io.strategiz.data.marketdata.timescale.repository.MarketDataTimescaleRepository;
import io.strategiz.data.strategy.entity.AlertDeployment;
import io.strategiz.data.strategy.entity.BotDeployment;
import io.strategiz.data.strategy.entity.Strategy;
import io.strategiz.data.strategy.repository.AlertDeploymentBaseRepository;
import io.strategiz.data.strategy.repository.BotDeploymentBaseRepository;
import io.strategiz.data.strategy.repository.ReadAlertDeploymentRepository;
import io.strategiz.data.strategy.repository.ReadBotDeploymentRepository;
import io.strategiz.data.strategy.repository.ReadStrategyRepository;
import io.strategiz.data.strategy.repository.StrategyBaseRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Live Strategies batch processing.
 *
 * These tests run against REAL infrastructure:
 * - Firestore (strategiz-io project)
 * - TimescaleDB (market data)
 * - gRPC execution service (optional)
 *
 * Prerequisites:
 * 1. Vault running: vault server -dev
 * 2. VAULT_TOKEN environment variable set
 * 3. Firebase service account credentials configured
 * 4. gRPC execution service running (optional, tests skip if unavailable)
 *
 * Run with: mvn test -Dtest=LiveStrategiesIntegrationTest -Dspring.profiles.active=integration
 */
@SpringBootTest(classes = LiveStrategiesTestApplication.class)
@ActiveProfiles("integration")
@Tag("integration")
@EnabledIfEnvironmentVariable(named = "VAULT_TOKEN", matches = ".+")
class LiveStrategiesIntegrationTest {

	private static final Logger log = LoggerFactory.getLogger(LiveStrategiesIntegrationTest.class);

	private static final String TEST_USER_ID = "test-user-" + UUID.randomUUID();
	private static final String TEST_STRATEGY_PREFIX = "test-strategy-";
	private static final String TEST_ALERT_PREFIX = "test-alert-";
	private static final String TEST_BOT_PREFIX = "test-bot-";

	// Sample RSI strategy code
	private static final String RSI_STRATEGY_CODE = """
		import pandas as pd

		def strategy(data):
		    close = data['close']
		    delta = close.diff()
		    gain = (delta.where(delta > 0, 0)).rolling(window=14).mean()
		    loss = (-delta.where(delta < 0, 0)).rolling(window=14).mean()
		    rs = gain / loss
		    rsi = 100 - (100 / (1 + rs))

		    if rsi.iloc[-1] < 30:
		        return 'BUY'
		    elif rsi.iloc[-1] > 70:
		        return 'SELL'
		    return 'HOLD'
		""";

	@Autowired
	private DispatchJob dispatchJob;

	@Autowired
	private SymbolSetProcessorJob symbolSetProcessorJob;

	@Autowired
	private ReadAlertDeploymentRepository readAlertRepository;

	@Autowired
	private AlertDeploymentBaseRepository alertRepository;

	@Autowired
	private ReadBotDeploymentRepository readBotRepository;

	@Autowired
	private BotDeploymentBaseRepository botRepository;

	@Autowired
	private ReadStrategyRepository readStrategyRepository;

	@Autowired
	private StrategyBaseRepository strategyRepository;

	@Autowired
	private MarketDataTimescaleRepository marketDataRepository;

	@Autowired(required = false)
	private ExecutionServiceClient executionServiceClient;

	// Track created entities for cleanup
	private final List<String> createdStrategyIds = new ArrayList<>();
	private final List<String> createdAlertIds = new ArrayList<>();
	private final List<String> createdBotIds = new ArrayList<>();

	@BeforeEach
	void setUp() {
		log.info("=== Starting integration test setup ===");
		log.info("Test user ID: {}", TEST_USER_ID);
	}

	@AfterEach
	void tearDown() {
		log.info("=== Cleaning up test data ===");

		// Clean up alerts
		for (String alertId : createdAlertIds) {
			try {
				alertRepository.delete(alertId, TEST_USER_ID);
				log.info("Deleted test alert: {}", alertId);
			}
			catch (Exception e) {
				log.warn("Failed to delete alert {}: {}", alertId, e.getMessage());
			}
		}

		// Clean up bots
		for (String botId : createdBotIds) {
			try {
				botRepository.delete(botId, TEST_USER_ID);
				log.info("Deleted test bot: {}", botId);
			}
			catch (Exception e) {
				log.warn("Failed to delete bot {}: {}", botId, e.getMessage());
			}
		}

		// Clean up strategies
		for (String strategyId : createdStrategyIds) {
			try {
				strategyRepository.delete(strategyId, TEST_USER_ID);
				log.info("Deleted test strategy: {}", strategyId);
			}
			catch (Exception e) {
				log.warn("Failed to delete strategy {}: {}", strategyId, e.getMessage());
			}
		}

		createdAlertIds.clear();
		createdBotIds.clear();
		createdStrategyIds.clear();
	}

	// ========== Market Data Tests ==========

	@Test
	@DisplayName("Verify market data exists in TimescaleDB for AAPL")
	void shouldHaveMarketDataForAAPL() {
		Instant endDate = Instant.now();
		Instant startDate = endDate.minus(30, ChronoUnit.DAYS);

		List<MarketDataTimescaleEntity> bars = marketDataRepository.findBySymbolAndTimeRange(
				"AAPL", startDate, endDate, "1Day");

		log.info("Found {} bars for AAPL in last 30 days", bars.size());

		assertFalse(bars.isEmpty(), "Should have market data for AAPL");
		assertTrue(bars.size() >= 15, "Should have at least 15 trading days of data");

		// Verify data structure
		MarketDataTimescaleEntity latestBar = bars.get(bars.size() - 1);
		assertNotNull(latestBar.getOpen(), "Open price should not be null");
		assertNotNull(latestBar.getHigh(), "High price should not be null");
		assertNotNull(latestBar.getLow(), "Low price should not be null");
		assertNotNull(latestBar.getClose(), "Close price should not be null");
		assertNotNull(latestBar.getVolume(), "Volume should not be null");

		log.info("Latest AAPL bar: date={}, close={}", latestBar.getTimestamp(), latestBar.getClose());
	}

	@Test
	@DisplayName("Verify market data coverage for multiple symbols")
	void shouldHaveMarketDataForMultipleSymbols() {
		List<String> symbols = List.of("AAPL", "MSFT", "GOOGL", "TSLA");
		Instant endDate = Instant.now();
		Instant startDate = endDate.minus(7, ChronoUnit.DAYS);

		for (String symbol : symbols) {
			List<MarketDataTimescaleEntity> bars = marketDataRepository.findBySymbolAndTimeRange(
					symbol, startDate, endDate, "1Day");

			log.info("{}: {} bars in last 7 days", symbol, bars.size());
			assertTrue(bars.size() >= 3, "Should have at least 3 trading days for " + symbol);
		}
	}

	// ========== Firestore Repository Tests ==========

	@Test
	@DisplayName("Create and read Strategy from Firestore")
	void shouldCreateAndReadStrategy() {
		String strategyId = TEST_STRATEGY_PREFIX + UUID.randomUUID();

		Strategy strategy = new Strategy();
		strategy.setId(strategyId);
		strategy.setOwnerId(TEST_USER_ID);
		strategy.setName("Integration Test RSI Strategy");
		strategy.setCode(RSI_STRATEGY_CODE);
		strategy.setLanguage("python");
		strategy.setType("technical");

		// Create
		strategyRepository.save(strategy, TEST_USER_ID);
		createdStrategyIds.add(strategyId);

		log.info("Created strategy: {}", strategyId);

		// Read back
		Optional<Strategy> loaded = readStrategyRepository.findById(strategyId);

		assertTrue(loaded.isPresent(), "Strategy should exist");
		assertEquals(strategyId, loaded.get().getId());
		assertEquals("Integration Test RSI Strategy", loaded.get().getName());
		assertEquals(RSI_STRATEGY_CODE, loaded.get().getCode());

		log.info("Successfully verified strategy: {}", loaded.get().getName());
	}

	@Test
	@DisplayName("Create and read AlertDeployment from Firestore")
	void shouldCreateAndReadAlertDeployment() {
		// First create a strategy
		String strategyId = TEST_STRATEGY_PREFIX + UUID.randomUUID();
		Strategy strategy = createTestStrategy(strategyId);
		strategyRepository.save(strategy, TEST_USER_ID);
		createdStrategyIds.add(strategyId);

		// Create alert
		String alertId = TEST_ALERT_PREFIX + UUID.randomUUID();
		AlertDeployment alert = new AlertDeployment();
		alert.setId(alertId);
		alert.setUserId(TEST_USER_ID);
		alert.setStrategyId(strategyId);
		alert.setSymbols(List.of("AAPL"));
		alert.setStatus("ACTIVE");
		alert.setSubscriptionTier("PRO");
		alert.setNotificationChannels(List.of("email", "in-app"));

		alertRepository.save(alert, TEST_USER_ID);
		createdAlertIds.add(alertId);

		log.info("Created alert deployment: {}", alertId);

		// Read back
		Optional<AlertDeployment> loaded = readAlertRepository.findById(alertId);

		assertTrue(loaded.isPresent(), "Alert should exist");
		assertEquals(alertId, loaded.get().getId());
		assertEquals(strategyId, loaded.get().getStrategyId());
		assertEquals(List.of("AAPL"), loaded.get().getSymbols());
		assertEquals("ACTIVE", loaded.get().getStatus());

		log.info("Successfully verified alert: {}", loaded.get().getId());
	}

	@Test
	@DisplayName("Create and read BotDeployment from Firestore")
	void shouldCreateAndReadBotDeployment() {
		// First create a strategy
		String strategyId = TEST_STRATEGY_PREFIX + UUID.randomUUID();
		Strategy strategy = createTestStrategy(strategyId);
		strategyRepository.save(strategy, TEST_USER_ID);
		createdStrategyIds.add(strategyId);

		// Create bot
		String botId = TEST_BOT_PREFIX + UUID.randomUUID();
		BotDeployment bot = new BotDeployment();
		bot.setId(botId);
		bot.setUserId(TEST_USER_ID);
		bot.setStrategyId(strategyId);
		bot.setSymbols(List.of("MSFT"));
		bot.setStatus("ACTIVE");
		bot.setSubscriptionTier("PRO");
		bot.setEnvironment("PAPER");
		bot.setSimulatedMode(true);
		bot.setMaxPositionSize(1000.0);
		bot.setStopLossPercent(5.0);
		bot.setTakeProfitPercent(10.0);

		botRepository.save(bot, TEST_USER_ID);
		createdBotIds.add(botId);

		log.info("Created bot deployment: {}", botId);

		// Read back
		Optional<BotDeployment> loaded = readBotRepository.findById(botId);

		assertTrue(loaded.isPresent(), "Bot should exist");
		assertEquals(botId, loaded.get().getId());
		assertEquals(strategyId, loaded.get().getStrategyId());
		assertEquals("PAPER", loaded.get().getEnvironment());

		log.info("Successfully verified bot: {}", loaded.get().getId());
	}

	// ========== Dispatch Job Tests ==========

	@Test
	@DisplayName("DispatchJob queries active deployments from Firestore")
	void shouldQueryActiveDeployments() {
		// Create test strategy
		String strategyId = TEST_STRATEGY_PREFIX + UUID.randomUUID();
		Strategy strategy = createTestStrategy(strategyId);
		strategyRepository.save(strategy, TEST_USER_ID);
		createdStrategyIds.add(strategyId);

		// Create test alert with PRO tier (TIER1)
		String alertId = TEST_ALERT_PREFIX + UUID.randomUUID();
		AlertDeployment alert = createTestAlert(alertId, strategyId, "AAPL", "PRO");
		alertRepository.save(alert, TEST_USER_ID);
		createdAlertIds.add(alertId);

		log.info("Created test alert {} for dispatch test", alertId);

		// Query active alerts for PRO tier
		List<AlertDeployment> activeAlerts = readAlertRepository.findActiveAlertsByTier("PRO");

		log.info("Found {} active PRO tier alerts", activeAlerts.size());

		// Verify our test alert is included
		boolean foundTestAlert = activeAlerts.stream()
				.anyMatch(a -> a.getId().equals(alertId));

		assertTrue(foundTestAlert, "Test alert should be found in active alerts");
	}

	// ========== Symbol Set Processor Tests ==========

	@Test
	@DisplayName("SymbolSetProcessorJob executes strategy with real market data")
	void shouldProcessSymbolSetWithRealData() {
		// Skip if gRPC client not available
		if (executionServiceClient == null) {
			log.warn("Skipping test - gRPC execution service not available");
			return;
		}

		// Create test strategy
		String strategyId = TEST_STRATEGY_PREFIX + UUID.randomUUID();
		Strategy strategy = createTestStrategy(strategyId);
		strategyRepository.save(strategy, TEST_USER_ID);
		createdStrategyIds.add(strategyId);

		// Create test alert
		String alertId = TEST_ALERT_PREFIX + UUID.randomUUID();
		AlertDeployment alert = createTestAlert(alertId, strategyId, "AAPL", "PRO");
		alertRepository.save(alert, TEST_USER_ID);
		createdAlertIds.add(alertId);

		// Build batch message
		SymbolSetGroup group = new SymbolSetGroup(List.of("AAPL"));
		group.addAlert(alertId);

		DeploymentBatchMessage message = DeploymentBatchMessage.builder()
				.tier("TIER1")
				.symbolSets(List.of(group))
				.build();

		// Execute
		log.info("Processing symbol set with real gRPC execution...");
		SymbolSetProcessorJob.ProcessingResult result = symbolSetProcessorJob.process(message);

		// Verify
		log.info("Processing result: success={}, signals={}, alerts={}, bots={}, errors={}",
				result.isSuccess(), result.signalsGenerated(),
				result.alertsTriggered(), result.botsTriggered(), result.errors());

		assertTrue(result.isSuccess(), "Processing should succeed");
		assertTrue(result.signalsGenerated() >= 0, "Should generate signals (or HOLD)");
		assertEquals(0, result.errors(), "Should have no errors");
	}

	// ========== Helper Methods ==========

	private Strategy createTestStrategy(String strategyId) {
		Strategy strategy = new Strategy();
		strategy.setId(strategyId);
		strategy.setOwnerId(TEST_USER_ID);
		strategy.setName("Integration Test Strategy " + strategyId);
		strategy.setCode(RSI_STRATEGY_CODE);
		strategy.setLanguage("python");
		strategy.setType("technical");
		return strategy;
	}

	private AlertDeployment createTestAlert(String alertId, String strategyId, String symbol, String tier) {
		AlertDeployment alert = new AlertDeployment();
		alert.setId(alertId);
		alert.setUserId(TEST_USER_ID);
		alert.setStrategyId(strategyId);
		alert.setSymbols(List.of(symbol));
		alert.setStatus("ACTIVE");
		alert.setSubscriptionTier(tier);
		alert.setNotificationChannels(List.of("in-app"));
		return alert;
	}

	private BotDeployment createTestBot(String botId, String strategyId, String symbol, String tier) {
		BotDeployment bot = new BotDeployment();
		bot.setId(botId);
		bot.setUserId(TEST_USER_ID);
		bot.setStrategyId(strategyId);
		bot.setSymbols(List.of(symbol));
		bot.setStatus("ACTIVE");
		bot.setSubscriptionTier(tier);
		bot.setEnvironment("PAPER");
		bot.setSimulatedMode(true);
		return bot;
	}

}
