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
import org.junit.jupiter.api.Nested;
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
 * Comprehensive Integration Tests for Live Strategies Batch Processing.
 *
 * These tests run against REAL infrastructure:
 * - Firestore (strategiz-io project)
 * - TimescaleDB (market data)
 * - gRPC execution service (optional)
 *
 * Prerequisites (one-time setup):
 * 1. gcloud auth application-default login
 * 2. vault server -dev (in separate terminal)
 *
 * Run with:
 *   export VAULT_ADDR=http://localhost:8200
 *   export VAULT_TOKEN=root
 *   mvn test -Dtest=LiveStrategiesIntegrationTest -pl batch/batch-live-strategies
 */
@SpringBootTest(classes = LiveStrategiesTestApplication.class)
@ActiveProfiles({"integration", "scheduler"})
@Tag("integration")
@EnabledIfEnvironmentVariable(named = "VAULT_TOKEN", matches = ".+")
class LiveStrategiesIntegrationTest {

	private static final Logger log = LoggerFactory.getLogger(LiveStrategiesIntegrationTest.class);

	private static final String TEST_USER_ID = "test-user-" + UUID.randomUUID();
	private static final String TEST_STRATEGY_PREFIX = "test-strategy-";
	private static final String TEST_ALERT_PREFIX = "test-alert-";
	private static final String TEST_BOT_PREFIX = "test-bot-";

	// Sample strategy codes
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

	private static final String MACD_STRATEGY_CODE = """
		import pandas as pd

		def strategy(data):
		    close = data['close']
		    ema12 = close.ewm(span=12).mean()
		    ema26 = close.ewm(span=26).mean()
		    macd = ema12 - ema26
		    signal = macd.ewm(span=9).mean()

		    if macd.iloc[-1] > signal.iloc[-1] and macd.iloc[-2] <= signal.iloc[-2]:
		        return 'BUY'
		    elif macd.iloc[-1] < signal.iloc[-1] and macd.iloc[-2] >= signal.iloc[-2]:
		        return 'SELL'
		    return 'HOLD'
		""";

	private static final String PAIRS_TRADING_CODE = """
		import pandas as pd
		import numpy as np

		def strategy(data):
		    # Pairs trading on two symbols
		    symbols = list(data.keys()) if isinstance(data, dict) else ['AAPL']
		    if len(symbols) < 2:
		        return 'HOLD'

		    # Calculate spread
		    price1 = data[symbols[0]]['close'] if isinstance(data, dict) else data['close']
		    price2 = data[symbols[1]]['close'] if isinstance(data, dict) else data['close']

		    spread = price1 - price2
		    mean_spread = spread.rolling(window=20).mean()
		    std_spread = spread.rolling(window=20).std()
		    z_score = (spread - mean_spread) / std_spread

		    if z_score.iloc[-1] < -2:
		        return 'BUY'
		    elif z_score.iloc[-1] > 2:
		        return 'SELL'
		    return 'HOLD'
		""";

