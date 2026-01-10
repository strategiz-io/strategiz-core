package io.strategiz.batch.livestrategies.job;

import io.strategiz.batch.livestrategies.fixtures.TestFixtures;
import io.strategiz.batch.livestrategies.model.DeploymentBatchMessage;
import io.strategiz.business.livestrategies.adapter.SignalAdapter;
import io.strategiz.business.livestrategies.model.Signal;
import io.strategiz.business.livestrategies.model.SymbolSetGroup;
import io.strategiz.client.execution.ExecutionServiceClient;
import io.strategiz.client.execution.model.DeploymentResult;
import io.strategiz.client.execution.model.ExecuteListResponse;
import io.strategiz.client.execution.model.LiveSignal;
import io.strategiz.data.marketdata.entity.MarketDataEntity;
import io.strategiz.data.marketdata.clickhouse.repository.MarketDataClickHouseRepository;
import io.strategiz.data.strategy.entity.AlertDeployment;
import io.strategiz.data.strategy.entity.BotDeployment;
import io.strategiz.data.strategy.entity.Strategy;
import io.strategiz.data.strategy.repository.ReadAlertDeploymentRepository;
import io.strategiz.data.strategy.repository.ReadBotDeploymentRepository;
import io.strategiz.data.strategy.repository.ReadStrategyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for SymbolSetProcessorJob.
 */
@ExtendWith(MockitoExtension.class)
class SymbolSetProcessorJobTest {

	@Mock
	private ReadAlertDeploymentRepository alertRepository;

	@Mock
	private ReadBotDeploymentRepository botRepository;

	@Mock
	private ReadStrategyRepository strategyRepository;

	@Mock
	private ExecutionServiceClient executionServiceClient;

	@Mock
	private MarketDataClickHouseRepository marketDataRepository;

	@Mock
	private SignalAdapter alertSignalAdapter;

	@Mock
	private SignalAdapter botSignalAdapter;

	@Captor
	private ArgumentCaptor<Signal> signalCaptor;

	private SymbolSetProcessorJob processorJob;

	@BeforeEach
	void setUp() {
		// Configure adapters
		lenient().when(alertSignalAdapter.canHandle(any())).thenAnswer(invocation -> {
			Signal signal = invocation.getArgument(0);
			return "ALERT".equals(signal.getDeploymentType());
		});
		lenient().when(alertSignalAdapter.process(any()))
			.thenReturn(SignalAdapter.SignalResult.success("alert-id", "email", "Alert processed"));

		lenient().when(botSignalAdapter.canHandle(any())).thenAnswer(invocation -> {
			Signal signal = invocation.getArgument(0);
			return "BOT".equals(signal.getDeploymentType());
		});
		lenient().when(botSignalAdapter.process(any()))
			.thenReturn(SignalAdapter.SignalResult.success("bot-id", "trading", "Bot processed"));

		List<SignalAdapter> adapters = List.of(alertSignalAdapter, botSignalAdapter);

		processorJob = new SymbolSetProcessorJob(alertRepository, botRepository, strategyRepository, adapters,
				executionServiceClient, marketDataRepository);

		// Set config values via reflection
		try {
			var lookbackField = SymbolSetProcessorJob.class.getDeclaredField("marketDataLookbackDays");
			lookbackField.setAccessible(true);
			lookbackField.set(processorJob, 365);

			var timeoutField = SymbolSetProcessorJob.class.getDeclaredField("executionTimeoutSeconds");
			timeoutField.setAccessible(true);
			timeoutField.set(processorJob, 30);
		}
		catch (Exception e) {
			fail("Failed to set config: " + e.getMessage());
		}
	}

