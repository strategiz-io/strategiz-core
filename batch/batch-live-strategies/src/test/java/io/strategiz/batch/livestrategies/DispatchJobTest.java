package io.strategiz.batch.livestrategies;

import io.strategiz.batch.livestrategies.fixtures.TestFixtures;
import io.strategiz.batch.livestrategies.pubsub.DeploymentPubSubPublisher;
import io.strategiz.business.marketdata.JobExecutionHistoryBusiness;
import io.strategiz.data.strategy.entity.AlertDeployment;
import io.strategiz.data.strategy.entity.BotDeployment;
import io.strategiz.data.strategy.repository.ReadAlertDeploymentRepository;
import io.strategiz.data.strategy.repository.ReadBotDeploymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DispatchJob.
 *
 * Tests the dispatch logic for live strategy alerts and bots: - Symbol set grouping
 * (multi-symbol strategy support) - Tier-based query routing - Pub/Sub batching - Error
 * handling
 */
@ExtendWith(MockitoExtension.class)
class DispatchJobTest {

	@Mock
	private ReadAlertDeploymentRepository alertRepository;

	@Mock
	private ReadBotDeploymentRepository botRepository;

	@Mock
	private JobExecutionHistoryBusiness jobHistoryBusiness;

	@Mock
	private DeploymentPubSubPublisher pubSubPublisher;

	private DispatchJob dispatchJob;

	@BeforeEach
	void setUp() {
		dispatchJob = new DispatchJob(alertRepository, botRepository, jobHistoryBusiness, pubSubPublisher);

		// Use reflection to enable dispatch (since @Value isn't processed in unit tests)
		try {
			var field = DispatchJob.class.getDeclaredField("dispatchEnabled");
			field.setAccessible(true);
			field.set(dispatchJob, true);
		}
		catch (Exception e) {
			fail("Failed to enable dispatch: " + e.getMessage());
		}
	}

	@Nested
	@DisplayName("execute() - Basic Dispatch")
	class ExecuteTests {

		@Test
		@DisplayName("Should return disabled result when dispatch is disabled")
		void shouldReturnDisabledWhenDisabled() throws Exception {
			// Disable dispatch
			var field = DispatchJob.class.getDeclaredField("dispatchEnabled");
			field.setAccessible(true);
			field.set(dispatchJob, false);

			DispatchJob.DispatchResult result = dispatchJob.execute("TIER1");

			assertFalse(result.success());
			assertEquals("Dispatch disabled", result.errorMessage());
			verify(alertRepository, never()).findActiveAlertsByTier(any());
		}

		@Test
		@DisplayName("Should return empty result when no deployments found")
		void shouldReturnEmptyWhenNoDeployments() {
			when(alertRepository.findActiveAlertsByTier("PRO")).thenReturn(List.of());
			when(botRepository.findActiveBotsByTier("PRO")).thenReturn(List.of());
			when(jobHistoryBusiness.recordJobStart(anyString(), anyString(), anyString())).thenReturn("exec-123");

			DispatchJob.DispatchResult result = dispatchJob.execute("TIER1");

			assertTrue(result.success());
			assertEquals("TIER1", result.tier());
			assertEquals(0, result.alertsProcessed());
			assertEquals(0, result.botsProcessed());
			assertEquals(0, result.symbolSetsCreated());
		}

		@Test
		@DisplayName("Should process alerts and publish to Pub/Sub")
		void shouldProcessAlertsAndPublish() {
			String userId = "user-123";
			String strategyId = "strategy-456";
			AlertDeployment alert1 = TestFixtures.createAlertDeployment(userId, strategyId, "AAPL", "PRO");
			AlertDeployment alert2 = TestFixtures.createAlertDeployment(userId, strategyId, "MSFT", "PRO");

			when(alertRepository.findActiveAlertsByTier("PRO")).thenReturn(List.of(alert1, alert2));
			when(botRepository.findActiveBotsByTier("PRO")).thenReturn(List.of());
			when(jobHistoryBusiness.recordJobStart(anyString(), anyString(), anyString())).thenReturn("exec-123");
			when(pubSubPublisher.isAvailable()).thenReturn(true);
			when(pubSubPublisher.publish(any())).thenReturn("pubsub-id-1");

			DispatchJob.DispatchResult result = dispatchJob.execute("TIER1");

			assertTrue(result.success());
			assertEquals(2, result.alertsProcessed());
			assertEquals(0, result.botsProcessed());
			assertEquals(2, result.symbolSetsCreated()); // AAPL and MSFT are different
															// symbol sets
			assertTrue(result.messagesPublished() > 0);

			verify(pubSubPublisher, atLeastOnce()).publish(any());
		}