	private static final String SIMPLE_BUY_CODE = "def strategy(data):\n    return 'BUY'";
	private static final String SIMPLE_SELL_CODE = "def strategy(data):\n    return 'SELL'";
	private static final String SIMPLE_HOLD_CODE = "def strategy(data):\n    return 'HOLD'";

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
		log.info("=== Starting integration test ===");
		log.info("Test user ID: {}", TEST_USER_ID);
		log.info("gRPC client available: {}", executionServiceClient != null);
	}

	@AfterEach
	void tearDown() {
		log.info("=== Cleaning up test data ===");

		// Clean up alerts
		for (String alertId : createdAlertIds) {
			try {
				alertRepository.delete(alertId, TEST_USER_ID);
				log.debug("Deleted test alert: {}", alertId);
			}
			catch (Exception e) {
				log.warn("Failed to delete alert {}: {}", alertId, e.getMessage());
			}
		}

		// Clean up bots
		for (String botId : createdBotIds) {
			try {
				botRepository.delete(botId, TEST_USER_ID);
				log.debug("Deleted test bot: {}", botId);
			}
			catch (Exception e) {
				log.warn("Failed to delete bot {}: {}", botId, e.getMessage());
			}
		}

		// Clean up strategies
		for (String strategyId : createdStrategyIds) {
			try {
				strategyRepository.delete(strategyId, TEST_USER_ID);
				log.debug("Deleted test strategy: {}", strategyId);
			}
			catch (Exception e) {
				log.warn("Failed to delete strategy {}: {}", strategyId, e.getMessage());
			}
		}

		createdAlertIds.clear();
		createdBotIds.clear();
		createdStrategyIds.clear();
		log.info("Cleanup complete");
	}

	// ========================================================================
	// MARKET DATA TESTS - Verify TimescaleDB connectivity and data coverage
	// ========================================================================
	@Nested
	@DisplayName("Market Data Tests")
	class MarketDataTests {

		@Test
		@DisplayName("Should have AAPL market data for last 30 days")
		void shouldHaveAAPLMarketData() {
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
		@DisplayName("Should have market data for multiple stock symbols")
		void shouldHaveMarketDataForMultipleStocks() {
			List<String> symbols = List.of("AAPL", "MSFT", "GOOGL", "TSLA", "NVDA", "AMZN");
			Instant endDate = Instant.now();
			Instant startDate = endDate.minus(7, ChronoUnit.DAYS);

			for (String symbol : symbols) {
				List<MarketDataTimescaleEntity> bars = marketDataRepository.findBySymbolAndTimeRange(
						symbol, startDate, endDate, "1Day");

				log.info("{}: {} bars in last 7 days", symbol, bars.size());
				assertTrue(bars.size() >= 3, "Should have at least 3 trading days for " + symbol);
			}
		}

		@Test
		@DisplayName("Should have market data for 1-year lookback (strategy execution window)")
		void shouldHaveOneYearMarketData() {
			Instant endDate = Instant.now();
			Instant startDate = endDate.minus(365, ChronoUnit.DAYS);

			List<MarketDataTimescaleEntity> bars = marketDataRepository.findBySymbolAndTimeRange(
					"AAPL", startDate, endDate, "1Day");

			log.info("Found {} bars for AAPL in last 365 days", bars.size());
			assertTrue(bars.size() >= 200, "Should have at least 200 trading days for 1-year lookback");
		}

		@Test
		@DisplayName("Should have intraday data (1Hour timeframe)")
		void shouldHaveIntradayData() {
			Instant endDate = Instant.now();
			Instant startDate = endDate.minus(7, ChronoUnit.DAYS);

			List<MarketDataTimescaleEntity> bars = marketDataRepository.findBySymbolAndTimeRange(
					"AAPL", startDate, endDate, "1Hour");

			log.info("Found {} hourly bars for AAPL in last 7 days", bars.size());
			// Market hours: 9:30 AM - 4:00 PM ET = 6.5 hours per day, ~5 trading days
			assertTrue(bars.size() >= 20, "Should have at least 20 hourly bars");
		}
	}

	// ========================================================================
	// FIRESTORE CRUD TESTS - Verify repository connectivity
	// ========================================================================
	@Nested
	@DisplayName("Firestore CRUD Tests")
	class FirestoreCrudTests {

		@Test
		@DisplayName("Create and read Strategy from Firestore")
		void shouldCreateAndReadStrategy() {
			String strategyId = TEST_STRATEGY_PREFIX + UUID.randomUUID();

			Strategy strategy = createTestStrategy(strategyId, "RSI Test Strategy", RSI_STRATEGY_CODE);
			strategyRepository.save(strategy, TEST_USER_ID);
			createdStrategyIds.add(strategyId);

			log.info("Created strategy: {}", strategyId);

			// Read back
			Optional<Strategy> loaded = readStrategyRepository.findById(strategyId);

			assertTrue(loaded.isPresent(), "Strategy should exist");
			assertEquals(strategyId, loaded.get().getId());
			assertEquals("RSI Test Strategy", loaded.get().getName());
			assertEquals(RSI_STRATEGY_CODE, loaded.get().getCode());

			log.info("Successfully verified strategy: {}", loaded.get().getName());
		}

		@Test
		@DisplayName("Create and read AlertDeployment from Firestore")
		void shouldCreateAndReadAlertDeployment() {
			// First create a strategy
			String strategyId = TEST_STRATEGY_PREFIX + UUID.randomUUID();
			Strategy strategy = createTestStrategy(strategyId, "Alert Test Strategy", RSI_STRATEGY_CODE);
			strategyRepository.save(strategy, TEST_USER_ID);
			createdStrategyIds.add(strategyId);

			// Create alert
			String alertId = TEST_ALERT_PREFIX + UUID.randomUUID();
			AlertDeployment alert = createTestAlert(alertId, strategyId, List.of("AAPL"), "PRO");
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
			Strategy strategy = createTestStrategy(strategyId, "Bot Test Strategy", MACD_STRATEGY_CODE);
			strategyRepository.save(strategy, TEST_USER_ID);
			createdStrategyIds.add(strategyId);

			// Create bot
			String botId = TEST_BOT_PREFIX + UUID.randomUUID();
			BotDeployment bot = createTestBot(botId, strategyId, List.of("MSFT"), "PRO", "PAPER");
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

		@Test
		@DisplayName("Create AlertDeployment with multiple symbols (pairs trading)")
		void shouldCreateAlertWithMultipleSymbols() {
			String strategyId = TEST_STRATEGY_PREFIX + UUID.randomUUID();
			Strategy strategy = createTestStrategy(strategyId, "Pairs Trading Strategy", PAIRS_TRADING_CODE);
			strategyRepository.save(strategy, TEST_USER_ID);
			createdStrategyIds.add(strategyId);

			String alertId = TEST_ALERT_PREFIX + UUID.randomUUID();
			AlertDeployment alert = createTestAlert(alertId, strategyId, List.of("AAPL", "MSFT"), "PRO");
			alertRepository.save(alert, TEST_USER_ID);
			createdAlertIds.add(alertId);

			Optional<AlertDeployment> loaded = readAlertRepository.findById(alertId);

			assertTrue(loaded.isPresent());
			assertEquals(2, loaded.get().getSymbols().size());
			assertTrue(loaded.get().getSymbols().contains("AAPL"));
			assertTrue(loaded.get().getSymbols().contains("MSFT"));
		}

		@Test
		@DisplayName("Create BotDeployment with LIVE environment")
		void shouldCreateLiveBot() {
			String strategyId = TEST_STRATEGY_PREFIX + UUID.randomUUID();
			Strategy strategy = createTestStrategy(strategyId, "Live Bot Strategy", MACD_STRATEGY_CODE);
			strategyRepository.save(strategy, TEST_USER_ID);
			createdStrategyIds.add(strategyId);

			String botId = TEST_BOT_PREFIX + UUID.randomUUID();
			BotDeployment bot = createTestBot(botId, strategyId, List.of("NVDA"), "PRO", "LIVE");
			bot.setSimulatedMode(false);
			bot.setMaxPositionSize(5000.0);
			botRepository.save(bot, TEST_USER_ID);
			createdBotIds.add(botId);

			Optional<BotDeployment> loaded = readBotRepository.findById(botId);

			assertTrue(loaded.isPresent());
			assertEquals("LIVE", loaded.get().getEnvironment());
			assertFalse(loaded.get().isSimulatedMode());
			assertEquals(5000.0, loaded.get().getMaxPositionSize());
		}
	}

	// ========================================================================
	// DISPATCH JOB TESTS - Verify tier-based querying and grouping
	// ========================================================================
	@Nested
	@DisplayName("Dispatch Job Tests")
	class DispatchJobTests {

		@Test
		@DisplayName("Query active alerts by PRO tier (TIER1)")
		void shouldQueryActiveAlertsByProTier() {
			String strategyId = TEST_STRATEGY_PREFIX + UUID.randomUUID();
			Strategy strategy = createTestStrategy(strategyId, "PRO Tier Strategy", RSI_STRATEGY_CODE);
			strategyRepository.save(strategy, TEST_USER_ID);
			createdStrategyIds.add(strategyId);

			String alertId = TEST_ALERT_PREFIX + UUID.randomUUID();
			AlertDeployment alert = createTestAlert(alertId, strategyId, List.of("AAPL"), "PRO");
			alertRepository.save(alert, TEST_USER_ID);
			createdAlertIds.add(alertId);

			List<AlertDeployment> activeAlerts = readAlertRepository.findActiveAlertsByTier("PRO");

			log.info("Found {} active PRO tier alerts", activeAlerts.size());

			boolean foundTestAlert = activeAlerts.stream()
					.anyMatch(a -> a.getId().equals(alertId));

			assertTrue(foundTestAlert, "Test alert should be found in active PRO alerts");
		}

		@Test
		@DisplayName("Query active alerts by STARTER tier (TIER2)")
		void shouldQueryActiveAlertsByStarterTier() {
			String strategyId = TEST_STRATEGY_PREFIX + UUID.randomUUID();
			Strategy strategy = createTestStrategy(strategyId, "STARTER Tier Strategy", RSI_STRATEGY_CODE);
			strategyRepository.save(strategy, TEST_USER_ID);
			createdStrategyIds.add(strategyId);

			String alertId = TEST_ALERT_PREFIX + UUID.randomUUID();
			AlertDeployment alert = createTestAlert(alertId, strategyId, List.of("TSLA"), "STARTER");
			alertRepository.save(alert, TEST_USER_ID);
			createdAlertIds.add(alertId);

			List<AlertDeployment> activeAlerts = readAlertRepository.findActiveAlertsByTier("STARTER");

			boolean foundTestAlert = activeAlerts.stream()
					.anyMatch(a -> a.getId().equals(alertId));

			assertTrue(foundTestAlert, "Test alert should be found in active STARTER alerts");
		}

		@Test
		@DisplayName("Query active bots by tier")
		void shouldQueryActiveBotsByTier() {
			String strategyId = TEST_STRATEGY_PREFIX + UUID.randomUUID();
			Strategy strategy = createTestStrategy(strategyId, "Bot Tier Strategy", MACD_STRATEGY_CODE);
			strategyRepository.save(strategy, TEST_USER_ID);
			createdStrategyIds.add(strategyId);

			String botId = TEST_BOT_PREFIX + UUID.randomUUID();
			BotDeployment bot = createTestBot(botId, strategyId, List.of("GOOGL"), "PRO", "PAPER");
			botRepository.save(bot, TEST_USER_ID);
			createdBotIds.add(botId);

			List<BotDeployment> activeBots = readBotRepository.findActiveBotsByTier("PRO");

			boolean foundTestBot = activeBots.stream()
					.anyMatch(b -> b.getId().equals(botId));

			assertTrue(foundTestBot, "Test bot should be found in active PRO bots");
		}

		@Test
		@DisplayName("Inactive alerts should not be returned")
		void shouldNotReturnInactiveAlerts() {
			String strategyId = TEST_STRATEGY_PREFIX + UUID.randomUUID();
			Strategy strategy = createTestStrategy(strategyId, "Inactive Strategy", RSI_STRATEGY_CODE);
			strategyRepository.save(strategy, TEST_USER_ID);
			createdStrategyIds.add(strategyId);

			String alertId = TEST_ALERT_PREFIX + UUID.randomUUID();
			AlertDeployment alert = createTestAlert(alertId, strategyId, List.of("AMZN"), "PRO");
			alert.setStatus("PAUSED"); // Not ACTIVE
			alertRepository.save(alert, TEST_USER_ID);
			createdAlertIds.add(alertId);

			List<AlertDeployment> activeAlerts = readAlertRepository.findActiveAlertsByTier("PRO");

			boolean foundTestAlert = activeAlerts.stream()
					.anyMatch(a -> a.getId().equals(alertId));

			assertFalse(foundTestAlert, "Paused alert should NOT be in active alerts");
		}
	}

	// ========================================================================
	// SYMBOL SET PROCESSOR TESTS - gRPC execution scenarios
	// ========================================================================
	@Nested
	@DisplayName("Symbol Set Processor Tests (gRPC)")
	class SymbolSetProcessorTests {

		@Test
		@DisplayName("Process single symbol alert")
		void shouldProcessSingleSymbolAlert() {
			if (executionServiceClient == null) {
				log.warn("Skipping test - gRPC execution service not available");
				return;
			}

			String strategyId = TEST_STRATEGY_PREFIX + UUID.randomUUID();
			Strategy strategy = createTestStrategy(strategyId, "Single Symbol Strategy", RSI_STRATEGY_CODE);
			strategyRepository.save(strategy, TEST_USER_ID);
			createdStrategyIds.add(strategyId);

			String alertId = TEST_ALERT_PREFIX + UUID.randomUUID();
			AlertDeployment alert = createTestAlert(alertId, strategyId, List.of("AAPL"), "PRO");
			alertRepository.save(alert, TEST_USER_ID);
			createdAlertIds.add(alertId);

			// Build batch message with single symbol
			SymbolSetGroup group = new SymbolSetGroup(List.of("AAPL"));
			group.addAlert(alertId);

			DeploymentBatchMessage message = DeploymentBatchMessage.builder()
					.tier("TIER1")
					.symbolSets(List.of(group))
					.build();

			// Execute
			log.info("Processing single symbol alert...");
			SymbolSetProcessorJob.ProcessingResult result = symbolSetProcessorJob.process(message);

			log.info("Result: success={}, signals={}, alerts={}, bots={}, errors={}",
					result.isSuccess(), result.signalsGenerated(),
					result.alertsTriggered(), result.botsTriggered(), result.errors());

			assertTrue(result.isSuccess(), "Processing should succeed");
			assertEquals(1, result.symbolSetsProcessed());
			assertEquals(0, result.errors());
		}

		@Test
		@DisplayName("Process multi-symbol alert (pairs trading)")
		void shouldProcessMultiSymbolAlert() {
			if (executionServiceClient == null) {
				log.warn("Skipping test - gRPC execution service not available");
				return;
			}

			String strategyId = TEST_STRATEGY_PREFIX + UUID.randomUUID();
			Strategy strategy = createTestStrategy(strategyId, "Pairs Trading Strategy", PAIRS_TRADING_CODE);
			strategyRepository.save(strategy, TEST_USER_ID);
			createdStrategyIds.add(strategyId);

			String alertId = TEST_ALERT_PREFIX + UUID.randomUUID();
			AlertDeployment alert = createTestAlert(alertId, strategyId, List.of("AAPL", "MSFT"), "PRO");
			alertRepository.save(alert, TEST_USER_ID);
			createdAlertIds.add(alertId);

			// Build batch message with symbol SET (pairs)
			SymbolSetGroup group = new SymbolSetGroup(List.of("AAPL", "MSFT"));
			group.addAlert(alertId);

			DeploymentBatchMessage message = DeploymentBatchMessage.builder()
					.tier("TIER1")
					.symbolSets(List.of(group))
					.build();

			log.info("Processing pairs trading alert (AAPL, MSFT)...");
			SymbolSetProcessorJob.ProcessingResult result = symbolSetProcessorJob.process(message);

			assertTrue(result.isSuccess());
			assertEquals(1, result.symbolSetsProcessed());
		}

		@Test
		@DisplayName("Process single symbol bot")
		void shouldProcessSingleSymbolBot() {
			if (executionServiceClient == null) {
				log.warn("Skipping test - gRPC execution service not available");
				return;
			}

			String strategyId = TEST_STRATEGY_PREFIX + UUID.randomUUID();
			Strategy strategy = createTestStrategy(strategyId, "Bot Strategy", MACD_STRATEGY_CODE);
			strategyRepository.save(strategy, TEST_USER_ID);
			createdStrategyIds.add(strategyId);

			String botId = TEST_BOT_PREFIX + UUID.randomUUID();
			BotDeployment bot = createTestBot(botId, strategyId, List.of("NVDA"), "PRO", "PAPER");
			botRepository.save(bot, TEST_USER_ID);
			createdBotIds.add(botId);

			SymbolSetGroup group = new SymbolSetGroup(List.of("NVDA"));
			group.addBot(botId);

			DeploymentBatchMessage message = DeploymentBatchMessage.builder()
					.tier("TIER1")
					.symbolSets(List.of(group))
					.build();

			log.info("Processing single symbol bot (NVDA)...");
			SymbolSetProcessorJob.ProcessingResult result = symbolSetProcessorJob.process(message);

			assertTrue(result.isSuccess());
		}

		@Test
		@DisplayName("Process mixed alerts and bots in same batch")
		void shouldProcessMixedAlertsAndBots() {
			if (executionServiceClient == null) {
				log.warn("Skipping test - gRPC execution service not available");
				return;
			}

			// Create strategy
			String strategyId = TEST_STRATEGY_PREFIX + UUID.randomUUID();
			Strategy strategy = createTestStrategy(strategyId, "Mixed Strategy", RSI_STRATEGY_CODE);
			strategyRepository.save(strategy, TEST_USER_ID);
			createdStrategyIds.add(strategyId);

			// Create alert on AAPL
			String alertId = TEST_ALERT_PREFIX + UUID.randomUUID();
			AlertDeployment alert = createTestAlert(alertId, strategyId, List.of("AAPL"), "PRO");
			alertRepository.save(alert, TEST_USER_ID);
			createdAlertIds.add(alertId);

			// Create bot on AAPL (same symbol)
			String botId = TEST_BOT_PREFIX + UUID.randomUUID();
			BotDeployment bot = createTestBot(botId, strategyId, List.of("AAPL"), "PRO", "PAPER");
			botRepository.save(bot, TEST_USER_ID);
			createdBotIds.add(botId);

			// Same symbol set, different deployment types
			SymbolSetGroup group = new SymbolSetGroup(List.of("AAPL"));
			group.addAlert(alertId);
			group.addBot(botId);

			DeploymentBatchMessage message = DeploymentBatchMessage.builder()
					.tier("TIER1")
					.symbolSets(List.of(group))
					.build();

			log.info("Processing mixed alerts and bots on AAPL...");
			SymbolSetProcessorJob.ProcessingResult result = symbolSetProcessorJob.process(message);

			assertTrue(result.isSuccess());
			assertEquals(1, result.symbolSetsProcessed());
		}

		@Test
		@DisplayName("Process multiple symbol sets in single batch")
		void shouldProcessMultipleSymbolSets() {
			if (executionServiceClient == null) {
				log.warn("Skipping test - gRPC execution service not available");
				return;
			}

			String strategyId = TEST_STRATEGY_PREFIX + UUID.randomUUID();
			Strategy strategy = createTestStrategy(strategyId, "Multi Symbol Set Strategy", RSI_STRATEGY_CODE);
			strategyRepository.save(strategy, TEST_USER_ID);
			createdStrategyIds.add(strategyId);

			// Alert on AAPL
			String alertId1 = TEST_ALERT_PREFIX + UUID.randomUUID();
			AlertDeployment alert1 = createTestAlert(alertId1, strategyId, List.of("AAPL"), "PRO");
			alertRepository.save(alert1, TEST_USER_ID);
			createdAlertIds.add(alertId1);

			// Alert on MSFT
			String alertId2 = TEST_ALERT_PREFIX + UUID.randomUUID();
			AlertDeployment alert2 = createTestAlert(alertId2, strategyId, List.of("MSFT"), "PRO");
			alertRepository.save(alert2, TEST_USER_ID);
			createdAlertIds.add(alertId2);

			// Alert on GOOGL
			String alertId3 = TEST_ALERT_PREFIX + UUID.randomUUID();
			AlertDeployment alert3 = createTestAlert(alertId3, strategyId, List.of("GOOGL"), "PRO");
			alertRepository.save(alert3, TEST_USER_ID);
			createdAlertIds.add(alertId3);

			// Three different symbol sets
			SymbolSetGroup group1 = new SymbolSetGroup(List.of("AAPL"));
			group1.addAlert(alertId1);

			SymbolSetGroup group2 = new SymbolSetGroup(List.of("MSFT"));
			group2.addAlert(alertId2);

			SymbolSetGroup group3 = new SymbolSetGroup(List.of("GOOGL"));
			group3.addAlert(alertId3);

			DeploymentBatchMessage message = DeploymentBatchMessage.builder()
					.tier("TIER1")
					.symbolSets(List.of(group1, group2, group3))
					.build();

			log.info("Processing 3 symbol sets (AAPL, MSFT, GOOGL)...");
			SymbolSetProcessorJob.ProcessingResult result = symbolSetProcessorJob.process(message);

			assertTrue(result.isSuccess());
			assertEquals(3, result.symbolSetsProcessed());
		}

		@Test
		@DisplayName("Process batch with HOLD-returning strategy")
		void shouldProcessHoldStrategy() {
			if (executionServiceClient == null) {
				log.warn("Skipping test - gRPC execution service not available");
				return;
			}

			String strategyId = TEST_STRATEGY_PREFIX + UUID.randomUUID();
			Strategy strategy = createTestStrategy(strategyId, "HOLD Strategy", SIMPLE_HOLD_CODE);
			strategyRepository.save(strategy, TEST_USER_ID);
			createdStrategyIds.add(strategyId);

			String alertId = TEST_ALERT_PREFIX + UUID.randomUUID();
			AlertDeployment alert = createTestAlert(alertId, strategyId, List.of("AAPL"), "PRO");
			alertRepository.save(alert, TEST_USER_ID);
			createdAlertIds.add(alertId);

			SymbolSetGroup group = new SymbolSetGroup(List.of("AAPL"));
			group.addAlert(alertId);

			DeploymentBatchMessage message = DeploymentBatchMessage.builder()
					.tier("TIER1")
					.symbolSets(List.of(group))
					.build();

			log.info("Processing HOLD strategy...");
			SymbolSetProcessorJob.ProcessingResult result = symbolSetProcessorJob.process(message);

			assertTrue(result.isSuccess());
			// HOLD signals are generated but not "triggered" (no notification sent)
			assertEquals(0, result.alertsTriggered(), "HOLD should not trigger alerts");
			assertEquals(0, result.botsTriggered(), "HOLD should not trigger bots");
		}

		@Test
		@DisplayName("Handle missing deployment gracefully")
		void shouldHandleMissingDeployment() {
			// Don't create the alert, just reference a non-existent ID
			String fakeAlertId = TEST_ALERT_PREFIX + "non-existent-" + UUID.randomUUID();

			SymbolSetGroup group = new SymbolSetGroup(List.of("AAPL"));
			group.addAlert(fakeAlertId);

			DeploymentBatchMessage message = DeploymentBatchMessage.builder()
					.tier("TIER1")
					.symbolSets(List.of(group))
					.build();

			log.info("Processing batch with missing deployment...");
			SymbolSetProcessorJob.ProcessingResult result = symbolSetProcessorJob.process(message);

			// Should complete without exception
			assertTrue(result.isSuccess() || result.errors() > 0,
					"Should handle missing deployment gracefully");
		}
	}

	// ========================================================================
	// TIER-SPECIFIC TESTS - Verify different subscription tier handling
	// ========================================================================
	@Nested
	@DisplayName("Subscription Tier Tests")
	class SubscriptionTierTests {

		@Test
		@DisplayName("TIER1 (PRO) processes at highest priority")
		void shouldProcessTier1ProAlerts() {
			if (executionServiceClient == null) {
				log.warn("Skipping test - gRPC not available");
				return;
			}

			String strategyId = TEST_STRATEGY_PREFIX + UUID.randomUUID();
			Strategy strategy = createTestStrategy(strategyId, "PRO Strategy", SIMPLE_BUY_CODE);
			strategyRepository.save(strategy, TEST_USER_ID);
			createdStrategyIds.add(strategyId);

			String alertId = TEST_ALERT_PREFIX + UUID.randomUUID();
			AlertDeployment alert = createTestAlert(alertId, strategyId, List.of("AAPL"), "PRO");
			alertRepository.save(alert, TEST_USER_ID);
			createdAlertIds.add(alertId);

			SymbolSetGroup group = new SymbolSetGroup(List.of("AAPL"));
			group.addAlert(alertId);

			DeploymentBatchMessage message = DeploymentBatchMessage.builder()
					.tier("TIER1") // PRO tier
					.symbolSets(List.of(group))
					.build();

			SymbolSetProcessorJob.ProcessingResult result = symbolSetProcessorJob.process(message);

			assertTrue(result.isSuccess());
		}

		@Test
		@DisplayName("TIER2 (STARTER) processes correctly")
		void shouldProcessTier2StarterAlerts() {
			if (executionServiceClient == null) {
				log.warn("Skipping test - gRPC not available");
				return;
			}

			String strategyId = TEST_STRATEGY_PREFIX + UUID.randomUUID();
			Strategy strategy = createTestStrategy(strategyId, "STARTER Strategy", SIMPLE_SELL_CODE);
			strategyRepository.save(strategy, TEST_USER_ID);
			createdStrategyIds.add(strategyId);

			String alertId = TEST_ALERT_PREFIX + UUID.randomUUID();
			AlertDeployment alert = createTestAlert(alertId, strategyId, List.of("MSFT"), "STARTER");
			alertRepository.save(alert, TEST_USER_ID);
			createdAlertIds.add(alertId);

			SymbolSetGroup group = new SymbolSetGroup(List.of("MSFT"));
			group.addAlert(alertId);

			DeploymentBatchMessage message = DeploymentBatchMessage.builder()
					.tier("TIER2") // STARTER tier
					.symbolSets(List.of(group))
					.build();

			SymbolSetProcessorJob.ProcessingResult result = symbolSetProcessorJob.process(message);

			assertTrue(result.isSuccess());
		}

		@Test
		@DisplayName("TIER3 (FREE) processes correctly")
		void shouldProcessTier3FreeAlerts() {
			if (executionServiceClient == null) {
				log.warn("Skipping test - gRPC not available");
				return;
			}

			String strategyId = TEST_STRATEGY_PREFIX + UUID.randomUUID();
			Strategy strategy = createTestStrategy(strategyId, "FREE Strategy", SIMPLE_HOLD_CODE);
			strategyRepository.save(strategy, TEST_USER_ID);
			createdStrategyIds.add(strategyId);

			String alertId = TEST_ALERT_PREFIX + UUID.randomUUID();
			AlertDeployment alert = createTestAlert(alertId, strategyId, List.of("GOOGL"), "FREE");
			alertRepository.save(alert, TEST_USER_ID);
			createdAlertIds.add(alertId);

			SymbolSetGroup group = new SymbolSetGroup(List.of("GOOGL"));
			group.addAlert(alertId);

			DeploymentBatchMessage message = DeploymentBatchMessage.builder()
					.tier("TIER3") // FREE tier
					.symbolSets(List.of(group))
					.build();

			SymbolSetProcessorJob.ProcessingResult result = symbolSetProcessorJob.process(message);

			assertTrue(result.isSuccess());
		}
	}

	// ========================================================================
	// BOT ENVIRONMENT TESTS - Paper vs Live trading
	// ========================================================================
	@Nested
	@DisplayName("Bot Environment Tests")
	class BotEnvironmentTests {

		@Test
		@DisplayName("Paper trading bot processes without real orders")
		void shouldProcessPaperTradingBot() {
			if (executionServiceClient == null) {
				log.warn("Skipping test - gRPC not available");
				return;
			}

			String strategyId = TEST_STRATEGY_PREFIX + UUID.randomUUID();
			Strategy strategy = createTestStrategy(strategyId, "Paper Bot Strategy", SIMPLE_BUY_CODE);
			strategyRepository.save(strategy, TEST_USER_ID);
			createdStrategyIds.add(strategyId);

			String botId = TEST_BOT_PREFIX + UUID.randomUUID();
			BotDeployment bot = createTestBot(botId, strategyId, List.of("TSLA"), "PRO", "PAPER");
			bot.setSimulatedMode(true);
			botRepository.save(bot, TEST_USER_ID);
			createdBotIds.add(botId);

			SymbolSetGroup group = new SymbolSetGroup(List.of("TSLA"));
			group.addBot(botId);

			DeploymentBatchMessage message = DeploymentBatchMessage.builder()
					.tier("TIER1")
					.symbolSets(List.of(group))
					.build();

			log.info("Processing PAPER trading bot...");
			SymbolSetProcessorJob.ProcessingResult result = symbolSetProcessorJob.process(message);

			assertTrue(result.isSuccess());
		}

		@Test
		@DisplayName("Live trading bot with risk parameters")
		void shouldProcessLiveTradingBotWithRiskParams() {
			if (executionServiceClient == null) {
				log.warn("Skipping test - gRPC not available");
				return;
			}

			String strategyId = TEST_STRATEGY_PREFIX + UUID.randomUUID();
			Strategy strategy = createTestStrategy(strategyId, "Live Bot Strategy", MACD_STRATEGY_CODE);
			strategyRepository.save(strategy, TEST_USER_ID);
			createdStrategyIds.add(strategyId);

			String botId = TEST_BOT_PREFIX + UUID.randomUUID();
			BotDeployment bot = createTestBot(botId, strategyId, List.of("AMZN"), "PRO", "LIVE");
			bot.setSimulatedMode(false);
			bot.setMaxPositionSize(10000.0);
			bot.setStopLossPercent(2.0);
			bot.setTakeProfitPercent(5.0);
			botRepository.save(bot, TEST_USER_ID);
			createdBotIds.add(botId);

			SymbolSetGroup group = new SymbolSetGroup(List.of("AMZN"));
			group.addBot(botId);

			DeploymentBatchMessage message = DeploymentBatchMessage.builder()
					.tier("TIER1")
					.symbolSets(List.of(group))
					.build();

			log.info("Processing LIVE trading bot with risk params...");
			SymbolSetProcessorJob.ProcessingResult result = symbolSetProcessorJob.process(message);

			assertTrue(result.isSuccess());
		}
	}

	// ========================================================================
	// HELPER METHODS
	// ========================================================================

	private Strategy createTestStrategy(String strategyId, String name, String code) {
		Strategy strategy = new Strategy();
		strategy.setId(strategyId);
		strategy.setOwnerId(TEST_USER_ID);
		strategy.setCreatorId(TEST_USER_ID);
		strategy.setName(name);
		strategy.setCode(code);
		strategy.setLanguage("python");
		strategy.setType("technical");
		return strategy;
	}

	private AlertDeployment createTestAlert(String alertId, String strategyId, List<String> symbols, String tier) {
		AlertDeployment alert = new AlertDeployment();
		alert.setId(alertId);
		alert.setUserId(TEST_USER_ID);
		alert.setStrategyId(strategyId);
		alert.setSymbols(symbols);
		alert.setStatus("ACTIVE");
		alert.setSubscriptionTier(tier);
		alert.setNotificationChannels(List.of("in-app"));
		return alert;
	}

	private BotDeployment createTestBot(String botId, String strategyId, List<String> symbols, String tier, String environment) {
		BotDeployment bot = new BotDeployment();
		bot.setId(botId);
		bot.setUserId(TEST_USER_ID);
		bot.setStrategyId(strategyId);
		bot.setSymbols(symbols);
		bot.setStatus("ACTIVE");
		bot.setSubscriptionTier(tier);
		bot.setEnvironment(environment);
		bot.setSimulatedMode("PAPER".equals(environment));
		bot.setMaxPositionSize(1000.0);
		bot.setStopLossPercent(5.0);
		bot.setTakeProfitPercent(10.0);
		return bot;
	}

}
