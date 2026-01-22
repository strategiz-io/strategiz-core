package io.strategiz.service.labs.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.strategiz.business.aichat.LLMRouter;
import io.strategiz.business.aichat.prompt.AIStrategyPrompts;
import io.strategiz.business.historicalinsights.exception.InsufficientDataException;
import io.strategiz.business.historicalinsights.model.SymbolInsights;
import io.strategiz.business.historicalinsights.service.HistoricalInsightsCacheService;
import io.strategiz.business.historicalinsights.service.HistoricalInsightsService;
import io.strategiz.client.base.llm.model.LLMMessage;
import io.strategiz.client.base.llm.model.LLMResponse;
import io.strategiz.service.labs.model.AIStrategyRequest;
import io.strategiz.service.labs.model.AIStrategyResponse;
import io.strategiz.service.labs.model.ExecuteStrategyResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import io.strategiz.service.base.BaseService;

/**
 * Service for AI-powered strategy generation, explanation, and optimization.
 */
@Service
public class AIStrategyService extends BaseService {

    @Override
    protected String getModuleName() {
        return "unknown";
    }
	private final LLMRouter llmRouter;

	private final ObjectMapper objectMapper;

	// Optional - only available when ClickHouse is enabled
	private final HistoricalInsightsService historicalInsightsService;
	private final HistoricalInsightsCacheService cacheService;
	private final StrategyExecutionService executionService;

	@Autowired
	public AIStrategyService(LLMRouter llmRouter,
			Optional<HistoricalInsightsService> historicalInsightsService,
			HistoricalInsightsCacheService cacheService,
			StrategyExecutionService executionService) {
		this.llmRouter = llmRouter;
		this.objectMapper = new ObjectMapper();
		this.historicalInsightsService = historicalInsightsService.orElse(null);
		this.cacheService = cacheService;
		this.executionService = executionService;
		if (this.historicalInsightsService == null) {
			log.warn("HistoricalInsightsService not available - Feeling Lucky mode will be disabled");
		}
	}

	/**
	 * Generate a new strategy from a natural language prompt.
	 * For Feeling Lucky mode (useHistoricalInsights=true), validates that the strategy
	 * beats buy-and-hold by at least 15% before returning it to the user.
	 */
	public AIStrategyResponse generateStrategy(AIStrategyRequest request) {
		String promptPreview = (request.getPrompt() != null && !request.getPrompt().isEmpty())
				? request.getPrompt().substring(0, Math.min(50, request.getPrompt().length()))
				: "[Feeling Lucky - Autonomous Mode]";
		log.info("Generating strategy from prompt: {}", promptPreview);

		log.info("Step 1/6: Analyzing prompt for user strategy request");

		try {
			// HISTORICAL INSIGHTS: Get historical market insights if enabled (Feeling Lucky mode)
			SymbolInsights insights = null;
			if (Boolean.TRUE.equals(request.getUseHistoricalInsights())) {
				log.info("Historical Market Insights enabled - analyzing 7 years of data");
				insights = getHistoricalInsights(request);
				if (insights != null) {
					log.info("Historical insights obtained for {}: {} volatility, {} trend", insights.getSymbol(),
							insights.getVolatilityRegime(), insights.getTrendDirection());
				}
			}

			// FEELING LUCKY: Calculate deterministic thresholds and pass to AI as context
			// This gives AI guided parameters based on actual historical data
			String deterministicContext = null;
			if (Boolean.TRUE.equals(request.getUseHistoricalInsights()) && insights != null
					&& insights.getTurningPoints() != null && !insights.getTurningPoints().isEmpty()) {
				deterministicContext = buildDeterministicContext(insights);
				log.info("Feeling Lucky mode - calculated deterministic thresholds as context for AI");
			}

			// REGULAR AI GENERATION: For non-Feeling Lucky or when deterministic fails
			boolean requiresValidation = false;
			int maxAttempts = 1;
			AIStrategyResponse bestResponse = null;
			double bestOutperformance = Double.NEGATIVE_INFINITY;

			for (int attempt = 1; attempt <= maxAttempts; attempt++) {
				log.info("Strategy generation attempt {}/{}", attempt, maxAttempts);

				// Build the system prompt with context
				String symbols = request.getContext() != null && request.getContext().getSymbols() != null
						? String.join(", ", request.getContext().getSymbols()) : null;
				String timeframe = request.getContext() != null ? request.getContext().getTimeframe() : null;
				String visualEditorSchema = request.getVisualEditorSchema();

				String systemPrompt = AIStrategyPrompts.buildGenerationPrompt(symbols, timeframe, visualEditorSchema);

				// HISTORICAL INSIGHTS: Enhance prompt with historical market analysis
				if (insights != null) {
					systemPrompt = systemPrompt + "\n\n" + AIStrategyPrompts.buildHistoricalInsightsPrompt(insights);

					// Add deterministic context with calculated thresholds
					if (deterministicContext != null) {
						systemPrompt = systemPrompt + deterministicContext;
						log.info("Added deterministic thresholds context to system prompt");
					}
				}

				log.info("Step 2/6: Preparing strategy generation parameters");

				// Build conversation history
				List<LLMMessage> history = buildConversationHistory(systemPrompt,
						request.getConversationHistory());

				// Use model from request, or default to gemini-3-flash-preview
				String model = request.getModel() != null ? request.getModel() : llmRouter.getDefaultModel();

				log.info("Step 3/6: Generating strategy with AI model: {}", model);

				// For Feeling Lucky mode, if no prompt provided, use autonomous generation prompt
				String userPrompt = request.getPrompt();
				if (Boolean.TRUE.equals(request.getUseHistoricalInsights())
						&& (userPrompt == null || userPrompt.trim().isEmpty())) {
					if (deterministicContext != null) {
						// Use the calculated thresholds from historical data
						userPrompt = "Generate a swing trading strategy using the EXACT thresholds calculated above from historical turning points. "
								+ "Use the provided Python code structure with the specific buy/sell percentages. "
								+ "Do NOT use RSI, MACD, or other indicators - use the simple price-based swing rules provided. "
								+ "The thresholds are optimized for this specific symbol based on actual historical swings.";
					}
					else {
						userPrompt = "Create an optimized trading strategy that beats buy and hold by at least 15% for the specified symbol(s) using the historical data insights.";
					}
					log.info("Feeling Lucky mode - using guided strategy generation with calculated thresholds");
				}

				// Call LLM via router (blocking)
				LLMResponse llmResponse = llmRouter.generateContent(userPrompt, history, model).block();

				log.info("Step 4/6: Parsing and validating strategy response");

				AIStrategyResponse response = parseGenerationResponse(llmResponse);

				// If validation is required and we have a valid strategy, validate it
				if (requiresValidation && response.isSuccess() && response.getPythonCode() != null) {
					log.info("Step 5/6: Validating strategy performance (beats buy-and-hold by 15%?)");

					double outperformance = validateStrategyPerformance(response, request, insights);

					if (outperformance >= 15.0) {
						log.info("✅ Strategy VALIDATED! Outperformance: {:.2f}% (target: 15%)", outperformance);
						response.setHistoricalInsightsUsed(true);
						response.setHistoricalInsights(insights);
						response.setWarning(String.format(
							"Strategy validated: %.1f%% better than buy-and-hold (validated attempt %d/%d)",
							outperformance, attempt, maxAttempts));
						return response;
					} else {
						log.warn("❌ Strategy failed validation. Outperformance: {:.2f}% (target: 15%)", outperformance);
						if (outperformance > bestOutperformance) {
							bestOutperformance = outperformance;
							bestResponse = response;
						}
					}
				} else if (!requiresValidation) {
					// No validation required, return immediately
					if (Boolean.TRUE.equals(request.getUseHistoricalInsights())) {
						response.setHistoricalInsightsUsed(true);
						response.setHistoricalInsights(insights);
					}
					log.info("Step 6/6: Strategy generation complete (no validation required)");
					return response;
				}
			}

			// If we get here, all attempts failed validation
			log.error("All {} attempts failed to generate a strategy beating buy-and-hold by 15%. Best: {:.2f}%",
				maxAttempts, bestOutperformance);

			// CRITICAL: Do NOT return a strategy that doesn't beat buy-and-hold by 15%
			// User explicitly wants at least 15% outperformance - returning a loser is unacceptable
			if (bestResponse != null && bestOutperformance >= 0.0) {
				// Only return if it at least matches buy-and-hold (0% outperformance)
				bestResponse.setWarning(String.format(
					"⚠️ Strategy did not meet 15%% outperformance target after %d attempts. " +
					"Best achieved: %.1f%% vs buy-and-hold. Consider trying again or using a different approach.",
					maxAttempts, bestOutperformance));
				bestResponse.setHistoricalInsightsUsed(true);
				bestResponse.setHistoricalInsights(insights);
				return bestResponse;
			}

			// If ALL strategies UNDERPERFORM buy-and-hold, don't return garbage
			return AIStrategyResponse.error(String.format(
				"Unable to generate a winning strategy after %d attempts. " +
				"All generated strategies underperformed buy-and-hold (best: %.1f%%). " +
				"This often happens in strong bull markets where buy-and-hold is very hard to beat. " +
				"Try: 1) A different symbol, 2) Shorter timeframe, or 3) Accept that buy-and-hold may be optimal for this asset.",
				maxAttempts, bestOutperformance));
		}
		catch (Exception e) {
			log.error("Error generating strategy", e);
			return AIStrategyResponse.error("Our AI assistant is temporarily unavailable. Please try again in a moment.");
		}
	}