		@Test
		@DisplayName("Should group multi-symbol strategies together")
		void shouldGroupMultiSymbolStrategies() {
			String userId = "user-123";
			String strategyId = "strategy-456";

			// Create alerts with the same multi-symbol set
			AlertDeployment alert1 = TestFixtures.createAlertDeployment("alert-1", userId, strategyId,
					List.of("AAPL", "MSFT"), "PRO");
			AlertDeployment alert2 = TestFixtures.createAlertDeployment("alert-2", userId, strategyId,
					List.of("MSFT", "AAPL"), "PRO"); // Same symbols, different order

			when(alertRepository.findActiveAlertsByTier("PRO")).thenReturn(List.of(alert1, alert2));
			when(botRepository.findActiveBotsByTier("PRO")).thenReturn(List.of());
			when(jobHistoryBusiness.recordJobStart(anyString(), anyString(), anyString())).thenReturn("exec-123");
			when(pubSubPublisher.isAvailable()).thenReturn(true);
			when(pubSubPublisher.publish(any())).thenReturn("pubsub-id-1");

			DispatchJob.DispatchResult result = dispatchJob.execute("TIER1");

			assertTrue(result.success());
			assertEquals(2, result.alertsProcessed());
			assertEquals(1, result.symbolSetsCreated()); // Should be grouped into 1
															// symbol set
		}

		@Test
		@DisplayName("Should process mixed alerts and bots")
		void shouldProcessMixedAlertsAndBots() {
			String userId = "user-123";
			String strategyId = "strategy-456";

			AlertDeployment alert = TestFixtures.createAlertDeployment(userId, strategyId, "AAPL", "PRO");
			BotDeployment bot = TestFixtures.createPaperBot(userId, strategyId, "AAPL", "PRO");

			when(alertRepository.findActiveAlertsByTier("PRO")).thenReturn(List.of(alert));
			when(botRepository.findActiveBotsByTier("PRO")).thenReturn(List.of(bot));
			when(jobHistoryBusiness.recordJobStart(anyString(), anyString(), anyString())).thenReturn("exec-123");
			when(pubSubPublisher.isAvailable()).thenReturn(true);
			when(pubSubPublisher.publish(any())).thenReturn("pubsub-id-1");

			DispatchJob.DispatchResult result = dispatchJob.execute("TIER1");

			assertTrue(result.success());
			assertEquals(1, result.alertsProcessed());
			assertEquals(1, result.botsProcessed());
			assertEquals(1, result.symbolSetsCreated()); // Both on AAPL, same symbol set
		}

	}

	@Nested
	@DisplayName("execute() - Tier Mapping")
	class TierMappingTests {

		@Test
		@DisplayName("TIER1 should query PRO subscription tier")
		void tier1ShouldQueryPro() {
			when(alertRepository.findActiveAlertsByTier("PRO")).thenReturn(List.of());
			when(botRepository.findActiveBotsByTier("PRO")).thenReturn(List.of());
			when(jobHistoryBusiness.recordJobStart(anyString(), anyString(), anyString())).thenReturn("exec-123");

			dispatchJob.execute("TIER1");

			verify(alertRepository).findActiveAlertsByTier("PRO");
			verify(botRepository).findActiveBotsByTier("PRO");
		}

		@Test
		@DisplayName("TIER2 should query STARTER subscription tier")
		void tier2ShouldQueryStarter() {
			when(alertRepository.findActiveAlertsByTier("STARTER")).thenReturn(List.of());
			when(botRepository.findActiveBotsByTier("STARTER")).thenReturn(List.of());
			when(jobHistoryBusiness.recordJobStart(anyString(), anyString(), anyString())).thenReturn("exec-123");

			dispatchJob.execute("TIER2");

			verify(alertRepository).findActiveAlertsByTier("STARTER");
			verify(botRepository).findActiveBotsByTier("STARTER");
		}

		@Test
		@DisplayName("TIER3 should query FREE subscription tier")
		void tier3ShouldQueryFree() {
			when(alertRepository.findActiveAlertsByTier("FREE")).thenReturn(List.of());
			when(botRepository.findActiveBotsByTier("FREE")).thenReturn(List.of());
			when(jobHistoryBusiness.recordJobStart(anyString(), anyString(), anyString())).thenReturn("exec-123");

			dispatchJob.execute("TIER3");

			verify(alertRepository).findActiveAlertsByTier("FREE");
			verify(botRepository).findActiveBotsByTier("FREE");
		}

	}

	@Nested
	@DisplayName("execute() - Error Handling")
	class ErrorHandlingTests {

