package io.strategiz.service.labs.controller;

import io.strategiz.business.aichat.LLMRouter;
import io.strategiz.business.historicalinsights.model.IndicatorRanking;
import io.strategiz.business.historicalinsights.model.SymbolInsights;
import io.strategiz.business.historicalinsights.service.HistoricalInsightsCacheService;
import io.strategiz.business.historicalinsights.service.HistoricalInsightsService;
import io.strategiz.business.preferences.service.SubscriptionService;
import io.strategiz.client.base.llm.model.LLMResponse;
import io.strategiz.data.featureflags.service.FeatureFlagService;
import io.strategiz.framework.authorization.context.AuthenticatedUser;
import io.strategiz.service.labs.model.AIStrategyRequest;
import io.strategiz.service.labs.model.AIStrategyResponse;
import io.strategiz.service.labs.service.AIStrategyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Controller-level integration test for Alpha Mode functionality.
 * Tests the AIStrategyController with Alpha Mode enabled, verifying historical insights flow.
 */
class AlphaModeControllerIntegrationTest {

	@Mock
	private LLMRouter llmRouter;

	@Mock
	private HistoricalInsightsService historicalInsightsService;

	@Mock
	private HistoricalInsightsCacheService cacheService;

	@Mock
	private SubscriptionService subscriptionService;

	@Mock
	private FeatureFlagService featureFlagService;

	private AIStrategyService aiStrategyService;

	private AIStrategyController controller;