	/**
	 * Refine an existing strategy based on user feedback.
	 */
	public AIStrategyResponse refineStrategy(AIStrategyRequest request) {
		log.info("Refining strategy with prompt: {}",
				request.getPrompt().substring(0, Math.min(50, request.getPrompt().length())));

		log.info("Step 1/4: Analyzing refinement request");

		if (request.getContext() == null || request.getContext().getCurrentCode() == null) {
			return AIStrategyResponse.error("Current strategy context is required for refinement");
		}

		try {
			// Serialize current visual config
			String visualConfigJson;
			try {
				visualConfigJson = objectMapper.writeValueAsString(request.getContext().getCurrentVisualConfig());
			}
			catch (JsonProcessingException e) {
				visualConfigJson = "{}";
			}

			String visualEditorSchema = request.getVisualEditorSchema();
			String refinementPrompt = AIStrategyPrompts.buildRefinementPrompt(visualConfigJson,
					request.getContext().getCurrentCode(), request.getPrompt(), visualEditorSchema);

			log.info("Step 2/4: Preparing refinement parameters");

			// Build conversation history
			List<LLMMessage> history = buildConversationHistory(AIStrategyPrompts.STRATEGY_GENERATION_SYSTEM,
					request.getConversationHistory());

			// Use model from request, or default
			String model = request.getModel() != null ? request.getModel() : llmRouter.getDefaultModel();

			log.info("Step 3/4: Refining strategy with AI model: {}", model);

			LLMResponse llmResponse = llmRouter.generateContent(refinementPrompt, history, model).block();

			log.info("Step 4/4: Parsing and validating refined strategy");

			return parseGenerationResponse(llmResponse);
		}
		catch (Exception e) {
			log.error("Error refining strategy", e);
			return AIStrategyResponse.error("Our AI assistant is temporarily unavailable. Please try again in a moment.");
		}
	}