	@Test
	@DisplayName("Should process batch with single alert")
	void shouldProcessSingleAlert() {
		// Setup
		String alertId = "alert-123";
		String userId = "user-456";
		String strategyId = "strategy-789";

		AlertDeployment alert = TestFixtures.createAlertDeployment(alertId, userId, strategyId, List.of("AAPL"), "PRO");
		Strategy strategy = TestFixtures.createRsiStrategy(strategyId, userId);

		SymbolSetGroup group = TestFixtures.createSymbolSetGroup("AAPL", alertId);
		DeploymentBatchMessage message = TestFixtures.createBatchMessage("TIER1", List.of(group));

		// Mock repositories
		when(alertRepository.findById(alertId)).thenReturn(Optional.of(alert));
		when(strategyRepository.findById(strategyId)).thenReturn(Optional.of(strategy));
		when(marketDataRepository.findBySymbolAndTimeRange(eq("AAPL"), any(), any(), eq("1Day")))
			.thenReturn(createMarketDataBars("AAPL", 10));

		// Mock gRPC response
		LiveSignal liveSignal = LiveSignal.builder().signalType("BUY").symbol("AAPL").price(150.0).build();

		DeploymentResult deploymentResult = DeploymentResult.builder()
			.deploymentId(alertId)
			.deploymentType("ALERT")
			.signal(liveSignal)
			.success(true)
			.build();

		ExecuteListResponse response = ExecuteListResponse.builder()
			.success(true)
			.results(List.of(deploymentResult))
			.build();

		when(executionServiceClient.executeList(any(), eq("TIER1"), eq(30))).thenReturn(response);

		// Execute
		SymbolSetProcessorJob.ProcessingResult result = processorJob.process(message);

		// Verify
		assertTrue(result.isSuccess());
		assertEquals(1, result.symbolSetsProcessed());
		assertEquals(1, result.signalsGenerated());
		assertEquals(1, result.alertsTriggered());
		assertEquals(0, result.botsTriggered());
		assertEquals(0, result.errors());

		verify(alertSignalAdapter).process(signalCaptor.capture());
		Signal capturedSignal = signalCaptor.getValue();
		assertEquals(Signal.Type.BUY, capturedSignal.getType());
		assertEquals("AAPL", capturedSignal.getSymbol());
	}

	@Test
	@DisplayName("Should process batch with single bot")
	void shouldProcessSingleBot() {
		// Setup
		String botId = "bot-123";
		String userId = "user-456";
		String strategyId = "strategy-789";

		BotDeployment bot = TestFixtures.createPaperBot(userId, strategyId, "MSFT", "PRO");
		bot.setId(botId);
		Strategy strategy = TestFixtures.createMacdStrategy(strategyId, userId);

		SymbolSetGroup group = TestFixtures.createSymbolSetGroup(List.of("MSFT"), List.of(), List.of(botId));
		DeploymentBatchMessage message = TestFixtures.createBatchMessage("TIER1", List.of(group));

		// Mock repositories
		when(botRepository.findById(botId)).thenReturn(Optional.of(bot));
		when(strategyRepository.findById(strategyId)).thenReturn(Optional.of(strategy));
		when(marketDataRepository.findBySymbolAndTimeRange(eq("MSFT"), any(), any(), eq("1Day")))
			.thenReturn(createMarketDataBars("MSFT", 10));

		// Mock gRPC response
		LiveSignal liveSignal = LiveSignal.builder().signalType("SELL").symbol("MSFT").price(400.0).build();

		DeploymentResult deploymentResult = DeploymentResult.builder()
			.deploymentId(botId)
			.deploymentType("BOT")
			.signal(liveSignal)
			.success(true)
			.build();

		ExecuteListResponse response = ExecuteListResponse.builder()
			.success(true)
			.results(List.of(deploymentResult))
			.build();

		when(executionServiceClient.executeList(any(), eq("TIER1"), eq(30))).thenReturn(response);

		// Execute
		SymbolSetProcessorJob.ProcessingResult result = processorJob.process(message);

		// Verify
		assertTrue(result.isSuccess());
		assertEquals(1, result.signalsGenerated());
		assertEquals(0, result.alertsTriggered());
		assertEquals(1, result.botsTriggered());

		verify(botSignalAdapter).process(signalCaptor.capture());
		Signal capturedSignal = signalCaptor.getValue();
		assertEquals(Signal.Type.SELL, capturedSignal.getType());
	}