	private static final String TEST_USER_ID = "test-user-123";
	private AuthenticatedUser testUser;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);

		// Create service with mocked dependencies
		aiStrategyService = new AIStrategyService(llmRouter, historicalInsightsService, cacheService);

		// Create controller with all dependencies
		controller = new AIStrategyController(aiStrategyService, subscriptionService, featureFlagService);

		// Setup test user
		testUser = AuthenticatedUser.builder()
				.userId(TEST_USER_ID)
				.acr("1")
				.build();

		// Default mocks for successful flow
		when(llmRouter.getDefaultModel()).thenReturn("gemini-2.5-flash");
		when(featureFlagService.isLabsAIEnabled()).thenReturn(true);
		when(featureFlagService.isAlphaModeEnabled()).thenReturn(true);
		when(subscriptionService.canSendMessage(anyString())).thenReturn(true);
		when(subscriptionService.canUseAlphaMode(anyString())).thenReturn(true);
		when(cacheService.getCachedInsights(anyString())).thenReturn(java.util.Optional.empty());
	}

	@Test
	void testGenerateStrategy_WithAlphaMode_ReturnsHistoricalInsights() {
		// Given: Alpha Mode request for AAPL
		AIStrategyRequest request = createAlphaModeRequest("Generate an RSI mean reversion strategy for AAPL", "AAPL");

		// Mock historical insights
		SymbolInsights aapl = createMockInsights("AAPL", "MEDIUM", "BULLISH", 67.5);
		when(historicalInsightsService.analyzeSymbolForStrategyGeneration(eq("AAPL"), eq("1D"), eq(2600), eq(false)))
				.thenReturn(aapl);

		// Mock LLM response
		String mockLLMResponse = """
				{
				  "visualConfig": {
				    "name": "AAPL RSI Mean Reversion",
				    "description": "Data-driven RSI strategy for AAPL based on 7 years of analysis",
				    "symbol": "AAPL",
				    "rules": [{
				      "id": "entry-1",
				      "type": "entry",
				      "action": "BUY",
				      "conditions": [{
				        "id": "c1",
				        "indicator": "rsi_14",
				        "comparator": "lt",
				        "value": 30,
				        "valueType": "number"
				      }],
				      "logic": "AND"
				    }],
				    "riskSettings": {
				      "stopLoss": 3.0,
				      "takeProfit": 9.0,
				      "positionSize": 5.0,
				      "maxPositions": 3
				    }
				  },
				  "pythonCode": "# AAPL RSI Strategy\\nSYMBOL = 'AAPL'\\nSTOP_LOSS = 3.0\\nTAKE_PROFIT = 9.0",
				  "summaryCard": "Based on 7 years of AAPL data, RSI(14) with 30/70 thresholds achieved a 67.5% win rate with MEDIUM volatility.",
				  "riskLevel": "MEDIUM",
				  "detectedIndicators": ["RSI"]
				}
				""";
		when(llmRouter.generateContent(anyString(), anyList(), anyString()))
				.thenReturn(Mono.just(LLMResponse.success(mockLLMResponse, "gemini-2.5-flash", "test-request-id")));

		// When: Call controller
		ResponseEntity<AIStrategyResponse> responseEntity = controller.generateStrategy(request, testUser);

		// Then: Verify response
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		AIStrategyResponse response = responseEntity.getBody();
		assertNotNull(response);
		assertTrue(response.isSuccess(), "Strategy generation should succeed");
		assertNotNull(response.getHistoricalInsights(), "Should include historical insights");
		assertEquals(Boolean.TRUE, response.getAlphaModeUsed(), "Should indicate Alpha Mode was used");
		assertEquals("AAPL", response.getHistoricalInsights().getSymbol());
		assertEquals("MEDIUM", response.getHistoricalInsights().getVolatilityRegime());
		assertEquals("BULLISH", response.getHistoricalInsights().getTrendDirection());
		assertEquals(67.5, response.getHistoricalInsights().getAvgWinRate());
		assertEquals(2, response.getHistoricalInsights().getTopIndicators().size());
		assertTrue(response.getSummaryCard().contains("67.5% win rate"));

		// Verify historical insights were fetched
		verify(historicalInsightsService, times(1))
				.analyzeSymbolForStrategyGeneration(eq("AAPL"), eq("1D"), eq(2600), eq(false));
		verify(cacheService, times(1)).cacheInsights(anyString(), eq(aapl));
		verify(subscriptionService, times(1)).canUseAlphaMode(TEST_USER_ID);
		verify(subscriptionService, times(1)).recordMessageUsage(TEST_USER_ID);
	}

	@Test
	void testGenerateStrategy_AlphaModeDisabled_Returns503() {
		// Given: Alpha Mode feature flag is disabled
		when(featureFlagService.isAlphaModeEnabled()).thenReturn(false);

		AIStrategyRequest request = createAlphaModeRequest("Generate strategy for TSLA", "TSLA");

		// When: Call controller
		ResponseEntity<AIStrategyResponse> responseEntity = controller.generateStrategy(request, testUser);

		// Then: Should return 503 Service Unavailable
		assertEquals(HttpStatus.SERVICE_UNAVAILABLE, responseEntity.getStatusCode());
		AIStrategyResponse response = responseEntity.getBody();
		assertNotNull(response);
		assertFalse(response.isSuccess());
		assertTrue(response.getError().contains("Alpha Mode is currently unavailable"));

		// Verify no insights were fetched
		verify(historicalInsightsService, never()).analyzeSymbolForStrategyGeneration(anyString(), anyString(), anyInt(), anyBoolean());
	}

	@Test
	void testGenerateStrategy_InsufficientSubscription_Returns403() {
		// Given: User doesn't have proper subscription tier
		when(subscriptionService.canUseAlphaMode(TEST_USER_ID)).thenReturn(false);

		AIStrategyRequest request = createAlphaModeRequest("Generate strategy for SPY", "SPY");

		// When: Call controller
		ResponseEntity<AIStrategyResponse> responseEntity = controller.generateStrategy(request, testUser);

		// Then: Should return 403 Forbidden
		assertEquals(HttpStatus.FORBIDDEN, responseEntity.getStatusCode());
		AIStrategyResponse response = responseEntity.getBody();
		assertNotNull(response);
		assertFalse(response.isSuccess());
		assertTrue(response.getError().contains("TRADER or STRATEGIST tier"));

		// Verify no insights were fetched
		verify(historicalInsightsService, never()).analyzeSymbolForStrategyGeneration(anyString(), anyString(), anyInt(), anyBoolean());
	}

	@Test
	void testGenerateStrategy_WithoutAlphaMode_NoHistoricalInsights() {
		// Given: Request WITHOUT Alpha Mode
		AIStrategyRequest request = new AIStrategyRequest();
		request.setPrompt("Generate a simple RSI strategy for AAPL");
		request.setAlphaMode(false); // Alpha Mode disabled

		AIStrategyRequest.StrategyContext context = new AIStrategyRequest.StrategyContext();
		context.setSymbols(List.of("AAPL"));
		context.setTimeframe("1D");
		request.setContext(context);

		// Mock LLM response
		String mockLLMResponse = """
				{
				  "visualConfig": { "name": "Basic RSI", "symbol": "AAPL", "rules": [] },
				  "pythonCode": "# Basic strategy",
				  "summaryCard": "Basic RSI strategy without historical analysis",
				  "riskLevel": "MEDIUM"
				}
				""";
		when(llmRouter.generateContent(anyString(), anyList(), anyString()))
				.thenReturn(Mono.just(LLMResponse.success(mockLLMResponse, "gemini-2.5-flash", "test-request-id")));

		// When: Call controller
		ResponseEntity<AIStrategyResponse> responseEntity = controller.generateStrategy(request, testUser);

		// Then: Verify response
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		AIStrategyResponse response = responseEntity.getBody();
		assertNotNull(response);
		assertTrue(response.isSuccess());
		assertNotEquals(Boolean.TRUE, response.getAlphaModeUsed());
		assertNull(response.getHistoricalInsights(), "Should NOT include historical insights");

		// No historical insights fetched
		verify(historicalInsightsService, never()).analyzeSymbolForStrategyGeneration(anyString(), anyString(), anyInt(), anyBoolean());
		verify(subscriptionService, never()).canUseAlphaMode(anyString());
	}

	@Test
	void testGenerateStrategy_AlphaMode_WithFundamentals() {
		// Given: Alpha Mode request with fundamentals enabled
		AIStrategyRequest request = createAlphaModeRequest("Create a value strategy for AAPL using fundamentals", "AAPL");
		request.getAlphaOptions().setUseFundamentals(true);

		// Mock historical insights with fundamentals
		SymbolInsights aapl = createMockInsights("AAPL", "LOW", "BULLISH", 72.0);
		when(historicalInsightsService.analyzeSymbolForStrategyGeneration(eq("AAPL"), eq("1D"), eq(2600), eq(true)))
				.thenReturn(aapl);

		// Mock LLM response
		String mockLLMResponse = """
				{
				  "visualConfig": { "name": "AAPL Value Strategy", "symbol": "AAPL", "rules": [] },
				  "pythonCode": "# Value strategy",
				  "summaryCard": "Data-driven value strategy with fundamental analysis",
				  "riskLevel": "LOW"
				}
				""";
		when(llmRouter.generateContent(anyString(), anyList(), anyString()))
				.thenReturn(Mono.just(LLMResponse.success(mockLLMResponse, "gemini-2.5-flash", "test-request-id")));

		// When: Call controller
		ResponseEntity<AIStrategyResponse> responseEntity = controller.generateStrategy(request, testUser);

		// Then: Verify response
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		AIStrategyResponse response = responseEntity.getBody();
		assertNotNull(response);
		assertTrue(response.isSuccess());
		assertEquals(Boolean.TRUE, response.getAlphaModeUsed());
		assertEquals("AAPL", response.getHistoricalInsights().getSymbol());

		// Verify fundamentals were requested
		verify(historicalInsightsService, times(1))
				.analyzeSymbolForStrategyGeneration(eq("AAPL"), eq("1D"), eq(2600), eq(true));
	}

	@Test
	void testGenerateStrategy_AlphaMode_HighVolatilitySymbol() {
		// Given: High volatility symbol (TSLA)
		AIStrategyRequest request = createAlphaModeRequest("Generate a momentum breakout strategy for TSLA", "TSLA");

		// Mock high volatility insights
		SymbolInsights tesla = createMockInsights("TSLA", "HIGH", "BULLISH", 58.5);
		tesla.setAvgDailyRange(4.5); // High daily range
		when(historicalInsightsService.analyzeSymbolForStrategyGeneration(eq("TSLA"), eq("1D"), eq(2600), eq(false)))
				.thenReturn(tesla);

		// Mock LLM response with wider stops
		String mockLLMResponse = """
				{
				  "visualConfig": {
				    "name": "TSLA Momentum Breakout",
				    "symbol": "TSLA",
				    "rules": [],
				    "riskSettings": {
				      "stopLoss": 5.0,
				      "takeProfit": 15.0
				    }
				  },
				  "pythonCode": "# TSLA strategy with wider stops",
				  "summaryCard": "HIGH volatility TSLA requires 5% stops and 15% targets based on historical data.",
				  "riskLevel": "HIGH"
				}
				""";
		when(llmRouter.generateContent(anyString(), anyList(), anyString()))
				.thenReturn(Mono.just(LLMResponse.success(mockLLMResponse, "gemini-2.5-flash", "test-request-id")));

		// When: Call controller
		ResponseEntity<AIStrategyResponse> responseEntity = controller.generateStrategy(request, testUser);

		// Then: Verify response
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		AIStrategyResponse response = responseEntity.getBody();
		assertNotNull(response);
		assertTrue(response.isSuccess());
		assertEquals("HIGH", response.getHistoricalInsights().getVolatilityRegime());
		assertEquals(4.5, response.getHistoricalInsights().getAvgDailyRange());
		assertTrue(response.getSummaryCard().contains("HIGH volatility"));

		// Verify correct insights were used
		verify(historicalInsightsService, times(1))
				.analyzeSymbolForStrategyGeneration(eq("TSLA"), eq("1D"), eq(2600), eq(false));
	}

	@Test
	void testGenerateStrategy_AlphaMode_MeanRevertingSymbol() {
		// Given: Mean reverting symbol (SPY)
		AIStrategyRequest request = createAlphaModeRequest("Create a mean reversion strategy for SPY", "SPY");

		// Mock mean reverting insights
		SymbolInsights spy = createMockInsights("SPY", "LOW", "SIDEWAYS", 70.0);
		spy.setMeanReverting(true);
		when(historicalInsightsService.analyzeSymbolForStrategyGeneration(eq("SPY"), eq("1D"), eq(2600), eq(false)))
				.thenReturn(spy);

		// Mock LLM response emphasizing mean reversion
		String mockLLMResponse = """
				{
				  "visualConfig": { "name": "SPY Mean Reversion", "symbol": "SPY", "rules": [] },
				  "pythonCode": "# SPY mean reversion",
				  "summaryCard": "SPY exhibits mean-reverting behavior with 70% win rate using RSI oversold/overbought levels.",
				  "riskLevel": "LOW"
				}
				""";
		when(llmRouter.generateContent(anyString(), anyList(), anyString()))
				.thenReturn(Mono.just(LLMResponse.success(mockLLMResponse, "gemini-2.5-flash", "test-request-id")));

		// When: Call controller
		ResponseEntity<AIStrategyResponse> responseEntity = controller.generateStrategy(request, testUser);

		// Then: Verify response
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		AIStrategyResponse response = responseEntity.getBody();
		assertNotNull(response);
		assertTrue(response.isSuccess());
		assertTrue(response.getHistoricalInsights().isMeanReverting());
		assertEquals("SIDEWAYS", response.getHistoricalInsights().getTrendDirection());
		assertTrue(response.getSummaryCard().contains("mean-reverting"));

		// Verify insights were fetched
		verify(historicalInsightsService, times(1))
				.analyzeSymbolForStrategyGeneration(eq("SPY"), eq("1D"), eq(2600), eq(false));
	}

	@Test
	void testGenerateStrategy_AlphaMode_CachingWorks() {
		// Given: Second request for same symbol should use cache
		AIStrategyRequest request = createAlphaModeRequest("Generate MACD strategy for AAPL", "AAPL");

		SymbolInsights cachedInsights = createMockInsights("AAPL", "MEDIUM", "BULLISH", 65.0);
		when(cacheService.getCachedInsights(anyString())).thenReturn(java.util.Optional.of(cachedInsights));

		String mockLLMResponse = """
				{
				  "visualConfig": { "name": "AAPL MACD", "symbol": "AAPL", "rules": [] },
				  "pythonCode": "# AAPL MACD",
				  "summaryCard": "MACD strategy for AAPL",
				  "riskLevel": "MEDIUM"
				}
				""";
		when(llmRouter.generateContent(anyString(), anyList(), anyString()))
				.thenReturn(Mono.just(LLMResponse.success(mockLLMResponse, "gemini-2.5-flash", "test-request-id")));

		// When: Call controller
		ResponseEntity<AIStrategyResponse> responseEntity = controller.generateStrategy(request, testUser);

		// Then: Verify response
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		AIStrategyResponse response = responseEntity.getBody();
		assertNotNull(response);
		assertTrue(response.isSuccess());
		assertNotNull(response.getHistoricalInsights());

		// Historical insights service should NOT be called (cache used)
		verify(historicalInsightsService, never())
				.analyzeSymbolForStrategyGeneration(anyString(), anyString(), anyInt(), anyBoolean());
		verify(cacheService, times(1)).getCachedInsights(anyString());
	}

	// Helper methods

	private AIStrategyRequest createAlphaModeRequest(String prompt, String symbol) {
		AIStrategyRequest request = new AIStrategyRequest();
		request.setPrompt(prompt);
		request.setAlphaMode(true);

		AIStrategyRequest.AlphaModeOptions options = new AIStrategyRequest.AlphaModeOptions();
		options.setLookbackDays(2600);
		options.setUseFundamentals(false);
		options.setForceRefresh(false);
		request.setAlphaOptions(options);

		AIStrategyRequest.StrategyContext context = new AIStrategyRequest.StrategyContext();
		context.setSymbols(List.of(symbol));
		context.setTimeframe("1D");
		request.setContext(context);

		return request;
	}

	private SymbolInsights createMockInsights(String symbol, String volatilityRegime, String trendDirection,
			double winRate) {
		SymbolInsights insights = new SymbolInsights();
		insights.setSymbol(symbol);
		insights.setTimeframe("1D");
		insights.setDaysAnalyzed(2600);
		insights.setAvgVolatility(2.50);
		insights.setVolatilityRegime(volatilityRegime);
		insights.setAvgDailyRange(1.8);
		insights.setTrendDirection(trendDirection);
		insights.setTrendStrength(0.65);
		insights.setMeanReverting(false);
		insights.setAvgMaxDrawdown(8.5);
		insights.setAvgWinRate(winRate);
		insights.setRecommendedRiskLevel("MEDIUM");

		// Mock top indicators
		List<IndicatorRanking> rankings = new ArrayList<>();

		Map<String, Object> rsiSettings = new HashMap<>();
		rsiSettings.put("period", 14);
		rsiSettings.put("oversold", 30);
		rsiSettings.put("overbought", 70);
		rankings.add(new IndicatorRanking("RSI", 0.68, rsiSettings, "Most effective for mean-reversion on this symbol"));

		Map<String, Object> macdSettings = new HashMap<>();
		macdSettings.put("fast", 12);
		macdSettings.put("slow", 26);
		macdSettings.put("signal", 9);
		rankings.add(new IndicatorRanking("MACD", 0.62, macdSettings, "Strong trend confirmation indicator"));

		insights.setTopIndicators(rankings);

		// Mock optimal parameters
		Map<String, Object> optimalParams = new HashMap<>();
		optimalParams.put("stop_loss_percent", 3.0);
		optimalParams.put("take_profit_percent", 9.0);
		optimalParams.put("position_size_percent", 5.0);
		insights.setOptimalParameters(optimalParams);

		return insights;
	}
}
