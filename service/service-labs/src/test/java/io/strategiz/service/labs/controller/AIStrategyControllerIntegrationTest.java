package io.strategiz.service.labs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.strategiz.business.preferences.service.SubscriptionService;
import io.strategiz.data.featureflags.service.FeatureFlagService;
import io.strategiz.framework.authorization.context.AuthenticatedUser;
import io.strategiz.framework.authorization.context.SecurityContextHolder;
import io.strategiz.framework.authorization.resolver.AuthUserArgumentResolver;
import io.strategiz.framework.exception.ErrorMessageService;
import io.strategiz.service.labs.model.AIStrategyRequest;
import io.strategiz.service.labs.model.AIStrategyResponse;
import io.strategiz.service.labs.service.AIStrategyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for AI Strategy Controller endpoints.
 * Tests all /v1/labs/ai/* endpoints for proper request handling,
 * validation, and response formatting.
 *
 * Uses standalone MockMvc setup for fast, isolated controller testing.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AI Strategy Controller Tests")
class AIStrategyControllerIntegrationTest {

	private static final String BASE_URL = "/v1/labs/ai";

	private MockMvc mockMvc;

	private ObjectMapper objectMapper;

	@Mock
	private AIStrategyService aiStrategyService;

	@Mock
	private SubscriptionService subscriptionService;

	@Mock
	private FeatureFlagService featureFlagService;

	@Mock
	private ErrorMessageService errorMessageService;

	@InjectMocks
	private AIStrategyController aiStrategyController;

	private static final String TEST_USER_ID = "test-user-123";

	@BeforeEach
	void setUp() {
		objectMapper = new ObjectMapper();
		mockMvc = MockMvcBuilders
			.standaloneSetup(aiStrategyController)
			.setCustomArgumentResolvers(new AuthUserArgumentResolver())
			.build();

		// Set up authenticated user in security context
		AuthenticatedUser testUser = AuthenticatedUser.builder()
			.userId(TEST_USER_ID)
			.acr("1")
			.build();
		SecurityContextHolder.getContext().setAuthenticatedUser(testUser);

		// Default: Labs AI is enabled
		when(featureFlagService.isLabsAIEnabled()).thenReturn(true);
		when(featureFlagService.isHistoricalInsightsEnabled()).thenReturn(true);

		// Default: User can send messages
		when(subscriptionService.canSendMessage(anyString())).thenReturn(true);
		when(subscriptionService.canUseHistoricalInsights(anyString())).thenReturn(true);
	}

	// ============================================================
	// Generate Strategy Endpoint Tests
	// ============================================================

	@Nested
	@DisplayName("POST /v1/labs/ai/generate-strategy")
	class GenerateStrategyTests {

		@Test
		@DisplayName("Should generate strategy successfully with valid request")
		void shouldGenerateStrategySuccessfully() throws Exception {
			// Arrange
			AIStrategyRequest request = new AIStrategyRequest();
			request.setPrompt("Buy AAPL when RSI is below 30 and MACD crosses above signal line");
			request.setModel("gemini-2.5-flash");

			AIStrategyResponse mockResponse = createSuccessResponse();
			when(aiStrategyService.generateStrategy(any(AIStrategyRequest.class))).thenReturn(mockResponse);

			// Act & Assert
			mockMvc.perform(post(BASE_URL + "/generate-strategy")
					.contentType(MediaType.APPLICATION_JSON)
					.header("X-User-Id", "test-user-123")
					.header("X-Auth-Acr", "1")
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.pythonCode").exists())
				.andExpect(jsonPath("$.visualConfig").exists())
				.andExpect(jsonPath("$.detectedIndicators").isArray());

			verify(aiStrategyService).generateStrategy(any(AIStrategyRequest.class));
			verify(subscriptionService).recordMessageUsage("test-user-123");
		}

		@Test
		@DisplayName("Should return 503 when Labs AI is disabled")
		void shouldReturn503WhenLabsAIDisabled() throws Exception {
			// Arrange
			when(featureFlagService.isLabsAIEnabled()).thenReturn(false);

			AIStrategyRequest request = new AIStrategyRequest();
			request.setPrompt("Buy AAPL when RSI is below 30");

			// Act & Assert
			mockMvc.perform(post(BASE_URL + "/generate-strategy")
					.contentType(MediaType.APPLICATION_JSON)
					.header("X-User-Id", "test-user-123")
					.header("X-Auth-Acr", "1")
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.error").value(containsString("temporarily unavailable")));

			verify(aiStrategyService, never()).generateStrategy(any());
		}

		@Test
		@DisplayName("Should return 429 when daily AI chat limit exceeded")
		void shouldReturn429WhenDailyLimitExceeded() throws Exception {
			// Arrange
			when(subscriptionService.canSendMessage(anyString())).thenReturn(false);

			AIStrategyRequest request = new AIStrategyRequest();
			request.setPrompt("Buy AAPL when RSI is below 30");

			// Act & Assert
			mockMvc.perform(post(BASE_URL + "/generate-strategy")
					.contentType(MediaType.APPLICATION_JSON)
					.header("X-User-Id", "test-user-123")
					.header("X-Auth-Acr", "1")
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.error").value(containsString("Daily AI chat limit exceeded")));

			verify(aiStrategyService, never()).generateStrategy(any());
		}

		@Test
		@DisplayName("Should return 403 when Historical Insights requested without subscription")
		void shouldReturn403ForHistoricalInsightsWithoutSubscription() throws Exception {
			// Arrange
			when(subscriptionService.canUseHistoricalInsights(anyString())).thenReturn(false);

			AIStrategyRequest request = new AIStrategyRequest();
			request.setPrompt("Buy AAPL when RSI is below 30");
			request.setUseHistoricalInsights(true);

			// Act & Assert
			mockMvc.perform(post(BASE_URL + "/generate-strategy")
					.contentType(MediaType.APPLICATION_JSON)
					.header("X-User-Id", "test-user-123")
					.header("X-Auth-Acr", "1")
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.error").value(containsString("requires a paid subscription")));
		}

		@Test
		@DisplayName("Should handle service errors gracefully")
		void shouldHandleServiceErrorsGracefully() throws Exception {
			// Arrange
			AIStrategyRequest request = new AIStrategyRequest();
			request.setPrompt("Buy AAPL when RSI is below 30");

			when(aiStrategyService.generateStrategy(any())).thenThrow(new RuntimeException("LLM service unavailable"));

			// Act & Assert
			mockMvc.perform(post(BASE_URL + "/generate-strategy")
					.contentType(MediaType.APPLICATION_JSON)
					.header("X-User-Id", "test-user-123")
					.header("X-Auth-Acr", "1")
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.error").exists());
		}
	}

	// ============================================================
	// Refine Strategy Endpoint Tests
	// ============================================================

	@Nested
	@DisplayName("POST /v1/labs/ai/refine-strategy")
	class RefineStrategyTests {

		@Test
		@DisplayName("Should refine strategy successfully")
		void shouldRefineStrategySuccessfully() throws Exception {
			// Arrange
			AIStrategyRequest request = new AIStrategyRequest();
			request.setPrompt("Make the stop loss tighter at 2%");
			request.setRequestType(AIStrategyRequest.RequestType.REFINE);

			AIStrategyRequest.StrategyContext context = new AIStrategyRequest.StrategyContext();
			context.setCurrentCode("# existing strategy code");
			context.setCurrentVisualConfig(Map.of("rules", List.of()));
			request.setContext(context);

			AIStrategyResponse mockResponse = createSuccessResponse();
			when(aiStrategyService.refineStrategy(any(AIStrategyRequest.class))).thenReturn(mockResponse);

			// Act & Assert
			mockMvc.perform(post(BASE_URL + "/refine-strategy")
					.contentType(MediaType.APPLICATION_JSON)
					.header("X-User-Id", "test-user-123")
					.header("X-Auth-Acr", "1")
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));

			verify(aiStrategyService).refineStrategy(any(AIStrategyRequest.class));
		}
	}

	// ============================================================
	// Parse Code Endpoint Tests (No Auth Required)
	// ============================================================

	@Nested
	@DisplayName("POST /v1/labs/ai/parse-code")
	class ParseCodeTests {

		@Test
		@DisplayName("Should parse Python code to visual config")
		void shouldParseCodeSuccessfully() throws Exception {
			// Arrange
			Map<String, String> request = new HashMap<>();
			request.put("code", "SYMBOL = 'AAPL'\nSTOP_LOSS = 0.05\nTAKE_PROFIT = 0.10");

			AIStrategyResponse mockResponse = new AIStrategyResponse();
			mockResponse.setSuccess(true);
			mockResponse.setVisualConfig(Map.of("symbol", "AAPL"));

			when(aiStrategyService.parseCodeToVisual(anyString(), any())).thenReturn(mockResponse);

			// Act & Assert
			mockMvc.perform(post(BASE_URL + "/parse-code")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.visualConfig").exists());
		}

		@Test
		@DisplayName("Should return 400 when code is missing")
		void shouldReturn400WhenCodeMissing() throws Exception {
			// Arrange
			Map<String, String> request = new HashMap<>();
			// No code provided

			// Act & Assert
			mockMvc.perform(post(BASE_URL + "/parse-code")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.error").value("Code is required"));
		}

		@Test
		@DisplayName("Should return 400 when code is empty")
		void shouldReturn400WhenCodeEmpty() throws Exception {
			// Arrange
			Map<String, String> request = new HashMap<>();
			request.put("code", "");

			// Act & Assert
			mockMvc.perform(post(BASE_URL + "/parse-code")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.error").value("Code is required"));
		}
	}

	// ============================================================
	// Explain Element Endpoint Tests (No Auth Required)
	// ============================================================

	@Nested
	@DisplayName("POST /v1/labs/ai/explain")
	class ExplainElementTests {

		@Test
		@DisplayName("Should explain strategy element successfully")
		void shouldExplainElementSuccessfully() throws Exception {
			// Arrange
			AIStrategyRequest request = new AIStrategyRequest();
			request.setPrompt("Explain this strategy");
			request.setElementToExplain("RSI < 30 AND MACD_CROSS_UP");

			AIStrategyResponse mockResponse = new AIStrategyResponse();
			mockResponse.setSuccess(true);
			mockResponse.setExplanation("This condition triggers when RSI is oversold and MACD shows bullish momentum.");

			when(aiStrategyService.explainElement(any(AIStrategyRequest.class))).thenReturn(mockResponse);

			// Act & Assert
			mockMvc.perform(post(BASE_URL + "/explain")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.explanation").exists());
		}

		@Test
		@DisplayName("Should return 400 when element to explain is missing")
		void shouldReturn400WhenElementMissing() throws Exception {
			// Arrange
			AIStrategyRequest request = new AIStrategyRequest();
			request.setPrompt("Explain this");
			// No elementToExplain

			// Act & Assert
			mockMvc.perform(post(BASE_URL + "/explain")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.error").value("Element to explain is required"));
		}
	}

	// ============================================================
	// Optimize Endpoint Tests (No Auth Required)
	// ============================================================

	@Nested
	@DisplayName("POST /v1/labs/ai/optimize")
	class OptimizeTests {

		@Test
		@DisplayName("Should return optimization suggestions")
		void shouldReturnOptimizationSuggestions() throws Exception {
			// Arrange
			AIStrategyRequest request = new AIStrategyRequest();
			request.setPrompt("Optimize this strategy");
			request.setBacktestResults(createBacktestResults());

			AIStrategyResponse mockResponse = new AIStrategyResponse();
			mockResponse.setSuccess(true);
			mockResponse.setSuggestions(List.of("Reduce stop loss to 3%", "Add RSI filter"));

			when(aiStrategyService.optimizeFromBacktest(any(AIStrategyRequest.class))).thenReturn(mockResponse);

			// Act & Assert
			mockMvc.perform(post(BASE_URL + "/optimize")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.suggestions").isArray());
		}

		@Test
		@DisplayName("Should return 400 when backtest results are missing")
		void shouldReturn400WhenBacktestResultsMissing() throws Exception {
			// Arrange
			AIStrategyRequest request = new AIStrategyRequest();
			request.setPrompt("Optimize this strategy");
			// No backtest results

			// Act & Assert
			mockMvc.perform(post(BASE_URL + "/optimize")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.error").value("Backtest results are required"));
		}
	}

	// ============================================================
	// Optimize Strategy Endpoint Tests (Auth Required)
	// ============================================================

	@Nested
	@DisplayName("POST /v1/labs/ai/optimize-strategy")
	class OptimizeStrategyTests {

		@Test
		@DisplayName("Should optimize strategy with sufficient trades")
		void shouldOptimizeStrategySuccessfully() throws Exception {
			// Arrange
			AIStrategyRequest request = createOptimizationRequest(50); // 50 trades

			AIStrategyResponse mockResponse = createSuccessResponse();
			mockResponse.setOptimizationSummary(new AIStrategyResponse.OptimizationSummary());

			when(aiStrategyService.optimizeStrategy(any(AIStrategyRequest.class))).thenReturn(mockResponse);

			// Act & Assert
			mockMvc.perform(post(BASE_URL + "/optimize-strategy")
					.contentType(MediaType.APPLICATION_JSON)
					.header("X-User-Id", "test-user-123")
					.header("X-Auth-Acr", "1")
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));
		}

		@Test
		@DisplayName("Should return 400 when insufficient trades for optimization")
		void shouldReturn400WhenInsufficientTrades() throws Exception {
			// Arrange
			AIStrategyRequest request = createOptimizationRequest(5); // Only 5 trades

			// Act & Assert
			mockMvc.perform(post(BASE_URL + "/optimize-strategy")
					.contentType(MediaType.APPLICATION_JSON)
					.header("X-User-Id", "test-user-123")
					.header("X-Auth-Acr", "1")
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.error").value(containsString("at least 10 trades")));
		}

		@Test
		@DisplayName("Should return 400 when backtest metrics are invalid")
		void shouldReturn400WhenMetricsInvalid() throws Exception {
			// Arrange
			AIStrategyRequest request = createOptimizationRequest(50);
			request.getBacktestResults().setSharpeRatio(Double.NaN);

			// Act & Assert
			mockMvc.perform(post(BASE_URL + "/optimize-strategy")
					.contentType(MediaType.APPLICATION_JSON)
					.header("X-User-Id", "test-user-123")
					.header("X-Auth-Acr", "1")
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.error").value(containsString("Invalid backtest metrics")));
		}

		@Test
		@DisplayName("Should return 400 for ENHANCE_EXISTING mode without context")
		void shouldReturn400ForEnhanceModeWithoutContext() throws Exception {
			// Arrange
			AIStrategyRequest request = createOptimizationRequest(50);
			request.setOptimizationMode(AIStrategyRequest.OptimizationMode.ENHANCE_EXISTING);
			request.setContext(null); // No context

			// Act & Assert
			mockMvc.perform(post(BASE_URL + "/optimize-strategy")
					.contentType(MediaType.APPLICATION_JSON)
					.header("X-User-Id", "test-user-123")
					.header("X-Auth-Acr", "1")
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.error").value(containsString("ENHANCE_EXISTING mode requires")));
		}
	}

	// ============================================================
	// Preview Indicators Endpoint Tests (No Auth Required)
	// ============================================================

	@Nested
	@DisplayName("POST /v1/labs/ai/preview-indicators")
	class PreviewIndicatorsTests {

		@Test
		@DisplayName("Should detect indicators from prompt")
		void shouldDetectIndicatorsSuccessfully() throws Exception {
			// Arrange
			Map<String, String> request = Map.of("prompt", "Buy when RSI below 30 and MACD crosses up");

			AIStrategyResponse mockResponse = new AIStrategyResponse();
			mockResponse.setSuccess(true);
			mockResponse.setDetectedIndicators(List.of("RSI", "MACD"));

			when(aiStrategyService.previewIndicators(anyString())).thenReturn(mockResponse);

			// Act & Assert
			mockMvc.perform(post(BASE_URL + "/preview-indicators")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.detectedIndicators").isArray())
				.andExpect(jsonPath("$.detectedIndicators", hasSize(2)));
		}

		@Test
		@DisplayName("Should return 400 when prompt is missing")
		void shouldReturn400WhenPromptMissing() throws Exception {
			// Arrange
			Map<String, String> request = new HashMap<>();

			// Act & Assert
			mockMvc.perform(post(BASE_URL + "/preview-indicators")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.error").value("Prompt is required"));
		}

		@Test
		@DisplayName("Should return empty list on error for graceful degradation")
		void shouldReturnEmptyListOnError() throws Exception {
			// Arrange
			Map<String, String> request = Map.of("prompt", "test prompt");

			when(aiStrategyService.previewIndicators(anyString())).thenThrow(new RuntimeException("Service error"));

			// Act & Assert
			mockMvc.perform(post(BASE_URL + "/preview-indicators")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.detectedIndicators").isArray())
				.andExpect(jsonPath("$.detectedIndicators", hasSize(0)));
		}
	}

	// ============================================================
	// Parse Backtest Query Endpoint Tests (No Auth Required)
	// ============================================================

	@Nested
	@DisplayName("POST /v1/labs/ai/parse-backtest-query")
	class ParseBacktestQueryTests {

		@Test
		@DisplayName("Should parse natural language backtest query")
		void shouldParseBacktestQuerySuccessfully() throws Exception {
			// Arrange
			Map<String, String> request = Map.of("query", "How would this do in the 2022 crash?");

			Map<String, Object> mockResponse = Map.of(
				"startDate", "2022-01-01",
				"endDate", "2022-12-31",
				"description", "2022 market downturn"
			);

			when(aiStrategyService.parseBacktestQuery(anyString())).thenReturn(mockResponse);

			// Act & Assert
			mockMvc.perform(post(BASE_URL + "/parse-backtest-query")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.startDate").value("2022-01-01"))
				.andExpect(jsonPath("$.endDate").value("2022-12-31"));
		}

		@Test
		@DisplayName("Should return 400 when query is missing")
		void shouldReturn400WhenQueryMissing() throws Exception {
			// Arrange
			Map<String, String> request = new HashMap<>();

			// Act & Assert
			mockMvc.perform(post(BASE_URL + "/parse-backtest-query")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Query is required"));
		}
	}

	// ============================================================
	// Model Selection Tests
	// ============================================================

	@Nested
	@DisplayName("Model Selection")
	class ModelSelectionTests {

		@Test
		@DisplayName("Should use specified model for generation")
		void shouldUseSpecifiedModel() throws Exception {
			// Arrange
			AIStrategyRequest request = new AIStrategyRequest();
			request.setPrompt("Buy AAPL when RSI below 30");
			request.setModel("claude-3-5-sonnet");

			AIStrategyResponse mockResponse = createSuccessResponse();
			when(aiStrategyService.generateStrategy(any(AIStrategyRequest.class))).thenReturn(mockResponse);

			// Act & Assert
			mockMvc.perform(post(BASE_URL + "/generate-strategy")
					.contentType(MediaType.APPLICATION_JSON)
					.header("X-User-Id", "test-user-123")
					.header("X-Auth-Acr", "1")
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk());

			// Verify model was passed to service
			verify(aiStrategyService).generateStrategy(argThat(req ->
				"claude-3-5-sonnet".equals(req.getModel())
			));
		}
	}

	// ============================================================
	// Helper Methods
	// ============================================================

	private AIStrategyResponse createSuccessResponse() {
		AIStrategyResponse response = new AIStrategyResponse();
		response.setSuccess(true);
		response.setPythonCode("# Generated strategy\nSYMBOL = 'AAPL'\nSTOP_LOSS = 0.05");
		response.setVisualConfig(Map.of(
			"symbol", "AAPL",
			"rules", List.of(Map.of("condition", "RSI < 30", "action", "BUY"))
		));
		response.setDetectedIndicators(List.of("RSI", "MACD"));
		response.setRiskLevel(AIStrategyResponse.RiskLevel.MEDIUM);
		response.setExplanation("This strategy buys AAPL when RSI indicates oversold conditions.");
		return response;
	}

	private AIStrategyRequest.BacktestResults createBacktestResults() {
		AIStrategyRequest.BacktestResults results = new AIStrategyRequest.BacktestResults();
		results.setTotalReturn(0.15);
		results.setTotalPnL(1500.0);
		results.setWinRate(0.55);
		results.setTotalTrades(50);
		results.setProfitableTrades(28);
		results.setAvgWin(100.0);
		results.setAvgLoss(-50.0);
		results.setProfitFactor(1.5);
		results.setMaxDrawdown(0.10);
		results.setSharpeRatio(1.2);
		return results;
	}

	private AIStrategyRequest createOptimizationRequest(int totalTrades) {
		AIStrategyRequest request = new AIStrategyRequest();
		request.setPrompt("Optimize this strategy");

		AIStrategyRequest.BacktestResults results = createBacktestResults();
		results.setTotalTrades(totalTrades);
		request.setBacktestResults(results);

		AIStrategyRequest.StrategyContext context = new AIStrategyRequest.StrategyContext();
		context.setCurrentCode("# existing code");
		context.setCurrentVisualConfig(Map.of("rules", List.of()));
		request.setContext(context);

		request.setOptimizationMode(AIStrategyRequest.OptimizationMode.ENHANCE_EXISTING);

		return request;
	}

}