		@Test
		@DisplayName("Should handle repository exceptions gracefully")
		void shouldHandleRepositoryException() {
			when(jobHistoryBusiness.recordJobStart(anyString(), anyString(), anyString())).thenReturn("exec-123");
			when(alertRepository.findActiveAlertsByTier("PRO")).thenThrow(new RuntimeException("Database error"));

			DispatchJob.DispatchResult result = dispatchJob.execute("TIER1");

			assertFalse(result.success());
			assertNotNull(result.errorMessage());
			assertTrue(result.errorMessage().contains("Database error"));

			verify(jobHistoryBusiness).recordJobCompletion(eq("exec-123"), eq("FAILED"), eq(0), eq(0L), eq(1), any());
		}

		@Test
		@DisplayName("Should handle Pub/Sub publish failures gracefully")
		void shouldHandlePubSubFailure() {
			String userId = "user-123";
			String strategyId = "strategy-456";
			AlertDeployment alert = TestFixtures.createAlertDeployment(userId, strategyId, "AAPL", "PRO");

			when(alertRepository.findActiveAlertsByTier("PRO")).thenReturn(List.of(alert));
			when(botRepository.findActiveBotsByTier("PRO")).thenReturn(List.of());
			when(jobHistoryBusiness.recordJobStart(anyString(), anyString(), anyString())).thenReturn("exec-123");
			when(pubSubPublisher.isAvailable()).thenReturn(true);
			when(pubSubPublisher.publish(any())).thenThrow(new RuntimeException("Pub/Sub error"));

			DispatchJob.DispatchResult result = dispatchJob.execute("TIER1");

			// Should still complete (error handled per-batch)
			assertTrue(result.success());
			assertEquals(0, result.messagesPublished()); // No messages published
		}

		@Test
		@DisplayName("Should skip alerts without symbols")
		void shouldSkipAlertsWithoutSymbols() {
			AlertDeployment alertWithSymbols = TestFixtures.createAlertDeployment("user-1", "strat-1", "AAPL", "PRO");
			AlertDeployment alertWithoutSymbols = new AlertDeployment();
			alertWithoutSymbols.setId("alert-no-symbols");
			alertWithoutSymbols.setSymbols(null); // No symbols

			when(alertRepository.findActiveAlertsByTier("PRO"))
				.thenReturn(List.of(alertWithSymbols, alertWithoutSymbols));
			when(botRepository.findActiveBotsByTier("PRO")).thenReturn(List.of());
			when(jobHistoryBusiness.recordJobStart(anyString(), anyString(), anyString())).thenReturn("exec-123");
			when(pubSubPublisher.isAvailable()).thenReturn(true);
			when(pubSubPublisher.publish(any())).thenReturn("pubsub-id-1");

			DispatchJob.DispatchResult result = dispatchJob.execute("TIER1");

			assertTrue(result.success());
			assertEquals(2, result.alertsProcessed()); // Both counted
			assertEquals(1, result.symbolSetsCreated()); // Only one with valid symbols
		}

	}

	@Nested
	@DisplayName("triggerManualExecution()")
	class ManualTriggerTests {

		@Test
		@DisplayName("Should execute dispatch when manually triggered")
		void shouldExecuteWhenManuallyTriggered() {
			when(alertRepository.findActiveAlertsByTier("PRO")).thenReturn(List.of());
			when(botRepository.findActiveBotsByTier("PRO")).thenReturn(List.of());
			when(jobHistoryBusiness.recordJobStart(anyString(), anyString(), anyString())).thenReturn("exec-123");

			DispatchJob.DispatchResult result = dispatchJob.triggerManualExecution("TIER1");

			assertTrue(result.success());
			verify(alertRepository).findActiveAlertsByTier("PRO");
		}

	}

	@Nested
	@DisplayName("isRunning()")
	class ConcurrencyTests {

		@Test
		@DisplayName("Should return false when not running")
		void shouldReturnFalseWhenNotRunning() {
			assertFalse(dispatchJob.isRunning());
		}

		@Test
		@DisplayName("Should prevent concurrent execution")
		void shouldPreventConcurrentExecution() throws Exception {
			// Manually set running flag
			var field = DispatchJob.class.getDeclaredField("isRunning");
			field.setAccessible(true);
			var isRunning = (java.util.concurrent.atomic.AtomicBoolean) field.get(dispatchJob);
			isRunning.set(true);

			DispatchJob.DispatchResult result = dispatchJob.execute("TIER1");

			assertFalse(result.success());
			assertEquals("Already running", result.errorMessage());
			verify(alertRepository, never()).findActiveAlertsByTier(any());
		}

	}

}