	@Test
	@DisplayName("Should not route HOLD signals to adapters")
	void shouldNotRouteHoldSignals() {
		// Setup
		String alertId = "alert-123";
		String userId = "user-456";
		String strategyId = "strategy-789";

		AlertDeployment alert = TestFixtures.createAlertDeployment(alertId, userId, strategyId, List.of("AAPL"), "PRO");
		Strategy strategy = TestFixtures.createStrategy(strategyId, userId, TestFixtures.SIMPLE_HOLD_STRATEGY_CODE);

		SymbolSetGroup group = TestFixtures.createSymbolSetGroup("AAPL", alertId);
		DeploymentBatchMessage message = TestFixtures.createBatchMessage("TIER1", List.of(group));

		// Mock repositories
		when(alertRepository.findById(alertId)).thenReturn(Optional.of(alert));
		when(strategyRepository.findById(strategyId)).thenReturn(Optional.of(strategy));
		when(marketDataRepository.findBySymbolAndTimeRange(eq("AAPL"), any(), any(), eq("1Day")))
			.thenReturn(createMarketDataBars("AAPL", 10));

		// Mock gRPC response with HOLD
		LiveSignal liveSignal = LiveSignal.builder().signalType("HOLD").symbol("AAPL").price(150.0).build();

		DeploymentResult deploymentResult = DeploymentResult.builder()
			.deploymentId(alertId)
			.deploymentType("ALERT")
			.signal(liveSignal)
			.success(true)
			.build();

		ExecuteListResponse response = ExecuteListResponse.builder()
			.success(true)
			.results(List.of(deploymentResult))
			.build();

		when(executionServiceClient.executeList(any(), eq("TIER1"), eq(30))).thenReturn(response);

		// Execute
		SymbolSetProcessorJob.ProcessingResult result = processorJob.process(message);

		// Verify - HOLD signals are generated but not triggered
		assertTrue(result.isSuccess());
		assertEquals(1, result.signalsGenerated());
		assertEquals(0, result.alertsTriggered());
		assertEquals(0, result.botsTriggered());

		// HOLD signals should not be routed to adapters
		verify(alertSignalAdapter, never()).process(any());
	}

	@Test
	@DisplayName("Should handle gRPC failure with HOLD fallback")
	void shouldHandleGrpcFailure() {
		// Setup
		String alertId = "alert-123";
		String userId = "user-456";
		String strategyId = "strategy-789";

		AlertDeployment alert = TestFixtures.createAlertDeployment(alertId, userId, strategyId, List.of("AAPL"), "PRO");
		Strategy strategy = TestFixtures.createRsiStrategy(strategyId, userId);

		SymbolSetGroup group = TestFixtures.createSymbolSetGroup("AAPL", alertId);
		DeploymentBatchMessage message = TestFixtures.createBatchMessage("TIER1", List.of(group));

		// Mock repositories
		when(alertRepository.findById(alertId)).thenReturn(Optional.of(alert));
		when(strategyRepository.findById(strategyId)).thenReturn(Optional.of(strategy));
		when(marketDataRepository.findBySymbolAndTimeRange(eq("AAPL"), any(), any(), eq("1Day")))
			.thenReturn(createMarketDataBars("AAPL", 10));

		// Mock gRPC failure
		when(executionServiceClient.executeList(any(), eq("TIER1"), eq(30)))
			.thenThrow(new RuntimeException("gRPC connection failed"));

		// Execute
		SymbolSetProcessorJob.ProcessingResult result = processorJob.process(message);

		// Verify - returns HOLD signals on failure, no adapters called
		assertTrue(result.isSuccess());
		assertEquals(1, result.signalsGenerated());
		assertEquals(0, result.alertsTriggered());
		verify(alertSignalAdapter, never()).process(any());
	}

