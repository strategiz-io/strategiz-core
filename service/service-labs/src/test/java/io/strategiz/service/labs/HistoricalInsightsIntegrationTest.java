package io.strategiz.service.labs;

import io.strategiz.business.aichat.LLMRouter;
import io.strategiz.business.aichat.prompt.AIStrategyPrompts;
import io.strategiz.business.historicalinsights.model.IndicatorRanking;
import io.strategiz.business.historicalinsights.model.SymbolInsights;
import io.strategiz.business.historicalinsights.service.HistoricalInsightsCacheService;
import io.strategiz.business.historicalinsights.service.HistoricalInsightsService;
import io.strategiz.client.base.llm.model.LLMResponse;
import io.strategiz.service.labs.model.AIStrategyRequest;
import io.strategiz.service.labs.model.AIStrategyResponse;
import io.strategiz.service.labs.service.AIStrategyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration test for Alpha Mode functionality.
 * Tests the complete flow from request to response with historical insights.
 */
class AlphaModeIntegrationTest {

	@Mock
	private LLMRouter llmRouter;

	@Mock
	private HistoricalInsightsService historicalInsightsService;

	@Mock
	private HistoricalInsightsCacheService cacheService;

	private AIStrategyService aiStrategyService;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		aiStrategyService = new AIStrategyService(llmRouter, historicalInsightsService, cacheService);