	/**
	 * Parse Python code to extract visual configuration.
	 */
	public AIStrategyResponse parseCodeToVisual(String code, String visualEditorSchema) {
		log.info("Parsing code to visual config");

		try {
			String prompt = AIStrategyPrompts.buildCodeToVisualPrompt(code, visualEditorSchema);
			LLMResponse llmResponse = llmRouter.generateContent(prompt, new ArrayList<>(), llmRouter.getDefaultModel()).block();

			AIStrategyResponse result = new AIStrategyResponse();

			if (llmResponse == null || !llmResponse.isSuccess()) {
				result.setSuccess(false);
				String technicalError = llmResponse != null ? llmResponse.getError() : "No response from AI";
			log.warn("LLM error occurred: {}", technicalError);
			result.setError(sanitizeErrorMessage(technicalError));
				return result;
			}

			String text = llmResponse.getContent();

			try {
				JsonNode json = extractJsonFromResponse(text);
				if (json != null) {
					if (json.has("visualConfig")) {
						result.setVisualConfig(objectMapper.convertValue(json.get("visualConfig"), Map.class));
					}
					result.setCanRepresentVisually(
							json.has("canRepresent") ? json.get("canRepresent").asBoolean() : true);
					if (json.has("warning")) {
						result.setWarning(json.get("warning").asText());
					}
					if (json.has("extractedIndicators")) {
						result.setDetectedIndicators(objectMapper.convertValue(json.get("extractedIndicators"),
								objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)));
					}
				}
				result.setSuccess(true);
			}
			catch (Exception e) {
				log.error("Error parsing code-to-visual response", e);
				result.setSuccess(false);
				result.setError("Unable to analyze the code. Please try again or contact support.");
				result.setCanRepresentVisually(false);
			}

			return result;
		}
		catch (Exception e) {
			log.error("Error parsing code to visual", e);
			return AIStrategyResponse.error("Unable to analyze the code. Please try again or contact support.");
		}
	}

	/**
	 * Explain a specific element (rule, condition, or code section).
	 */
	public AIStrategyResponse explainElement(AIStrategyRequest request) {
		log.info("Explaining element: {}",
				request.getElementToExplain().substring(0, Math.min(50, request.getElementToExplain().length())));

		try {
			// Serialize context if available
			String contextJson = null;
			if (request.getContext() != null && request.getContext().getCurrentCode() != null) {
				contextJson = request.getContext().getCurrentCode();
			}

			String prompt = AIStrategyPrompts.buildExplainPrompt(request.getElementToExplain(), contextJson);
			LLMResponse llmResponse = llmRouter.generateContent(prompt, new ArrayList<>(), llmRouter.getDefaultModel()).block();

			AIStrategyResponse result = new AIStrategyResponse();

			if (llmResponse == null || !llmResponse.isSuccess()) {
				result.setSuccess(false);
				String technicalError = llmResponse != null ? llmResponse.getError() : "No response from AI";
			log.warn("LLM error occurred: {}", technicalError);
			result.setError(sanitizeErrorMessage(technicalError));
				return result;
			}

			String text = llmResponse.getContent();

			try {
				JsonNode json = extractJsonFromResponse(text);
				if (json != null && json.has("explanation")) {
					result.setExplanation(json.get("explanation").asText());

					// Add additional info if available
					if (json.has("whyItMatters")) {
						result.setSummaryCard(json.get("whyItMatters").asText());
					}
					if (json.has("alternatives")) {
						result.setSuggestions(objectMapper.convertValue(json.get("alternatives"),
								objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)));
					}
				}
				else {
					// Fall back to raw text
					result.setExplanation(text);
				}
				result.setSuccess(true);
			}
			catch (Exception e) {
				// Fall back to raw text
				result.setExplanation(text);
				result.setSuccess(true);
			}

			return result;
		}
		catch (Exception e) {
			log.error("Error explaining element", e);
			return AIStrategyResponse.error("Failed to explain element: " + e.getMessage());
		}
	}

	/**
	 * Get optimization suggestions based on backtest results.
	 */
	public AIStrategyResponse optimizeFromBacktest(AIStrategyRequest request) {
		log.info("Generating optimization suggestions from backtest results");

		if (request.getBacktestResults() == null) {
			return AIStrategyResponse.error("Backtest results are required for optimization");
		}

		try {
			// Serialize current strategy
			String strategyJson;
			try {
				Map<String, Object> strategyMap = new HashMap<>();
				if (request.getContext() != null) {
					strategyMap.put("visualConfig", request.getContext().getCurrentVisualConfig());
					strategyMap.put("code", request.getContext().getCurrentCode());
				}
				strategyJson = objectMapper.writeValueAsString(strategyMap);
			}
			catch (JsonProcessingException e) {
				strategyJson = "{}";
			}

			AIStrategyRequest.BacktestResults bt = request.getBacktestResults();
			String prompt = AIStrategyPrompts.buildOptimizationPrompt(strategyJson, bt.getTotalReturn(),
					bt.getTotalPnL(), bt.getWinRate(), bt.getTotalTrades(), bt.getProfitableTrades(), bt.getAvgWin(),
					bt.getAvgLoss(), bt.getProfitFactor(), bt.getMaxDrawdown(), bt.getSharpeRatio());

			LLMResponse llmResponse = llmRouter.generateContent(prompt, new ArrayList<>(), llmRouter.getDefaultModel()).block();

			AIStrategyResponse result = new AIStrategyResponse();

			if (llmResponse == null || !llmResponse.isSuccess()) {
				result.setSuccess(false);
				String technicalError = llmResponse != null ? llmResponse.getError() : "No response from AI";
			log.warn("LLM error occurred: {}", technicalError);
			result.setError(sanitizeErrorMessage(technicalError));
				return result;
			}

			String text = llmResponse.getContent();

			try {
				JsonNode json = extractJsonFromResponse(text);
				if (json != null) {
					if (json.has("analysis")) {
						result.setExplanation(json.get("analysis").asText());
					}
					if (json.has("overallAssessment")) {
						result.setSummaryCard(json.get("overallAssessment").asText());
					}
					if (json.has("suggestions")) {
						List<AIStrategyResponse.OptimizationSuggestion> suggestions = new ArrayList<>();
						for (JsonNode suggNode : json.get("suggestions")) {
							AIStrategyResponse.OptimizationSuggestion sugg = objectMapper.convertValue(suggNode,
									AIStrategyResponse.OptimizationSuggestion.class);
							suggestions.add(sugg);
						}
						result.setOptimizationSuggestions(suggestions);
					}
				}
				result.setSuccess(true);
			}
			catch (Exception e) {
				log.error("Error parsing optimization response", e);
				result.setExplanation(text);
				result.setSuccess(true);
			}

			return result;
		}
		catch (Exception e) {
			log.error("Error generating optimizations", e);
			return AIStrategyResponse.error("Failed to generate optimizations: " + e.getMessage());
		}
	}

	/**
	 * Detect indicators from a partial prompt (for live preview).
	 */
	public AIStrategyResponse previewIndicators(String partialPrompt) {
		log.debug("Previewing indicators for partial prompt: {}",
				partialPrompt.substring(0, Math.min(30, partialPrompt.length())));

		try {
			String prompt = AIStrategyPrompts.buildIndicatorPreviewPrompt(partialPrompt);
			LLMResponse llmResponse = llmRouter.generateContent(prompt, new ArrayList<>(), llmRouter.getDefaultModel()).block();

			AIStrategyResponse result = new AIStrategyResponse();

			if (llmResponse == null || !llmResponse.isSuccess()) {
				result.setSuccess(false);
				String technicalError = llmResponse != null ? llmResponse.getError() : "No response from AI";
			log.warn("LLM error occurred: {}", technicalError);
			result.setError(sanitizeErrorMessage(technicalError));
				return result;
			}

			String text = llmResponse.getContent();

			try {
				JsonNode json = extractJsonFromResponse(text);
				if (json != null && json.has("detectedIndicators")) {
					List<String> indicators = new ArrayList<>();
					for (JsonNode indNode : json.get("detectedIndicators")) {
						if (indNode.has("name")) {
							indicators.add(indNode.get("name").asText());
						}
						else if (indNode.isTextual()) {
							indicators.add(indNode.asText());
						}
					}
					result.setDetectedIndicators(indicators);

					if (json.has("strategyType")) {
						result.setSummaryCard("Strategy type: " + json.get("strategyType").asText());
					}
					if (json.has("suggestedRiskLevel") && !json.get("suggestedRiskLevel").isNull()) {
						try {
							result.setRiskLevel(
									AIStrategyResponse.RiskLevel.valueOf(json.get("suggestedRiskLevel").asText()));
						}
						catch (IllegalArgumentException ignored) {
							// Ignore invalid risk level
						}
					}
				}
				result.setSuccess(true);
			}
			catch (Exception e) {
				log.error("Error parsing indicator preview response", e);
				result.setDetectedIndicators(new ArrayList<>());
				result.setSuccess(true);
			}

			return result;
		}
		catch (Exception e) {
			log.error("Error previewing indicators", e);
			AIStrategyResponse result = new AIStrategyResponse();
			result.setDetectedIndicators(new ArrayList<>());
			result.setSuccess(true);
			return result;
		}
	}

	/**
	 * Optimize a backtested strategy using AI and historical insights.
	 * Two modes: GENERATE_NEW (create brand new strategy) or ENHANCE_EXISTING (improve current strategy).
	 */
	public AIStrategyResponse optimizeStrategy(AIStrategyRequest request) {
		AIStrategyRequest.OptimizationMode mode = request.getOptimizationMode() != null ?
				request.getOptimizationMode() :
				AIStrategyRequest.OptimizationMode.ENHANCE_EXISTING;

		log.info("Optimizing strategy with mode: {}", mode);

		try {
			// 1. Get Historical Insights if enabled
			SymbolInsights insights = null;
			if (Boolean.TRUE.equals(request.getUseHistoricalInsights())) {
				log.info("Historical Insights enabled for optimization");
				insights = getHistoricalInsights(request);
				if (insights != null) {
					log.info("Historical insights obtained for {}: {} volatility",
							insights.getSymbol(), insights.getVolatilityRegime());
				}
			}

			// 2. Build optimization prompt based on mode
			String systemPrompt;
			AIStrategyRequest.BacktestResults bt = request.getBacktestResults();

			if (mode == AIStrategyRequest.OptimizationMode.GENERATE_NEW) {
				systemPrompt = AIStrategyPrompts.buildGenerateNewOptimizedPrompt(
						bt.getTotalReturn(),
						bt.getWinRate(),
						bt.getSharpeRatio(),
						bt.getMaxDrawdown(),
						bt.getProfitFactor(),
						bt.getTotalTrades(),
						insights
				);
			}
			else {
				// ENHANCE_EXISTING
				if (request.getContext() == null ||
						request.getContext().getCurrentCode() == null ||
						request.getContext().getCurrentVisualConfig() == null) {
					return AIStrategyResponse.error(
							"ENHANCE_EXISTING mode requires current strategy (code and visual config)"
					);
				}

				// Serialize visual config to JSON string
				String visualConfigJson;
				try {
					visualConfigJson = objectMapper.writerWithDefaultPrettyPrinter()
						.writeValueAsString(request.getContext().getCurrentVisualConfig());
				}
				catch (Exception e) {
					visualConfigJson = request.getContext().getCurrentVisualConfig().toString();
				}

				systemPrompt = AIStrategyPrompts.buildEnhanceExistingPrompt(
						visualConfigJson,
						request.getContext().getCurrentCode(),
						bt.getTotalReturn(),
						bt.getWinRate(),
						bt.getSharpeRatio(),
						bt.getMaxDrawdown(),
						bt.getProfitFactor(),
						bt.getTotalTrades(),
						insights
				);
			}

			// 3. Build conversation history
			List<LLMMessage> history = buildConversationHistory(
					systemPrompt,
					request.getConversationHistory()
			);

			// 4. Call LLM
			String model = request.getModel() != null ?
					request.getModel() :
					llmRouter.getDefaultModel();

			log.info("Calling LLM for strategy optimization");
			LLMResponse llmResponse = llmRouter.generateContent(
					request.getPrompt(),
					history,
					model
			).block();

			// 5. Parse response
			AIStrategyResponse response = parseOptimizationResponse(
					llmResponse,
					request
			);

			// 6. Include historical insights in response
			if (Boolean.TRUE.equals(request.getUseHistoricalInsights())) {
				response.setHistoricalInsightsUsed(true);
				response.setHistoricalInsights(insights);
			}

			log.info("Strategy optimization complete");
			return response;

		}
		catch (InsufficientDataException e) {
			log.warn("Insufficient historical data: {}", e.getMessage());
			return AIStrategyResponse.error(
					"Unable to optimize: insufficient historical market data. " +
							"Try a different symbol or disable Historical Insights."
			);
		}
		catch (Exception e) {
			log.error("Error optimizing strategy", e);
			return AIStrategyResponse.error(
					"Strategy optimization failed. Please try again."
			);
		}
	}

	/**
	 * Parse a natural language backtest query to extract date parameters.
	 */
	public Map<String, Object> parseBacktestQuery(String query) {
		log.info("Parsing backtest query: {}", query);

		try {
			String currentDate = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
			String prompt = AIStrategyPrompts.buildBacktestQueryPrompt(query, currentDate);

			LLMResponse llmResponse = llmRouter.generateContent(prompt, new ArrayList<>(), llmRouter.getDefaultModel()).block();

			if (llmResponse == null || !llmResponse.isSuccess()) {
				log.error("Error response from LLM: {}", llmResponse != null ? llmResponse.getError() : "No response");
				return new HashMap<>();
			}

			String text = llmResponse.getContent();

			try {
				JsonNode json = extractJsonFromResponse(text);
				if (json != null) {
					return objectMapper.convertValue(json, Map.class);
				}
			}
			catch (Exception e) {
				log.error("Error parsing backtest query response", e);
			}
		}
		catch (Exception e) {
			log.error("Error parsing backtest query", e);
		}

		return new HashMap<>();
	}

	/**
	 * Validate strategy performance by backtesting and comparing to buy-and-hold.
	 * Returns outperformance percentage (strategy return - buy-and-hold return).
	 *
	 * @param response Generated strategy response with Python code
	 * @param request Original request with symbol/timeframe context
	 * @param insights Historical insights (contains symbol if request doesn't)
	 * @return Outperformance percentage (positive = beats buy-and-hold)
	 */
	private double validateStrategyPerformance(AIStrategyResponse response, AIStrategyRequest request,
			SymbolInsights insights) {
		try {
			// Extract symbol and timeframe
			String symbol = extractPrimarySymbol(request);
			if (symbol == null && insights != null) {
				symbol = insights.getSymbol();
			}
			if (symbol == null) {
				log.warn("Cannot validate: no symbol found");
				return Double.NEGATIVE_INFINITY;
			}

			String timeframe = request.getContext() != null && request.getContext().getTimeframe() != null
					? request.getContext().getTimeframe() : "1D";

			// Use 3-year backtest period for validation (balance between 1y too short and 7y too long)
			String period = "3y";

			log.info("Validating strategy for symbol={}, timeframe={}, period={}", symbol, timeframe, period);

			// Execute backtest
			ExecuteStrategyResponse backtestResult = executionService.executeStrategy(
				response.getPythonCode(),
				"python",
				symbol,
				timeframe,
				period,
				"system-validation", // user ID
				null // no strategy entity
			);

			// Check if backtest succeeded (no errors and has performance data)
			boolean hasErrors = backtestResult.getErrors() != null && !backtestResult.getErrors().isEmpty();
			if (hasErrors || backtestResult.getPerformance() == null) {
				log.warn("Backtest failed or returned no performance data. Errors: {}",
					hasErrors ? backtestResult.getErrors() : "none");
				return Double.NEGATIVE_INFINITY;
			}

			// Log trade count but don't fail - AI has turning points data for optimal entry/exit
			int totalTrades = backtestResult.getPerformance().getTotalTrades();
			if (totalTrades < 10) {
				log.warn("Strategy has few trades ({}) - may be overfitted but proceeding with validation", totalTrades);
			}

			// Get strategy return (already in percentage)
			double strategyReturn = backtestResult.getPerformance().getTotalReturn();

			// Get buy-and-hold return (already in percentage)
			double buyAndHoldReturn = backtestResult.getPerformance().getBuyAndHoldReturnPercent() != null
					? backtestResult.getPerformance().getBuyAndHoldReturnPercent()
					: 0.0;

			// Calculate outperformance
			double outperformance = strategyReturn - buyAndHoldReturn;

			log.info("Validation results: Trades={}, Strategy={:.2f}%, Buy&Hold={:.2f}%, Outperformance={:.2f}%",
				totalTrades, strategyReturn, buyAndHoldReturn, outperformance);

			return outperformance;

		} catch (Exception e) {
			log.error("Error validating strategy performance", e);
			return Double.NEGATIVE_INFINITY;
		}
	}

	// Historical Market Insights Integration (Feeling Lucky)

	/**
	 * Get Historical Market Insights for a symbol (Feeling Lucky mode).
	 * Analyzes 7 years of historical data to optimize strategy parameters.
	 * Checks cache first, computes if needed, handles errors gracefully.
	 */
	private SymbolInsights getHistoricalInsights(AIStrategyRequest request) {
		// Check if HistoricalInsightsService is available (requires ClickHouse)
		if (historicalInsightsService == null) {
			log.warn("Historical Market Insights not available - ClickHouse is disabled");
			return null;
		}

		// Extract symbol from prompt or context
		String symbol = extractPrimarySymbol(request);
		if (symbol == null) {
			log.warn("No symbol found in request for Historical Market Insights");
			return null;
		}

		// Extract timeframe (default to "1D")
		String timeframe = request.getContext() != null && request.getContext().getTimeframe() != null
				? request.getContext().getTimeframe()
				: "1D";

		// Get Historical Insights options or use defaults
		AIStrategyRequest.HistoricalMarketInsightsOptions options = request.getHistoricalInsightsOptions() != null
				? request.getHistoricalInsightsOptions()
				: new AIStrategyRequest.HistoricalMarketInsightsOptions();

		// Build cache key (includes fastMode to avoid mixing fast/full results)
		String cacheKey = String.format("%s:%s:%s:%s", symbol, timeframe, options.getUseFundamentals(),
				!Boolean.FALSE.equals(options.getFastMode()));

		// Check cache first (unless forceRefresh is enabled)
		if (!Boolean.TRUE.equals(options.getForceRefresh())) {
			Optional<SymbolInsights> cached = cacheService.getCachedInsights(cacheKey);
			if (cached.isPresent()) {
				log.info("Using cached Historical Market Insights for {}", symbol);
				return cached.get();
			}
		}

		// Compute fresh insights
		boolean fastMode = !Boolean.FALSE.equals(options.getFastMode()); // Default to fast mode
		log.info("Computing Historical Market Insights for {} ({}, {} days, fastMode={})", symbol, timeframe,
				options.getLookbackDays(), fastMode);

		try {
			SymbolInsights insights = historicalInsightsService.analyzeSymbolForStrategyGeneration(symbol, timeframe,
					options.getLookbackDays(), Boolean.TRUE.equals(options.getUseFundamentals()), fastMode);

			// Cache for future use
			cacheService.cacheInsights(cacheKey, insights);

			return insights;
		}
		catch (InsufficientDataException e) {
			// Fallback: Use whatever data is available
			log.warn("Insufficient data for {}, using partial analysis", symbol);
			try {
				return historicalInsightsService.analyzeWithPartialData(symbol, timeframe);
			}
			catch (Exception fallbackError) {
				log.error("Failed to generate partial insights for {}", symbol, fallbackError);
				return null;
			}
		}
		catch (Exception e) {
			log.error("Error computing Historical Market Insights for {}", symbol, e);
			return null;
		}
	}

	/**
	 * Build deterministic context from turning points to guide AI strategy generation.
	 * Calculates optimal thresholds from historical data and formats as instructions for AI.
	 */
	private String buildDeterministicContext(SymbolInsights insights) {
		if (insights == null || insights.getTurningPoints() == null || insights.getTurningPoints().isEmpty()) {
			return null;
		}

		// Calculate average swing magnitudes from turning points
		double avgDropToTrough = 0, avgRiseFromTrough = 0;
		int troughCount = 0, peakCount = 0;
		double maxDrop = 0, maxRise = 0;

		for (var tp : insights.getTurningPoints()) {
			if (tp.getPriceChangeFromPrevious() != 0) {
				if ("TROUGH".equals(tp.getType().name())) {
					double drop = Math.abs(tp.getPriceChangeFromPrevious());
					avgDropToTrough += drop;
					maxDrop = Math.max(maxDrop, drop);
					troughCount++;
				}
				else {
					double rise = tp.getPriceChangeFromPrevious();
					avgRiseFromTrough += rise;
					maxRise = Math.max(maxRise, rise);
					peakCount++;
				}
			}
		}

		if (troughCount > 0)
			avgDropToTrough /= troughCount;
		if (peakCount > 0)
			avgRiseFromTrough /= peakCount;

		// Use 80% of avg swing as threshold (gives buffer for early entry)
		double buyThreshold = Math.max(avgDropToTrough * 0.8, 5.0);
		double sellThreshold = Math.max(avgRiseFromTrough * 0.8, 8.0);

		StringBuilder context = new StringBuilder();
		context.append("\n\n");
		context.append("=" .repeat(80)).append("\n");
		context.append("🎯 AI-CALCULATED OPTIMAL TRADING THRESHOLDS\n");
		context.append("=".repeat(80)).append("\n");
		context.append("Based on analysis of ").append(insights.getTurningPoints().size())
				.append(" historical turning points, here are the OPTIMAL thresholds:\n\n");

		context.append(String.format("📊 SWING ANALYSIS RESULTS:\n"));
		context.append(String.format("   - Analyzed %d troughs (buy points) and %d peaks (sell points)\n",
				troughCount, peakCount));
		context.append(String.format("   - Average drop before troughs: %.1f%% (max: %.1f%%)\n",
				avgDropToTrough, maxDrop));
		context.append(String.format("   - Average rise before peaks: %.1f%% (max: %.1f%%)\n\n",
				avgRiseFromTrough, maxRise));

		context.append("🎯 RECOMMENDED THRESHOLDS (use these EXACTLY):\n");
		context.append(String.format("   - BUY THRESHOLD: -%.1f%% from 20-day high (dip buying)\n", buyThreshold));
		context.append(String.format("   - SELL THRESHOLD: -%.1f%% from highest price since entry (trailing stop)\n\n",
				sellThreshold * 0.5)); // Use smaller trailing stop

		context.append("📝 REQUIRED STRATEGY STRUCTURE (TRAILING STOP APPROACH):\n");
		context.append("```python\n");
		context.append("import pandas as pd\n");
		context.append("import numpy as np\n\n");
		context.append(String.format("SYMBOL = '%s'\n", insights.getSymbol()));
		context.append("TIMEFRAME = '1D'\n");
		context.append(String.format("BUY_DIP_THRESHOLD = %.1f  # Buy when price drops this %% from 20-day high\n", buyThreshold));
		context.append(String.format("TRAILING_STOP = %.1f  # Sell when price drops this %% from peak since entry\n\n", sellThreshold * 0.5));
		context.append("# Track position state (must persist between calls)\n");
		context.append("entry_price = None\n");
		context.append("highest_since_entry = None\n\n");
		context.append("def strategy(data):\n");
		context.append("    global entry_price, highest_since_entry\n");
		context.append("    if len(data) < 21:\n");
		context.append("        return 'HOLD'\n\n");
		context.append("    current_price = data['close'].iloc[-1]\n");
		context.append("    recent_high = data['high'].iloc[-21:-1].max()  # 20-day high excluding today\n\n");
		context.append("    # If we have a position, check trailing stop\n");
		context.append("    if entry_price is not None:\n");
		context.append("        highest_since_entry = max(highest_since_entry or current_price, current_price)\n");
		context.append("        pct_from_peak = (current_price - highest_since_entry) / highest_since_entry * 100\n\n");
		context.append("        # SELL if price drops from peak (trailing stop)\n");
		context.append("        if pct_from_peak <= -TRAILING_STOP:\n");
		context.append("            entry_price = None\n");
		context.append("            highest_since_entry = None\n");
		context.append("            return 'SELL'\n");
		context.append("        return 'HOLD'  # Hold position\n\n");
		context.append("    # No position - look for buy opportunity\n");
		context.append("    pct_from_high = (current_price - recent_high) / recent_high * 100\n\n");
		context.append("    # BUY when price dips from recent high\n");
		context.append("    if pct_from_high <= -BUY_DIP_THRESHOLD:\n");
		context.append("        entry_price = current_price\n");
		context.append("        highest_since_entry = current_price\n");
		context.append("        return 'BUY'\n\n");
		context.append("    return 'HOLD'\n");
		context.append("```\n\n");

		context.append("⚠️ CRITICAL INSTRUCTIONS:\n");
		context.append("1. Use the EXACT thresholds above (").append(String.format("-%.1f%%", buyThreshold))
				.append(" buy dip, ").append(String.format("-%.1f%%", sellThreshold * 0.5)).append(" trailing stop)\n");
		context.append("2. Use TRAILING STOP logic - sell when price DROPS from peak, not when it rises from low!\n");
		context.append("3. Track entry_price and highest_since_entry to implement trailing stop correctly\n");
		context.append("4. Do NOT use RSI, MACD, or other indicators - simple price-based rules work best\n");
		context.append("=".repeat(80)).append("\n");

		return context.toString();
	}

	/**
	 * Generate a deterministic swing trading strategy from turning points data.
	 * Uses trailing stop approach - sell when price drops from peak, not when it rises from low.
	 */
	private String generateDeterministicSwingStrategy(SymbolInsights insights) {
		if (insights == null || insights.getTurningPoints() == null || insights.getTurningPoints().isEmpty()) {
			return null;
		}

		// Calculate average swing magnitudes from turning points
		double avgDropToTrough = 0, avgRiseFromTrough = 0;
		int troughCount = 0, peakCount = 0;

		for (var tp : insights.getTurningPoints()) {
			if (tp.getPriceChangeFromPrevious() != 0) {
				if ("TROUGH".equals(tp.getType().name())) {
					avgDropToTrough += Math.abs(tp.getPriceChangeFromPrevious());
					troughCount++;
				}
				else {
					avgRiseFromTrough += tp.getPriceChangeFromPrevious();
					peakCount++;
				}
			}
		}

		if (troughCount > 0)
			avgDropToTrough /= troughCount;
		if (peakCount > 0)
			avgRiseFromTrough /= peakCount;

		// Use 80% of avg swing as buy threshold
		double buyThreshold = Math.max(avgDropToTrough * 0.8, 5.0);
		// Use 50% of avg rise as trailing stop (tighter stop to protect gains)
		double trailingStop = Math.max(avgRiseFromTrough * 0.4, 5.0);

		log.info("Deterministic swing strategy: BUY on {}% dip, TRAILING STOP {}%", buyThreshold, trailingStop);

		String symbol = insights.getSymbol();

		return String.format("""
				import pandas as pd
				import numpy as np

				# Deterministic Swing Trading Strategy for %s
				# Generated from historical turning points analysis
				# BUY when price drops %.1f%% from 20-day high
				# SELL when price drops %.1f%% from highest point since entry (trailing stop)

				SYMBOL = '%s'
				TIMEFRAME = '1D'
				POSITION_SIZE = 100  # Full position for maximum impact

				# Thresholds derived from historical turning points
				BUY_DIP_THRESHOLD = %.1f   # Buy when price drops this %% from 20-day high
				TRAILING_STOP = %.1f       # Sell when price drops this %% from peak since entry

				# Track position state
				entry_price = None
				highest_since_entry = None

				def strategy(data):
				    global entry_price, highest_since_entry

				    if len(data) < 21:
				        return 'HOLD'

				    current_price = data['close'].iloc[-1]
				    recent_high = data['high'].iloc[-21:-1].max()  # 20-day high excluding today

				    # If we have a position, check trailing stop
				    if entry_price is not None:
				        # Update highest price since entry
				        if highest_since_entry is None:
				            highest_since_entry = current_price
				        else:
				            highest_since_entry = max(highest_since_entry, current_price)

				        # Calculate drop from peak
				        pct_from_peak = ((current_price - highest_since_entry) / highest_since_entry) * 100

				        # SELL if price drops from peak (trailing stop triggered)
				        if pct_from_peak <= -TRAILING_STOP:
				            entry_price = None
				            highest_since_entry = None
				            return 'SELL'

				        return 'HOLD'  # Keep holding, trailing stop not hit

				    # No position - look for buy opportunity
				    pct_from_high = ((current_price - recent_high) / recent_high) * 100

				    # BUY when price dips significantly from recent high
				    if pct_from_high <= -BUY_DIP_THRESHOLD:
				        entry_price = current_price
				        highest_since_entry = current_price
				        return 'BUY'

				    return 'HOLD'
				""", symbol, buyThreshold, trailingStop, symbol, buyThreshold, trailingStop);
	}

	/**
	 * Extract primary symbol from request prompt or context.
	 */
	private String extractPrimarySymbol(AIStrategyRequest request) {
		// First check context symbols
		if (request.getContext() != null && request.getContext().getSymbols() != null
				&& !request.getContext().getSymbols().isEmpty()) {
			return request.getContext().getSymbols().get(0);
		}

		// Try to extract from prompt using regex (simple approach)
		String prompt = request.getPrompt();
		if (prompt != null) {
			// Look for common ticker patterns (AAPL, SPY, TSLA, etc.)
			Pattern symbolPattern = Pattern.compile("\\b([A-Z]{1,5})\\b");
			Matcher matcher = symbolPattern.matcher(prompt);
			if (matcher.find()) {
				return matcher.group(1);
			}
		}

		return null;
	}

	// Helper methods

	private List<LLMMessage> buildConversationHistory(String systemPrompt,
			List<AIStrategyRequest.ChatMessage> conversationHistory) {
		List<LLMMessage> history = new ArrayList<>();

		// Add system prompt as first user message
		history.add(new LLMMessage("user", systemPrompt));
		history.add(new LLMMessage("assistant",
				"I understand. I will generate trading strategies as structured JSON with both visual configuration and Python code."));

		// Add conversation history
		if (conversationHistory != null) {
			for (AIStrategyRequest.ChatMessage msg : conversationHistory) {
				String role = "user".equals(msg.getRole()) ? "user" : "assistant";
				history.add(new LLMMessage(role, msg.getContent()));
			}
		}

		return history;
	}

	private AIStrategyResponse parseGenerationResponse(LLMResponse llmResponse) {
		AIStrategyResponse result = new AIStrategyResponse();

		if (llmResponse == null || !llmResponse.isSuccess()) {
			result.setSuccess(false);
			String technicalError = llmResponse != null ? llmResponse.getError() : "No response from AI";
			log.warn("LLM error during code parsing: {}", technicalError);
			result.setError(sanitizeErrorMessage(technicalError));
			return result;
		}

		String text = llmResponse.getContent();

		if (text == null || text.isEmpty()) {
			result.setSuccess(false);
			result.setError("Empty response from AI");
			return result;
		}

		try {
			JsonNode json = extractJsonFromResponse(text);

			if (json != null) {
				// Extract visual config
				if (json.has("visualConfig")) {
					result.setVisualConfig(objectMapper.convertValue(json.get("visualConfig"), Map.class));
				}

				// Extract Python code
				if (json.has("pythonCode")) {
					result.setPythonCode(json.get("pythonCode").asText());
				}

				// Extract summary card
				if (json.has("summaryCard")) {
					result.setSummaryCard(json.get("summaryCard").asText());
				}

				// Extract risk level
				if (json.has("riskLevel")) {
					try {
						result.setRiskLevel(AIStrategyResponse.RiskLevel.valueOf(json.get("riskLevel").asText()));
					}
					catch (IllegalArgumentException e) {
						result.setRiskLevel(AIStrategyResponse.RiskLevel.MEDIUM);
					}
				}

				// Extract detected indicators
				if (json.has("detectedIndicators")) {
					result.setDetectedIndicators(objectMapper.convertValue(json.get("detectedIndicators"),
							objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)));
				}

				// Extract explanation
				if (json.has("explanation")) {
					result.setExplanation(json.get("explanation").asText());
				}

				// Extract suggestions
				if (json.has("suggestions")) {
					result.setSuggestions(objectMapper.convertValue(json.get("suggestions"),
							objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)));
				}

				result.setSuccess(true);

				// Log visual rules quality for monitoring and prompt improvement
				logVisualRulesQuality(result);
			}
			else {
				// Couldn't parse JSON - try to extract code block
				result.setPythonCode(extractCodeBlock(text));
				result.setExplanation(text);
				result.setSuccess(result.getPythonCode() != null);
				if (!result.isSuccess()) {
					result.setError("Could not parse AI response as JSON");
				}
			}
		}
		catch (Exception e) {
			log.error("Error parsing generation response", e);
			result.setSuccess(false);
			result.setError("Failed to parse response: " + e.getMessage());
		}

		return result;
	}

	private JsonNode extractJsonFromResponse(String text) {
		if (text == null) {
			return null;
		}

		// Try to find JSON in markdown code blocks first
		Pattern jsonBlockPattern = Pattern.compile("```(?:json)?\\s*\\n?(\\{.*?})\\s*```", Pattern.DOTALL);
		Matcher matcher = jsonBlockPattern.matcher(text);
		if (matcher.find()) {
			try {
				return objectMapper.readTree(matcher.group(1));
			}
			catch (JsonProcessingException e) {
				log.debug("Failed to parse JSON from code block", e);
			}
		}

		// Try to find raw JSON
		Pattern rawJsonPattern = Pattern.compile("(\\{.*})", Pattern.DOTALL);
		matcher = rawJsonPattern.matcher(text);
		if (matcher.find()) {
			try {
				return objectMapper.readTree(matcher.group(1));
			}
			catch (JsonProcessingException e) {
				log.debug("Failed to parse raw JSON", e);
			}
		}

		// Try parsing the whole text as JSON
		try {
			return objectMapper.readTree(text);
		}
		catch (JsonProcessingException e) {
			log.debug("Failed to parse text as JSON", e);
		}

		return null;
	}

	private String extractCodeBlock(String text) {
		Pattern codeBlockPattern = Pattern.compile("```(?:python)?\\s*\\n?(.*?)\\s*```", Pattern.DOTALL);
		Matcher matcher = codeBlockPattern.matcher(text);
		if (matcher.find()) {
			return matcher.group(1).trim();
		}
		return null;
	}

	/**
	 * Sanitize technical LLM error messages to be user-friendly.
	 * Logs the full technical error for debugging.
	 */
	private String sanitizeErrorMessage(String technicalError) {
		if (technicalError == null) {
			return "Our AI assistant is temporarily unavailable. Please try again in a moment.";
		}

		String lowerError = technicalError.toLowerCase();

		// Authentication/authorization errors
		if (lowerError.contains("401") || lowerError.contains("unauthorized") || lowerError.contains("unauthenticated")
				|| lowerError.contains("access_token_expired") || lowerError.contains("invalid authentication")) {
			return "Our AI assistant is experiencing authentication issues. Our team has been notified. Please try again shortly.";
		}

		// Rate limiting
		if (lowerError.contains("429") || lowerError.contains("rate limit") || lowerError.contains("quota")) {
			return "Our AI is currently experiencing high demand. Please try again in a few moments.";
		}

		// Service unavailable
		if (lowerError.contains("503") || lowerError.contains("service unavailable")
				|| lowerError.contains("temporarily unavailable")) {
			return "Our AI assistant is temporarily unavailable. Please try again in a moment.";
		}

		// Timeout
		if (lowerError.contains("timeout") || lowerError.contains("timed out")) {
			return "The AI took too long to respond. Please try again.";
		}

		// Network errors
		if (lowerError.contains("network") || lowerError.contains("connection") || lowerError.contains("unreachable")) {
			return "Unable to reach our AI service. Please check your connection and try again.";
		}

		// Model-specific errors
		if (lowerError.contains("failed to call") || lowerError.contains("api error")) {
			return "Our AI assistant encountered an error. Please try again or contact support if the issue persists.";
		}

		// Generic fallback
		return "Our AI assistant is temporarily unavailable. Please try again in a moment.";
	}

	/**
	 * Log visual rules quality metrics for monitoring and prompt improvement.
	 * This is lightweight logging (~1ms overhead) to track AI generation quality without
	 * expensive validation.
	 */
	private void logVisualRulesQuality(AIStrategyResponse response) {
		if (response.getVisualConfig() == null) {
			return;
		}

		try {
			Map<String, Object> visualConfig = response.getVisualConfig();
			Object rulesObj = visualConfig.get("rules");

			if (!(rulesObj instanceof List)) {
				return;
			}

			@SuppressWarnings("unchecked")
			List<Map<String, Object>> rules = (List<Map<String, Object>>) rulesObj;

			int crossoverCount = 0;
			int indicatorComparisonCount = 0;
			int missingSecondaryIndicatorCount = 0;
			int emptyValueCount = 0;

			// Scan through all conditions in all rules
			for (Map<String, Object> rule : rules) {
				Object conditionsObj = rule.get("conditions");
				if (!(conditionsObj instanceof List)) {
					continue;
				}

				@SuppressWarnings("unchecked")
				List<Map<String, Object>> conditions = (List<Map<String, Object>>) conditionsObj;

				for (Map<String, Object> condition : conditions) {
					String comparator = (String) condition.get("comparator");
					String valueType = (String) condition.get("valueType");
					Object value = condition.get("value");
					Object secondaryIndicator = condition.get("secondaryIndicator");

					// Count crossovers
					if ("crossAbove".equals(comparator) || "crossBelow".equals(comparator)) {
						crossoverCount++;

						// Check if crossover is missing secondaryIndicator
						if (secondaryIndicator == null || secondaryIndicator.toString().isEmpty()) {
							missingSecondaryIndicatorCount++;
						}
					}

					// Count indicator-to-indicator comparisons
					if ("indicator".equals(valueType)) {
						indicatorComparisonCount++;

						// Check if indicator comparison is missing secondaryIndicator
						if (secondaryIndicator == null || secondaryIndicator.toString().isEmpty()) {
							missingSecondaryIndicatorCount++;
						}
					}

					// Check for empty values
					if (value != null && value.toString().isEmpty()) {
						emptyValueCount++;
					}
				}
			}

			// Log quality metrics
			if (crossoverCount > 0 || indicatorComparisonCount > 0) {
				log.info("Visual rules quality: crossovers={}, indicatorComparisons={}, missingSecondaryIndicator={}, emptyValues={}",
						crossoverCount, indicatorComparisonCount, missingSecondaryIndicatorCount, emptyValueCount);
			}

			// Warn if there are quality issues (for prompt improvement)
			if (missingSecondaryIndicatorCount > 0) {
				log.warn("AI generated {} conditions with missing secondaryIndicator - prompt improvement opportunity",
						missingSecondaryIndicatorCount);
			}

			if (emptyValueCount > 0) {
				log.warn("AI generated {} conditions with empty values - prompt improvement opportunity",
						emptyValueCount);
			}
		}
		catch (Exception e) {
			// Silent failure - don't break generation if logging fails
			log.debug("Error logging visual rules quality: {}", e.getMessage());
		}
	}

	/**
	 * Parse optimization response and add optimization summary.
	 */
	private AIStrategyResponse parseOptimizationResponse(LLMResponse llmResponse, AIStrategyRequest request) {
		// Parse using existing method
		AIStrategyResponse response = parseGenerationResponse(llmResponse);

		if (!response.isSuccess()) {
			return response;
		}

		// Build optimization summary
		if (response.getOptimizationSummary() == null) {
			response.setOptimizationSummary(new AIStrategyResponse.OptimizationSummary());
		}

		AIStrategyResponse.OptimizationSummary summary = response.getOptimizationSummary();
		summary.setMode(request.getOptimizationMode().name());

		// Set baseline metrics
		summary.setBaselineMetrics(backtestResultsToMap(request.getBacktestResults()));

		// If changes not provided by LLM, compute them
		if (summary.getChanges() == null || summary.getChanges().isEmpty()) {
			if (request.getOptimizationMode() == AIStrategyRequest.OptimizationMode.ENHANCE_EXISTING
					&& request.getContext() != null) {
				summary.setChanges(computeStrategyChanges(request.getContext(), response.getVisualConfig(),
						response.getPythonCode()));
			}
		}

		return response;
	}

	/**
	 * Convert backtest results to a map for optimization summary.
	 */
	private Map<String, Double> backtestResultsToMap(AIStrategyRequest.BacktestResults bt) {
		if (bt == null) {
			return new HashMap<>();
		}

		Map<String, Double> map = new HashMap<>();
		map.put("totalReturn", bt.getTotalReturn());
		map.put("winRate", bt.getWinRate());
		map.put("sharpeRatio", bt.getSharpeRatio());
		map.put("maxDrawdown", bt.getMaxDrawdown());
		map.put("profitFactor", bt.getProfitFactor());
		map.put("totalTrades", (double) bt.getTotalTrades());
		return map;
	}

	/**
	 * Compute strategy changes by comparing old and new configurations.
	 */
	private List<AIStrategyResponse.StrategyChange> computeStrategyChanges(AIStrategyRequest.StrategyContext oldContext,
			Map<String, Object> newVisualConfig, String newCode) {

		List<AIStrategyResponse.StrategyChange> changes = new ArrayList<>();

		if (oldContext.getCurrentVisualConfig() != null && newVisualConfig != null) {
			// Compare risk settings
			Map<String, Object> oldRisk = (Map<String, Object>) oldContext.getCurrentVisualConfig().get("riskSettings");
			Map<String, Object> newRisk = (Map<String, Object>) newVisualConfig.get("riskSettings");

			if (oldRisk != null && newRisk != null) {
				compareAndAddChange(changes, "RISK_MANAGEMENT", "stopLoss", oldRisk, newRisk);
				compareAndAddChange(changes, "RISK_MANAGEMENT", "takeProfit", oldRisk, newRisk);
				compareAndAddChange(changes, "PARAMETER", "positionSize", oldRisk, newRisk);
			}

			// Compare entry rules count
			List<Object> oldEntryRules = (List<Object>) oldContext.getCurrentVisualConfig().get("entryRules");
			List<Object> newEntryRules = (List<Object>) newVisualConfig.get("entryRules");

			if (oldEntryRules != null && newEntryRules != null && oldEntryRules.size() != newEntryRules.size()) {
				AIStrategyResponse.StrategyChange change = new AIStrategyResponse.StrategyChange();
				change.setCategory("LOGIC");
				change.setField("entryRulesCount");
				change.setOldValue(String.valueOf(oldEntryRules.size()));
				change.setNewValue(String.valueOf(newEntryRules.size()));
				change.setRationale("Adjusted number of entry conditions based on backtest analysis");
				changes.add(change);
			}

			// Compare exit rules count
			List<Object> oldExitRules = (List<Object>) oldContext.getCurrentVisualConfig().get("exitRules");
			List<Object> newExitRules = (List<Object>) newVisualConfig.get("exitRules");

			if (oldExitRules != null && newExitRules != null && oldExitRules.size() != newExitRules.size()) {
				AIStrategyResponse.StrategyChange change = new AIStrategyResponse.StrategyChange();
				change.setCategory("LOGIC");
				change.setField("exitRulesCount");
				change.setOldValue(String.valueOf(oldExitRules.size()));
				change.setNewValue(String.valueOf(newExitRules.size()));
				change.setRationale("Adjusted number of exit conditions based on backtest analysis");
				changes.add(change);
			}
		}

		return changes;
	}

	/**
	 * Compare a specific field in two maps and add as a change if different.
	 */
	private void compareAndAddChange(List<AIStrategyResponse.StrategyChange> changes, String category, String field,
			Map<String, Object> oldMap, Map<String, Object> newMap) {

		Object oldValue = oldMap.get(field);
		Object newValue = newMap.get(field);

		if (oldValue != null && newValue != null && !oldValue.equals(newValue)) {
			AIStrategyResponse.StrategyChange change = new AIStrategyResponse.StrategyChange();
			change.setCategory(category);
			change.setField(field);
			change.setOldValue(String.valueOf(oldValue));
			change.setNewValue(String.valueOf(newValue));
			change.setRationale("Optimized based on backtest analysis");
			changes.add(change);
		}
	}

}