	@Test
	@DisplayName("Should handle missing alert gracefully")
	void shouldHandleMissingAlert() {
		// Setup
		String alertId = "missing-alert";
		SymbolSetGroup group = TestFixtures.createSymbolSetGroup("AAPL", alertId);
		DeploymentBatchMessage message = TestFixtures.createBatchMessage("TIER1", List.of(group));

		// Mock repositories - alert not found
		when(alertRepository.findById(alertId)).thenReturn(Optional.empty());
		when(marketDataRepository.findBySymbolAndTimeRange(eq("AAPL"), any(), any(), eq("1Day")))
			.thenReturn(createMarketDataBars("AAPL", 10));

		// Mock empty gRPC response (no deployments) - lenient since gRPC may be skipped
		ExecuteListResponse response = ExecuteListResponse.builder().success(true).results(List.of()).build();
		lenient().when(executionServiceClient.executeList(any(), eq("TIER1"), eq(30))).thenReturn(response);

		// Execute
		SymbolSetProcessorJob.ProcessingResult result = processorJob.process(message);

		// Verify - completes without error
		assertTrue(result.isSuccess());
		assertEquals(0, result.signalsGenerated());
	}

	@Test
	@DisplayName("Should process pairs trading strategy with two symbols")
	void shouldProcessPairsTradingStrategy() {
		// Setup - pairs trading on AAPL/MSFT
		String alertId = "pairs-alert";
		String userId = "user-456";
		String strategyId = "pairs-strategy";

		AlertDeployment alert = TestFixtures.createAlertDeployment(alertId, userId, strategyId,
				List.of("AAPL", "MSFT"), "PRO");
		Strategy strategy = TestFixtures.createStrategy(strategyId, userId, """
				def strategy(data):
				    return 'BUY'
				""");

		SymbolSetGroup group = TestFixtures.createSymbolSetGroup(List.of("AAPL", "MSFT"), List.of(alertId), List.of());
		DeploymentBatchMessage message = TestFixtures.createBatchMessage("TIER1", List.of(group));

		// Mock repositories
		when(alertRepository.findById(alertId)).thenReturn(Optional.of(alert));
		when(strategyRepository.findById(strategyId)).thenReturn(Optional.of(strategy));
		when(marketDataRepository.findBySymbolAndTimeRange(eq("AAPL"), any(), any(), eq("1Day")))
			.thenReturn(createMarketDataBars("AAPL", 10));
		when(marketDataRepository.findBySymbolAndTimeRange(eq("MSFT"), any(), any(), eq("1Day")))
			.thenReturn(createMarketDataBars("MSFT", 10));

		// Mock gRPC response
		LiveSignal liveSignal = LiveSignal.builder().signalType("BUY").symbol("AAPL").price(150.0).build();

		DeploymentResult deploymentResult = DeploymentResult.builder()
			.deploymentId(alertId)
			.deploymentType("ALERT")
			.signal(liveSignal)
			.success(true)
			.build();

		ExecuteListResponse response = ExecuteListResponse.builder()
			.success(true)
			.results(List.of(deploymentResult))
			.build();

		when(executionServiceClient.executeList(any(), eq("TIER1"), eq(30))).thenReturn(response);

		// Execute
		SymbolSetProcessorJob.ProcessingResult result = processorJob.process(message);

		// Verify
		assertTrue(result.isSuccess());
		assertEquals(1, result.symbolSetsProcessed());
		assertEquals(1, result.alertsTriggered());

		// Verify market data was fetched for both symbols
		verify(marketDataRepository).findBySymbolAndTimeRange(eq("AAPL"), any(), any(), eq("1Day"));
		verify(marketDataRepository).findBySymbolAndTimeRange(eq("MSFT"), any(), any(), eq("1Day"));
	}

	// ===== Helper Methods =====

	private List<MarketDataEntity> createMarketDataBars(String symbol, int count) {
		List<MarketDataEntity> bars = new ArrayList<>();
		Instant now = Instant.now();

		for (int i = count; i > 0; i--) {
			MarketDataEntity bar = new MarketDataEntity();
			bar.setSymbol(symbol);
			bar.setTimestamp(now.minusSeconds(i * 86400L));
			bar.setOpen(new BigDecimal("150.00"));
			bar.setHigh(new BigDecimal("155.00"));
			bar.setLow(new BigDecimal("148.00"));
			bar.setClose(new BigDecimal("152.00"));
			bar.setVolume(new BigDecimal("1000000"));
			bars.add(bar);
		}

		return bars;
	}

}