		// Mock default LLM model
		when(llmRouter.getDefaultModel()).thenReturn("gemini-2.5-flash");
	}

	@Test
	void testAlphaMode_WithAAPL_GeneratesEnhancedStrategy() {
		// Given: A request with Alpha Mode enabled for AAPL
		AIStrategyRequest request = createAlphaModeRequest("Generate an RSI strategy for AAPL", "AAPL");

		// Mock historical insights for AAPL
		SymbolInsights aapl = createMockInsights("AAPL", "MEDIUM", "BULLISH", 65.0);
		when(historicalInsightsService.analyzeSymbolForStrategyGeneration(eq("AAPL"), anyString(), anyInt(),
				anyBoolean())).thenReturn(aapl);
		when(cacheService.getCachedInsights(anyString())).thenReturn(java.util.Optional.empty());

		// Mock LLM response
		String mockLLMResponse = """
				{
				  "visualConfig": {
				    "name": "AAPL RSI Strategy",
				    "description": "RSI-based mean reversion for AAPL",
				    "symbol": "AAPL",
				    "rules": []
				  },
				  "pythonCode": "# AAPL Strategy",
				  "summaryCard": "Based on 7 years of AAPL data, RSI achieved 65% win rate.",
				  "riskLevel": "MEDIUM"
				}
				""";
		when(llmRouter.generateContent(anyString(), anyList(), anyString()))
			.thenReturn(Mono.just(LLMResponse.success(mockLLMResponse, "gemini-2.5-flash", "mock-request-id")));

		// When: Generate strategy
		AIStrategyResponse response = aiStrategyService.generateStrategy(request);

		// Then: Response should include Alpha Mode insights
		assertTrue(response.isSuccess(), "Strategy generation should succeed");
		assertNotNull(response.getHistoricalInsights(), "Should include historical insights");
		assertEquals(Boolean.TRUE, response.getAlphaModeUsed(), "Should indicate Alpha Mode was used");
		assertEquals("AAPL", response.getHistoricalInsights().getSymbol());
		assertEquals(65.0, response.getHistoricalInsights().getAvgWinRate());

		// Verify insights were fetched
		verify(historicalInsightsService, times(1)).analyzeSymbolForStrategyGeneration(eq("AAPL"), anyString(),
				anyInt(), anyBoolean());

		// Verify insights were cached
		verify(cacheService, times(1)).cacheInsights(anyString(), eq(aapl));
	}

	@Test
	void testAlphaMode_WithTSLA_HighVolatility() {
		// Given: TSLA with HIGH volatility
		AIStrategyRequest request = createAlphaModeRequest("Create a momentum strategy for TSLA", "TSLA");

		SymbolInsights tesla = createMockInsights("TSLA", "HIGH", "BULLISH", 58.0);
		when(historicalInsightsService.analyzeSymbolForStrategyGeneration(eq("TSLA"), anyString(), anyInt(),
				anyBoolean())).thenReturn(tesla);
		when(cacheService.getCachedInsights(anyString())).thenReturn(java.util.Optional.empty());

		String mockLLMResponse = """
				{
				  "visualConfig": { "name": "TSLA Momentum", "symbol": "TSLA", "rules": [] },
				  "pythonCode": "# TSLA",
				  "summaryCard": "TSLA high volatility momentum strategy",
				  "riskLevel": "HIGH"
				}
				""";
		when(llmRouter.generateContent(anyString(), anyList(), anyString()))
			.thenReturn(Mono.just(LLMResponse.success(mockLLMResponse, "gemini-2.5-flash", "mock-request-id")));

		// When
		AIStrategyResponse response = aiStrategyService.generateStrategy(request);

		// Then
		assertTrue(response.isSuccess());
		assertEquals("HIGH", response.getHistoricalInsights().getVolatilityRegime());
		assertEquals(58.0, response.getHistoricalInsights().getAvgWinRate());
	}

	@Test
	void testAlphaMode_WithSPY_MeanReverting() {
		// Given: SPY with mean-reverting characteristics
		AIStrategyRequest request = createAlphaModeRequest("Build a mean reversion strategy for SPY", "SPY");

		SymbolInsights spy = createMockInsights("SPY", "LOW", "SIDEWAYS", 70.0);
		spy.setMeanReverting(true);
		when(historicalInsightsService.analyzeSymbolForStrategyGeneration(eq("SPY"), anyString(), anyInt(),
				anyBoolean())).thenReturn(spy);
		when(cacheService.getCachedInsights(anyString())).thenReturn(java.util.Optional.empty());

		String mockLLMResponse = """
				{
				  "visualConfig": { "name": "SPY Mean Reversion", "symbol": "SPY", "rules": [] },
				  "pythonCode": "# SPY",
				  "summaryCard": "SPY mean reversion with 70% win rate",
				  "riskLevel": "LOW"
				}
				""";
		when(llmRouter.generateContent(anyString(), anyList(), anyString()))
			.thenReturn(Mono.just(LLMResponse.success(mockLLMResponse, "gemini-2.5-flash", "mock-request-id")));

		// When
		AIStrategyResponse response = aiStrategyService.generateStrategy(request);

		// Then
		assertTrue(response.isSuccess());
		assertTrue(response.getHistoricalInsights().isMeanReverting());
		assertEquals("SIDEWAYS", response.getHistoricalInsights().getTrendDirection());
	}

	@Test
	void testAlphaMode_UsesCache_OnSecondRequest() {
		// Given: First request for AAPL
		AIStrategyRequest request1 = createAlphaModeRequest("RSI strategy for AAPL", "AAPL");
		SymbolInsights aapl = createMockInsights("AAPL", "MEDIUM", "BULLISH", 65.0);

		// First call: No cache
		when(cacheService.getCachedInsights(anyString())).thenReturn(java.util.Optional.empty());
		when(historicalInsightsService.analyzeSymbolForStrategyGeneration(eq("AAPL"), anyString(), anyInt(),
				anyBoolean())).thenReturn(aapl);

		String mockLLMResponse = """
				{
				  "visualConfig": { "name": "AAPL RSI", "symbol": "AAPL", "rules": [] },
				  "pythonCode": "# AAPL",
				  "summaryCard": "AAPL strategy",
				  "riskLevel": "MEDIUM"
				}
				""";
		when(llmRouter.generateContent(anyString(), anyList(), anyString()))
			.thenReturn(Mono.just(LLMResponse.success(mockLLMResponse, "gemini-2.5-flash", "mock-request-id")));

		// When: First request
		aiStrategyService.generateStrategy(request1);

		// Then: Insights were fetched from service
		verify(historicalInsightsService, times(1)).analyzeSymbolForStrategyGeneration(eq("AAPL"), anyString(),
				anyInt(), anyBoolean());

		// Given: Second request for AAPL (should use cache)
		AIStrategyRequest request2 = createAlphaModeRequest("MACD strategy for AAPL", "AAPL");
		when(cacheService.getCachedInsights(anyString())).thenReturn(java.util.Optional.of(aapl));

		// When: Second request
		aiStrategyService.generateStrategy(request2);

		// Then: Service not called again (cache used)
		verify(historicalInsightsService, times(1)).analyzeSymbolForStrategyGeneration(eq("AAPL"), anyString(),
				anyInt(), anyBoolean());
		verify(cacheService, times(2)).getCachedInsights(anyString());
	}

	@Test
	void testAlphaMode_WithFundamentals_IncludesFundamentalData() {
		// Given: Request with fundamentals enabled
		AIStrategyRequest request = createAlphaModeRequest("Value strategy for AAPL", "AAPL");
		request.getAlphaOptions().setUseFundamentals(true);

		SymbolInsights aapl = createMockInsights("AAPL", "MEDIUM", "BULLISH", 65.0);
		// Note: fundamentals would be added in the real service
		when(historicalInsightsService.analyzeSymbolForStrategyGeneration(eq("AAPL"), anyString(), anyInt(),
				eq(true))).thenReturn(aapl);
		when(cacheService.getCachedInsights(anyString())).thenReturn(java.util.Optional.empty());

		String mockLLMResponse = """
				{
				  "visualConfig": { "name": "AAPL Value", "symbol": "AAPL", "rules": [] },
				  "pythonCode": "# AAPL",
				  "summaryCard": "AAPL value strategy",
				  "riskLevel": "MEDIUM"
				}
				""";
		when(llmRouter.generateContent(anyString(), anyList(), anyString()))
			.thenReturn(Mono.just(LLMResponse.success(mockLLMResponse, "gemini-2.5-flash", "mock-request-id")));

		// When
		AIStrategyResponse response = aiStrategyService.generateStrategy(request);

		// Then
		assertTrue(response.isSuccess());
		verify(historicalInsightsService, times(1)).analyzeSymbolForStrategyGeneration(eq("AAPL"), anyString(),
				anyInt(), eq(true));
	}

	@Test
	void testAlphaMode_Disabled_NoInsights() {
		// Given: Request with Alpha Mode disabled
		AIStrategyRequest request = new AIStrategyRequest();
		request.setPrompt("Generate RSI strategy for AAPL");
		request.setAlphaMode(false);

		AIStrategyRequest.StrategyContext context = new AIStrategyRequest.StrategyContext();
		context.setSymbols(List.of("AAPL"));
		request.setContext(context);

		String mockLLMResponse = """
				{
				  "visualConfig": { "name": "AAPL RSI", "symbol": "AAPL", "rules": [] },
				  "pythonCode": "# AAPL",
				  "summaryCard": "Basic RSI strategy",
				  "riskLevel": "MEDIUM"
				}
				""";
		when(llmRouter.generateContent(anyString(), anyList(), anyString()))
			.thenReturn(Mono.just(LLMResponse.success(mockLLMResponse, "gemini-2.5-flash", "mock-request-id")));

		// When
		AIStrategyResponse response = aiStrategyService.generateStrategy(request);

		// Then: No insights fetched or included
		assertTrue(response.isSuccess());
		assertNull(response.getHistoricalInsights());
		assertNotEquals(Boolean.TRUE, response.getAlphaModeUsed());
		verify(historicalInsightsService, never()).analyzeSymbolForStrategyGeneration(anyString(), anyString(),
				anyInt(), anyBoolean());
	}

	@Test
	void testPromptEnhancement_IncludesHistoricalData() {
		// Given: Alpha Mode request
		SymbolInsights insights = createMockInsights("AAPL", "MEDIUM", "BULLISH", 65.0);

		// When: Build Alpha Mode prompt
		String enhancedPrompt = AIStrategyPrompts.buildAlphaModePrompt(insights);

		// Then: Prompt includes key historical data
		assertTrue(enhancedPrompt.contains("ALPHA MODE: HISTORICAL MARKET ANALYSIS"),
				"Should have Alpha Mode header");
		assertTrue(enhancedPrompt.contains("Symbol: AAPL"), "Should include symbol");
		assertTrue(enhancedPrompt.contains("Volatility Regime: MEDIUM"), "Should include volatility regime");
		assertTrue(enhancedPrompt.contains("Trend Direction: BULLISH"), "Should include trend direction");
		assertTrue(enhancedPrompt.contains("Historical Avg Win Rate: 65.0%"), "Should include win rate");
		assertTrue(enhancedPrompt.contains("TOP PERFORMING INDICATORS"), "Should include indicator rankings");
		assertTrue(enhancedPrompt.contains("RSI"), "Should mention RSI indicator");
		assertTrue(enhancedPrompt.contains("ALPHA MODE INSTRUCTIONS"), "Should include AI instructions");
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
		rankings.add(new IndicatorRanking("RSI", 0.65, rsiSettings, "Effective for mean-reversion strategies"));

		Map<String, Object> macdSettings = new HashMap<>();
		macdSettings.put("fast", 12);
		macdSettings.put("slow", 26);
		macdSettings.put("signal", 9);
		rankings.add(new IndicatorRanking("MACD", 0.60, macdSettings, "Strong trend-following indicator"));

		insights.setTopIndicators(rankings);

		// Mock optimal parameters
		Map<String, Object> optimalParams = new HashMap<>();
		optimalParams.put("stop_loss_percent", 3.0);
		optimalParams.put("take_profit_percent", 9.0);
		insights.setOptimalParameters(optimalParams);

		return insights;
	}

}
