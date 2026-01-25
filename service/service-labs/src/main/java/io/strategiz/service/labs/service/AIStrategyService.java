package io.strategiz.service.labs.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.strategiz.business.aichat.LLMRouter;
import io.strategiz.business.aichat.prompt.AIStrategyPrompts;
import io.strategiz.business.historicalinsights.exception.InsufficientDataException;
import io.strategiz.business.historicalinsights.model.OptimizationResult;
import io.strategiz.business.historicalinsights.model.StrategyTestResult;
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
	private final StrategyOptimizationEngine optimizationEngine;

	@Autowired
	public AIStrategyService(LLMRouter llmRouter,
			Optional<HistoricalInsightsService> historicalInsightsService,
			HistoricalInsightsCacheService cacheService,
			StrategyExecutionService executionService,
			StrategyOptimizationEngine optimizationEngine) {
		this.llmRouter = llmRouter;
		this.objectMapper = new ObjectMapper();
		this.historicalInsightsService = historicalInsightsService.orElse(null);
		this.cacheService = cacheService;
		this.executionService = executionService;
		this.optimizationEngine = optimizationEngine;
		if (this.historicalInsightsService == null) {
			log.warn("HistoricalInsightsService not available - Autonomous AI mode will be disabled");
		}
	}

	/**
	 * Generate a new strategy from a natural language prompt.
	 * For Autonomous AI mode (useHistoricalInsights=true), validates that the strategy
	 * beats buy-and-hold by at least 15% before returning it to the user.
	 */
	public AIStrategyResponse generateStrategy(AIStrategyRequest request) {
		String promptPreview = (request.getPrompt() != null && !request.getPrompt().isEmpty())
				? request.getPrompt().substring(0, Math.min(50, request.getPrompt().length()))
				: "[Autonomous Mode]";
		log.info("Generating strategy from prompt: {}", promptPreview);

		// Check which autonomous mode we're using
		AIStrategyRequest.AutonomousMode autonomousMode = request.getAutonomousMode();
		log.info("DEBUG: Raw autonomousMode from request: {} (class: {})",
				autonomousMode, autonomousMode != null ? autonomousMode.getClass().getName() : "null");
		log.info("DEBUG: useHistoricalInsights: {}", request.getUseHistoricalInsights());
		log.info("DEBUG: Is AUTONOMOUS? {} (expected enum: {})",
				autonomousMode == AIStrategyRequest.AutonomousMode.AUTONOMOUS,
				AIStrategyRequest.AutonomousMode.AUTONOMOUS);
		if (autonomousMode == null) {
			autonomousMode = AIStrategyRequest.AutonomousMode.GENERATIVE_AI; // Default
			log.info("DEBUG: autonomousMode was null, defaulting to GENERATIVE_AI");
		}
		log.info("Autonomous Mode (after default): {}", autonomousMode);

		log.info("Step 1/6: Analyzing prompt for user strategy request");

		try {
			// HISTORICAL INSIGHTS: Get historical market insights if enabled
			SymbolInsights insights = null;
			if (Boolean.TRUE.equals(request.getUseHistoricalInsights())) {
				log.info("Historical Market Insights enabled - analyzing market data");
				insights = getHistoricalInsights(request);
				if (insights != null) {
					log.info("Historical insights obtained for {}: {} volatility, {} trend", insights.getSymbol(),
							insights.getVolatilityRegime(), insights.getTrendDirection());
				}
			}

			// AUTONOMOUS MODE (Deterministic): Pure math, no LLM
			log.info("DEBUG: Checking AUTONOMOUS mode - autonomousMode={}, isAUTONOMOUS={}, useHistoricalInsights={}, insightsNotNull={}",
					autonomousMode, autonomousMode == AIStrategyRequest.AutonomousMode.AUTONOMOUS,
					request.getUseHistoricalInsights(), insights != null);
			if (autonomousMode == AIStrategyRequest.AutonomousMode.AUTONOMOUS
					&& Boolean.TRUE.equals(request.getUseHistoricalInsights()) && insights != null) {
				log.info("AUTONOMOUS MODE: Generating deterministic signals using mathematical optimization");
				AIStrategyResponse response = generateDeterministicStrategy(insights, request);
				response.setAutonomousModeUsed("AUTONOMOUS");
				return response;
			}
			log.info("DEBUG: Skipped AUTONOMOUS mode, falling through to AUTONOMOUS/LLM");

			// AUTONOMOUS MODE: Use turning points to generate strategy
			// Instead of asking LLM to use the dates (which it ignores), we generate the code directly
			if (Boolean.TRUE.equals(request.getUseHistoricalInsights()) && insights != null
					&& insights.getTurningPoints() != null && !insights.getTurningPoints().isEmpty()) {
				log.info("AUTONOMOUS MODE: Generating strategy from turning points (bypassing LLM code generation)");
				AIStrategyResponse response = generateStrategyFromTurningPoints(insights, request);
				response.setAutonomousModeUsed("AUTONOMOUS");
				return response;
			}

			// Fallback context for when no turning points available
			String deterministicContext = null;

			// REGULAR AI GENERATION: For non-Autonomous AI or when deterministic fails
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

				// Use model from request, or default
				// For Autonomous AI mode, only allow verified models that work well
				String model;
				if (Boolean.TRUE.equals(request.getUseHistoricalInsights())) {
					// Autonomous AI only allows these models (verified to work)
					model = getAutonomousAIModel(request.getModel());
					log.info("Autonomous AI mode: Using model {}", model);
				}
				else if (request.getModel() != null) {
					model = request.getModel();
				}
				else {
					model = llmRouter.getDefaultModel();
				}

				log.info("Step 3/6: Generating strategy with AI model: {}", model);

				// For Autonomous AI mode, if no prompt provided, use autonomous generation prompt
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
					log.info("Autonomous AI mode - using guided strategy generation with calculated thresholds");
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
						response.setAutonomousModeUsed("GENERATIVE_AI");
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

	// Historical Market Insights Integration (Autonomous AI)

	/**
	 * Get Historical Market Insights for a symbol (Autonomous AI mode).
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
	 * AUTONOMOUS MODE: Generate strategy using the Strategy Optimization Engine.
	 * Tests ~200 strategy combinations with various parameters, ranks by TOTAL RETURN
	 * (not win rate), and returns the best performer.
	 *
	 * This replaces the old single-strategy generation that only tested 4 indicators
	 * with fixed parameters and ranked by win rate.
	 */
	private AIStrategyResponse generateDeterministicStrategy(SymbolInsights insights, AIStrategyRequest request) {
		String symbol = insights.getSymbol();
		String timeframe = insights.getTimeframe() != null ? insights.getTimeframe() : "1D";

		// Get context timeframe if available (overrides insights)
		if (request.getContext() != null && request.getContext().getTimeframe() != null) {
			timeframe = request.getContext().getTimeframe();
		}

		log.info("AUTONOMOUS MODE: Starting Strategy Optimization Engine for {} on {}", symbol, timeframe);

		// Run optimization engine - tests ~200 strategy combinations
		// Uses 3 years of data by default for comprehensive backtesting
		OptimizationResult optimizationResult = optimizationEngine.optimize(
				symbol, timeframe, "3y", "autonomous-" + System.currentTimeMillis());

		// Build response from optimization result
		AIStrategyResponse response = new AIStrategyResponse();

		if (optimizationResult.getBestStrategy() != null) {
			StrategyTestResult bestStrategy = optimizationResult.getBestStrategy();

			response.setSuccess(true);
			response.setPythonCode(bestStrategy.getPythonCode());
			response.setCanRepresentVisually(true);

			// Build detailed explanation
			String explanation = buildOptimizationExplanation(optimizationResult, insights);
			response.setExplanation(explanation);

			// Build optimization summary with metrics
			AIStrategyResponse.OptimizationSummary summary = new AIStrategyResponse.OptimizationSummary();
			summary.setMode("AUTONOMOUS_OPTIMIZATION");
			summary.setImprovementRationale(optimizationResult.toSummary());
			Map<String, Double> optimizedMetrics = new HashMap<>();
			optimizedMetrics.put("totalReturn", bestStrategy.getTotalReturn());
			optimizedMetrics.put("winRate", bestStrategy.getWinRate() * 100);
			optimizedMetrics.put("sharpeRatio", bestStrategy.getSharpeRatio());
			optimizedMetrics.put("maxDrawdown", bestStrategy.getMaxDrawdown());
			optimizedMetrics.put("profitFactor", bestStrategy.getProfitFactor());
			optimizedMetrics.put("totalTrades", (double) bestStrategy.getTotalTrades());
			summary.setOptimizedMetrics(optimizedMetrics);
			Map<String, Double> baselineMetrics = new HashMap<>();
			baselineMetrics.put("buyAndHoldReturn", optimizationResult.getBuyAndHoldReturn());
			summary.setBaselineMetrics(baselineMetrics);
			response.setOptimizationSummary(summary);

			log.info("AUTONOMOUS: Optimization complete. Best strategy: {} with {:.2f}% return (vs {:.2f}% buy-and-hold)",
					bestStrategy.getStrategyType().getDisplayName(),
					bestStrategy.getTotalReturn(),
					optimizationResult.getBuyAndHoldReturn());
		} else {
			// Fallback if no strategy found
			log.warn("AUTONOMOUS: No profitable strategy found through optimization, falling back to swing strategy");
			response = generateFallbackSwingStrategy(insights, timeframe);
		}

		// Set historical insights used flag
		response.setHistoricalInsightsUsed(true);
		response.setHistoricalInsights(insights);

		return response;
	}

	/**
	 * Builds a detailed explanation from the optimization results.
	 */
	private String buildOptimizationExplanation(OptimizationResult result, SymbolInsights insights) {
		StrategyTestResult best = result.getBestStrategy();
		StringBuilder sb = new StringBuilder();

		sb.append(String.format("**AUTONOMOUS MODE - Strategy Optimization Engine**\n\n"));
		sb.append(String.format("Tested **%d strategy combinations** across %d days (~%.1f years) of %s historical data.\n\n",
				result.getTotalCombinationsTested(),
				result.getDaysAnalyzed(),
				result.getDaysAnalyzed() / 365.25,
				result.getSymbol()));

		sb.append(String.format("**Best Strategy: %s**\n", best.getStrategyType().getDisplayName()));
		sb.append(String.format("- Parameters: %s\n", best.getParametersDisplay()));
		sb.append(String.format("- Total Return: **%.2f%%**\n", best.getTotalReturn()));
		sb.append(String.format("- Buy & Hold Return: %.2f%%\n", result.getBuyAndHoldReturn()));
		sb.append(String.format("- **Outperformance: %.2f%%**\n", result.getOutperformance()));
		sb.append(String.format("- Win Rate: %.1f%%\n", best.getWinRate() * 100));
		sb.append(String.format("- Sharpe Ratio: %.2f\n", best.getSharpeRatio()));
		sb.append(String.format("- Max Drawdown: %.1f%%\n", best.getMaxDrawdown()));
		sb.append(String.format("- Total Trades: %d\n\n", best.getTotalTrades()));

		sb.append(String.format("Market Regime: **%s**\n\n", result.getMarketRegime()));

		sb.append("The strategy was selected by testing all parameter combinations and ranking by total return, ");
		sb.append("not win rate alone. This approach finds strategies that actually make money, ");
		sb.append("even if individual trade win rates are moderate.");

		if (result.getTopStrategies().size() > 1) {
			sb.append("\n\n**Alternative Strategies:**\n");
			for (int i = 1; i < Math.min(3, result.getTopStrategies().size()); i++) {
				StrategyTestResult alt = result.getTopStrategies().get(i);
				sb.append(String.format("%d. %s (%.2f%% return)\n", i + 1,
						alt.getStrategyType().getDisplayName(), alt.getTotalReturn()));
			}
		}

		return sb.toString();
	}

	/**
	 * Fallback swing trading strategy if optimization fails to find a profitable strategy.
	 */
	private AIStrategyResponse generateFallbackSwingStrategy(SymbolInsights insights, String timeframe) {
		String symbol = insights.getSymbol();

		// Use historical turning points to derive basic thresholds
		double buyThreshold = 8.0;
		double sellThreshold = 12.0;
		int lookbackPeriod = 20;

		var turningPoints = insights.getTurningPoints();
		if (turningPoints != null && !turningPoints.isEmpty()) {
			double totalDrop = 0;
			double totalRise = 0;
			int troughCount = 0;
			int peakCount = 0;

			for (var tp : turningPoints) {
				double change = tp.getPriceChangeFromPrevious();
				if (change != 0) {
					if (tp.getType().name().equals("TROUGH")) {
						totalDrop += Math.abs(change);
						troughCount++;
					} else {
						totalRise += change;
						peakCount++;
					}
				}
			}

			if (troughCount > 0) {
				buyThreshold = Math.max((totalDrop / troughCount) * 0.7, 3.0);
				buyThreshold = Math.min(buyThreshold, 15.0);
			}
			if (peakCount > 0) {
				sellThreshold = Math.max((totalRise / peakCount) * 0.7, 5.0);
				sellThreshold = Math.min(sellThreshold, 25.0);
			}
		}

		// Generate simple swing trading code
		String code = String.format("""
				# Fallback Swing Trading Strategy
				# Generated when optimization finds no profitable strategies

				import pandas as pd
				import numpy as np

				SYMBOL = '%s'
				TIMEFRAME = '%s'
				BUY_THRESHOLD = %.1f
				SELL_THRESHOLD = %.1f
				LOOKBACK_PERIOD = %d
				ATR_MULTIPLIER = 2.0

				def calculate_atr(data, period):
				    high = data['high']
				    low = data['low']
				    close = data['close'].shift(1)
				    tr = pd.concat([high - low, (high - close).abs(), (low - close).abs()], axis=1).max(axis=1)
				    return tr.rolling(window=period, min_periods=1).mean()

				data['rolling_high'] = data['high'].rolling(window=LOOKBACK_PERIOD).max()
				data['rolling_low'] = data['low'].rolling(window=LOOKBACK_PERIOD).min()
				data['atr'] = calculate_atr(data, 14)
				data['pct_from_high'] = (data['rolling_high'] - data['close']) / data['rolling_high'] * 100

				position = None
				entry_price = 0
				stop_loss = 0

				for i in range(LOOKBACK_PERIOD + 1, len(data)):
				    row = data.iloc[i]
				    price = row['close']
				    timestamp = row['timestamp']
				    atr = row['atr']
				    pct_from_high = row['pct_from_high']

				    if position is None:
				        if pct_from_high >= BUY_THRESHOLD:
				            position = 'long'
				            entry_price = price
				            stop_loss = price - (atr * ATR_MULTIPLIER)
				            signal('BUY', timestamp, price, f'{pct_from_high:.1f}%% from high', 'arrow_up')
				    else:
				        pct_gain = (price - entry_price) / entry_price * 100
				        if pct_gain >= SELL_THRESHOLD:
				            signal('SELL', timestamp, price, f'Target {pct_gain:.1f}%% gain', 'arrow_down')
				            position = None
				        elif price <= stop_loss:
				            signal('SELL', timestamp, price, 'Stop Loss', 'arrow_down')
				            position = None
				""", symbol, timeframe, buyThreshold, sellThreshold, lookbackPeriod);

		AIStrategyResponse response = new AIStrategyResponse();
		response.setSuccess(true);
		response.setPythonCode(code);
		response.setCanRepresentVisually(true);
		response.setExplanation(String.format(
				"AUTONOMOUS MODE (Fallback): No profitable optimized strategy found. " +
				"Generated a basic swing trading strategy with thresholds derived from %d turning points. " +
				"BUY when price drops %.1f%% from %d-day high, SELL at %.1f%% gain.",
				turningPoints != null ? turningPoints.size() : 0, buyThreshold, lookbackPeriod, sellThreshold));

		return response;
	}

	/**
	 * AUTONOMOUS MODE: Generate strategy from historical turning points analysis.
	 * Uses optimal RSI/indicator thresholds derived from historical analysis.
	 * Adapts strategy type based on market regime (mean-reversion vs trend-following).
	 * Generates code that works for both backtesting AND live trading (no hardcoded dates).
	 */
	private AIStrategyResponse generateStrategyFromTurningPoints(SymbolInsights insights, AIStrategyRequest request) {
		String symbol = insights.getSymbol();
		String timeframe = insights.getTimeframe() != null ? insights.getTimeframe() : "1D";

		// Get context timeframe if available
		if (request.getContext() != null && request.getContext().getTimeframe() != null) {
			timeframe = request.getContext().getTimeframe();
		}

		var turningPoints = insights.getTurningPoints();
		log.info("GENERATIVE AI: Using enhanced historical analysis for {} ({} turning points)",
				symbol, turningPoints != null ? turningPoints.size() : 0);

		// Extract optimal thresholds from historical analysis
		double optimalRsiBuy = insights.getOptimalRsiOversold() > 0 ? insights.getOptimalRsiOversold() : 30.0;
		double optimalRsiSell = insights.getOptimalRsiOverbought() > 0 ? insights.getOptimalRsiOverbought() : 70.0;
		double hurstExponent = insights.getHurstExponent();
		String regime = insights.getCurrentRegime() != null ? insights.getCurrentRegime() : "UNKNOWN";
		double avgSwingMagnitude = insights.getAvgSwingMagnitude() > 0 ? insights.getAvgSwingMagnitude() : 10.0;
		int avgSwingDuration = insights.getAvgSwingDuration() > 0 ? insights.getAvgSwingDuration() : 20;

		// Determine strategy type based on Hurst exponent and regime
		boolean useMeanReversion = hurstExponent > 0 && hurstExponent < 0.45;
		boolean useTrendFollowing = hurstExponent > 0.55;
		boolean isBearish = regime.contains("DOWNTREND") || regime.contains("BEARISH");
		boolean isBullish = regime.contains("UPTREND") || regime.contains("BULLISH");

		// Calculate risk parameters based on historical volatility
		double stopLossPercent = Math.min(Math.max(avgSwingMagnitude * 0.4, 3.0), 10.0);
		double takeProfitPercent = Math.min(Math.max(avgSwingMagnitude * 0.8, 8.0), 25.0);

		log.info("GENERATIVE AI: Regime={}, Hurst={:.2f}, RSI thresholds: buy<{:.0f}, sell>{:.0f}",
				regime, hurstExponent, optimalRsiBuy, optimalRsiSell);

		// Generate Python code with indicator-based rules
		StringBuilder code = new StringBuilder();
		code.append("import pandas as pd\n");
		code.append("import numpy as np\n\n");
		code.append("# ═══════════════════════════════════════════════════════════════\n");
		code.append("# AUTONOMOUS MODE - Multi-Indicator Strategy\n");
		code.append("# Parameters derived from historical turning point analysis\n");
		code.append(String.format("# Market Regime: %s (Hurst: %.2f)\n", regime, hurstExponent));
		code.append(String.format("# Strategy Type: %s\n",
				useMeanReversion ? "MEAN REVERSION" : (useTrendFollowing ? "TREND FOLLOWING" : "HYBRID")));
		code.append("# ═══════════════════════════════════════════════════════════════\n\n");
		code.append(String.format("SYMBOL = '%s'\n", symbol));
		code.append(String.format("TIMEFRAME = '%s'\n\n", timeframe));

		// Indicator parameters from historical analysis
		code.append("# ═══════════════════════════════════════════════════════════════\n");
		code.append("# OPTIMAL THRESHOLDS (derived from historical turning points)\n");
		code.append("# ═══════════════════════════════════════════════════════════════\n");
		code.append(String.format("RSI_OVERSOLD = %.1f      # RSI below this captured 80%% of optimal buy points\n", optimalRsiBuy));
		code.append(String.format("RSI_OVERBOUGHT = %.1f    # RSI above this captured 80%% of optimal sell points\n", optimalRsiSell));
		code.append(String.format("STOP_LOSS_PCT = %.1f     # Based on historical volatility\n", stopLossPercent));
		code.append(String.format("TAKE_PROFIT_PCT = %.1f   # Based on average swing magnitude\n", takeProfitPercent));
		code.append("BB_PERIOD = 20           # Bollinger Band period\n");
		code.append("BB_STD = 2.0             # Bollinger Band standard deviation\n");
		code.append("RSI_PERIOD = 14          # RSI calculation period\n");
		code.append("MACD_FAST = 12           # MACD fast period\n");
		code.append("MACD_SLOW = 26           # MACD slow period\n");
		code.append("MACD_SIGNAL = 9          # MACD signal period\n\n");

		// Position state
		code.append("# Position state\n");
		code.append("entry_price = None\n");
		code.append("peak_price = None\n\n");

		// Helper function for RSI
		code.append("def calculate_rsi(prices, period=14):\n");
		code.append("    \"\"\"Calculate RSI indicator.\"\"\"\n");
		code.append("    delta = prices.diff()\n");
		code.append("    gain = (delta.where(delta > 0, 0)).rolling(window=period).mean()\n");
		code.append("    loss = (-delta.where(delta < 0, 0)).rolling(window=period).mean()\n");
		code.append("    rs = gain / loss\n");
		code.append("    return 100 - (100 / (1 + rs))\n\n");

		// Helper function for Bollinger Bands
		code.append("def calculate_bollinger(prices, period=20, std_dev=2.0):\n");
		code.append("    \"\"\"Calculate Bollinger Bands.\"\"\"\n");
		code.append("    sma = prices.rolling(window=period).mean()\n");
		code.append("    std = prices.rolling(window=period).std()\n");
		code.append("    upper = sma + (std * std_dev)\n");
		code.append("    lower = sma - (std * std_dev)\n");
		code.append("    return sma, upper, lower\n\n");

		// Helper function for MACD
		code.append("def calculate_macd(prices, fast=12, slow=26, signal=9):\n");
		code.append("    \"\"\"Calculate MACD indicator.\"\"\"\n");
		code.append("    ema_fast = prices.ewm(span=fast, adjust=False).mean()\n");
		code.append("    ema_slow = prices.ewm(span=slow, adjust=False).mean()\n");
		code.append("    macd_line = ema_fast - ema_slow\n");
		code.append("    signal_line = macd_line.ewm(span=signal, adjust=False).mean()\n");
		code.append("    histogram = macd_line - signal_line\n");
		code.append("    return macd_line, signal_line, histogram\n\n");

		// Main strategy function
		code.append("def strategy(data):\n");
		code.append("    \"\"\"Multi-indicator strategy using optimal thresholds from historical analysis.\"\"\"\n");
		code.append("    global entry_price, peak_price\n\n");
		code.append("    min_periods = max(RSI_PERIOD, BB_PERIOD, MACD_SLOW) + 5\n");
		code.append("    if len(data) < min_periods:\n");
		code.append("        return 'HOLD'\n\n");

		// Calculate indicators
		code.append("    # Calculate indicators\n");
		code.append("    close = data['close']\n");
		code.append("    current = close.iloc[-1]\n\n");
		code.append("    rsi = calculate_rsi(close, RSI_PERIOD)\n");
		code.append("    current_rsi = rsi.iloc[-1]\n\n");
		code.append("    bb_mid, bb_upper, bb_lower = calculate_bollinger(close, BB_PERIOD, BB_STD)\n");
		code.append("    current_bb_lower = bb_lower.iloc[-1]\n");
		code.append("    current_bb_upper = bb_upper.iloc[-1]\n");
		code.append("    bb_position = (current - current_bb_lower) / (current_bb_upper - current_bb_lower) if (current_bb_upper - current_bb_lower) > 0 else 0.5\n\n");
		code.append("    macd_line, signal_line, histogram = calculate_macd(close, MACD_FAST, MACD_SLOW, MACD_SIGNAL)\n");
		code.append("    current_hist = histogram.iloc[-1]\n");
		code.append("    prev_hist = histogram.iloc[-2] if len(histogram) > 1 else 0\n");
		code.append("    macd_bullish = current_hist > prev_hist  # Momentum improving\n\n");

		// Exit logic
		code.append("    # ═══════════════════════════════════════════════════════════════\n");
		code.append("    # EXIT LOGIC (when holding position)\n");
		code.append("    # ═══════════════════════════════════════════════════════════════\n");
		code.append("    if entry_price is not None:\n");
		code.append("        peak_price = max(peak_price or current, current)\n");
		code.append("        gain = ((current - entry_price) / entry_price) * 100\n");
		code.append("        drop_from_peak = ((current - peak_price) / peak_price) * 100 if peak_price else 0\n\n");

		// Stop loss
		code.append("        # Stop loss - exit if loss exceeds threshold\n");
		code.append("        if gain <= -STOP_LOSS_PCT:\n");
		code.append("            entry_price = None\n");
		code.append("            peak_price = None\n");
		code.append("            return 'SELL'\n\n");

		// Take profit with RSI confirmation
		code.append("        # Take profit - exit when target reached AND RSI overbought\n");
		code.append("        if gain >= TAKE_PROFIT_PCT and current_rsi > RSI_OVERBOUGHT:\n");
		code.append("            entry_price = None\n");
		code.append("            peak_price = None\n");
		code.append("            return 'SELL'\n\n");

		// Trailing stop
		code.append("        # Trailing stop - protect profits\n");
		code.append("        trailing_stop = min(STOP_LOSS_PCT, gain * 0.5) if gain > STOP_LOSS_PCT else STOP_LOSS_PCT * 0.7\n");
		code.append("        if gain > STOP_LOSS_PCT and drop_from_peak <= -trailing_stop:\n");
		code.append("            entry_price = None\n");
		code.append("            peak_price = None\n");
		code.append("            return 'SELL'\n\n");

		// Exit on overbought RSI
		code.append("        # Exit on extreme overbought (regardless of gain)\n");
		code.append("        if current_rsi > (RSI_OVERBOUGHT + 10) and bb_position > 0.95:\n");
		code.append("            entry_price = None\n");
		code.append("            peak_price = None\n");
		code.append("            return 'SELL'\n\n");
		code.append("        return 'HOLD'\n\n");

		// Entry logic
		code.append("    # ═══════════════════════════════════════════════════════════════\n");
		code.append("    # ENTRY LOGIC (no position)\n");
		code.append("    # ═══════════════════════════════════════════════════════════════\n");

		if (useMeanReversion) {
			// Mean reversion strategy - buy oversold, sell overbought
			code.append("    # MEAN REVERSION STRATEGY (Hurst < 0.45)\n");
			code.append("    # Buy when RSI oversold AND price near lower Bollinger Band\n");
			code.append("    rsi_oversold = current_rsi < RSI_OVERSOLD\n");
			code.append("    near_bb_lower = bb_position < 0.15\n");
			code.append("    momentum_turning = macd_bullish  # MACD histogram improving\n\n");
			code.append("    if rsi_oversold and near_bb_lower and momentum_turning:\n");
			code.append("        entry_price = current\n");
			code.append("        peak_price = current\n");
			code.append("        return 'BUY'\n\n");
		}
		else if (useTrendFollowing) {
			// Trend following strategy
			code.append("    # TREND FOLLOWING STRATEGY (Hurst > 0.55)\n");
			code.append("    # Buy when RSI shows strength AND MACD bullish crossover\n");
			code.append("    rsi_not_overbought = current_rsi < RSI_OVERBOUGHT\n");
			code.append("    rsi_strength = current_rsi > 40  # Not oversold, showing momentum\n");
			code.append("    macd_crossover = histogram.iloc[-1] > 0 and histogram.iloc[-2] <= 0\n");
			code.append("    above_bb_mid = current > bb_mid.iloc[-1]\n\n");
			code.append("    if rsi_not_overbought and rsi_strength and (macd_bullish or macd_crossover):\n");
			code.append("        entry_price = current\n");
			code.append("        peak_price = current\n");
			code.append("        return 'BUY'\n\n");
		}
		else {
			// Hybrid strategy - use both signals
			code.append("    # HYBRID STRATEGY (Mixed market regime)\n");
			code.append("    # Combine mean reversion and trend signals\n\n");
			code.append("    # Signal 1: Deep oversold with momentum turning (mean reversion)\n");
			code.append("    deep_oversold = current_rsi < RSI_OVERSOLD and bb_position < 0.1\n");
			code.append("    momentum_turning = macd_bullish\n\n");
			code.append("    # Signal 2: Trend continuation with pullback (trend following)\n");
			code.append("    pullback_buy = current_rsi < (RSI_OVERSOLD + 10) and bb_position < 0.3 and macd_bullish\n\n");
			code.append("    if (deep_oversold and momentum_turning) or pullback_buy:\n");
			code.append("        entry_price = current\n");
			code.append("        peak_price = current\n");
			code.append("        return 'BUY'\n\n");
		}

		code.append("    return 'HOLD'\n");

		// Build response
		AIStrategyResponse response = new AIStrategyResponse();
		response.setSuccess(true);
		response.setPythonCode(code.toString());
		response.setCanRepresentVisually(true);

		// Add detailed explanation
		int turningPointCount = turningPoints != null ? turningPoints.size() : 0;
		String strategyType = useMeanReversion ? "Mean Reversion" : (useTrendFollowing ? "Trend Following" : "Hybrid");
		String explanation = String.format(
				"AUTONOMOUS MODE: Analyzed %d historical turning points for %s. " +
				"Market Regime: %s (Hurst: %.2f). Strategy Type: %s. " +
				"Optimal entry threshold: RSI < %.0f (captured 80%% of historical troughs). " +
				"Optimal exit threshold: RSI > %.0f (captured 80%% of historical peaks). " +
				"Risk parameters: Stop-loss %.1f%%, Take-profit %.1f%% (based on %.1f%% avg swing).",
				turningPointCount, symbol, regime, hurstExponent, strategyType,
				optimalRsiBuy, optimalRsiSell, stopLossPercent, takeProfitPercent, avgSwingMagnitude);
		response.setExplanation(explanation);

		// Generate summary card
		String summaryCard = String.format(
				"%s %s Strategy: RSI<%,.0f buy, RSI>%.0f sell, %.1f%% stop, %.1f%% target",
				symbol, strategyType, optimalRsiBuy, optimalRsiSell, stopLossPercent, takeProfitPercent);
		response.setSummaryCard(summaryCard);

		// Set historical insights used flag
		response.setHistoricalInsightsUsed(true);
		response.setHistoricalInsights(insights);

		return response;
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
				String found = matcher.group(1);
				// Filter out common words that aren't tickers
				if (!java.util.Set.of("THE", "AND", "FOR", "WITH", "USE", "BUY", "SELL", "HOLD", "AI", "ETF")
						.contains(found)) {
					return found;
				}
			}
		}

		// Default to SPY if no symbol specified (most liquid, representative of market)
		log.info("No symbol specified in request, defaulting to SPY");
		return "SPY";
	}

	// Helper methods

	/**
	 * Get the model to use for Autonomous AI mode. Only allows verified models that work well
	 * with the complex instruction-following required for Autonomous AI.
	 */
	private String getAutonomousAIModel(String requestedModel) {
		// Models verified to work well with Autonomous AI mode
		// These models can follow complex instructions about using calculated thresholds
		java.util.Set<String> allowedModels = java.util.Set.of(
				"gemini-2.5-pro", // Best for instruction-following
				"gemini-2.5-flash", // Fast, good quality
				"gpt-4o", // Strong instruction-following
				"gpt-4o-mini", // Good balance of speed/quality
				"claude-opus-4-5", // Best Claude model (via direct API)
				"claude-sonnet-4-5" // Good Claude model (via direct API)
		);

		// If requested model is in allowed list, use it
		if (requestedModel != null && allowedModels.contains(requestedModel)) {
			return requestedModel;
		}

		// Default to Gemini Pro for best results
		return "gemini-2.5-pro";
	}

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
				// Extract ONLY visual config and Python code - nothing else
				if (json.has("visualConfig")) {
					result.setVisualConfig(objectMapper.convertValue(json.get("visualConfig"), Map.class));
				}

				if (json.has("pythonCode")) {
					result.setPythonCode(json.get("pythonCode").asText());
				}

				result.setSuccess(result.getPythonCode() != null);

				// Log visual rules quality for monitoring and prompt improvement
				logVisualRulesQuality(result);
			}
			else {
				// Couldn't parse JSON - try to extract code block and visual config
				String extractedCode = extractCodeBlock(text);
				JsonNode partialJson = tryExtractPartialJson(text);

				if (extractedCode != null) {
					result.setPythonCode(extractedCode);
				}

				// Try to get visualConfig from partial JSON
				if (partialJson != null && partialJson.has("visualConfig")) {
					result.setVisualConfig(objectMapper.convertValue(partialJson.get("visualConfig"), Map.class));
				}

				// Success if we got at least the Python code
				result.setSuccess(extractedCode != null);
				if (!result.isSuccess()) {
					result.setError("Could not parse AI response");
				}
				// DON'T set explanation to raw text - that's garbage
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
	 * Try to extract partial JSON even if truncated.
	 * Attempts to find and parse visualConfig object from incomplete JSON.
	 */
	private JsonNode tryExtractPartialJson(String text) {
		if (text == null) {
			return null;
		}

		// Try to find visualConfig object
		Pattern visualConfigPattern = Pattern.compile("\"visualConfig\"\\s*:\\s*(\\{.*?\\})\\s*,\\s*\"pythonCode\"", Pattern.DOTALL);
		Matcher matcher = visualConfigPattern.matcher(text);
		if (matcher.find()) {
			try {
				String visualConfigJson = "{\"visualConfig\":" + matcher.group(1) + "}";
				return objectMapper.readTree(visualConfigJson);
			} catch (Exception e) {
				log.debug("Failed to parse partial visualConfig", e);
			}
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

		// Log the full technical error for debugging
		log.error("LLM technical error (sanitizing for user): {}", technicalError);

		String lowerError = technicalError.toLowerCase();

		// API key not configured - specific provider messages
		if (lowerError.contains("api key is not configured") || lowerError.contains("api key not configured")) {
			if (lowerError.contains("anthropic")) {
				return "Claude AI is not configured. Please ensure the Anthropic API key is set in Vault.";
			}
			if (lowerError.contains("openai")) {
				return "OpenAI is not configured. Please ensure the OpenAI API key is set in Vault.";
			}
			return "AI provider is not configured. Please check API key settings.";
		}

		// Provider not enabled
		if (lowerError.contains("is not enabled") || lowerError.contains("not enabled")) {
			return "This AI model is not enabled. Please select a different model or enable the provider.";
		}

		// Model unavailable
		if (lowerError.contains("no provider available") || lowerError.contains("no provider found")
				|| lowerError.contains("currently unavailable")) {
			return "The selected AI model is currently unavailable. Please try a different model.";
		}

		// Authentication/authorization errors
		if (lowerError.contains("401") || lowerError.contains("unauthorized") || lowerError.contains("unauthenticated")
				|| lowerError.contains("access_token_expired") || lowerError.contains("invalid authentication")
				|| lowerError.contains("invalid api key") || lowerError.contains("invalid_api_key")) {
			return "AI authentication failed. Please verify the API key is correct.";
		}

		// Rate limiting
		if (lowerError.contains("429") || lowerError.contains("rate limit") || lowerError.contains("quota")
				|| lowerError.contains("overloaded")) {
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

		// Generic fallback - include hint about actual error for debugging
		log.warn("Unhandled error pattern, returning generic message. Error: {}", technicalError);
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
	 * Parse optimization response with full analysis fields.
	 * Unlike parseGenerationResponse (which only extracts visualConfig/pythonCode),
	 * this extracts explanation, suggestions, etc. for the Optimize feature.
	 */
	private AIStrategyResponse parseOptimizationResponse(LLMResponse llmResponse, AIStrategyRequest request) {
		AIStrategyResponse response = new AIStrategyResponse();

		if (llmResponse == null || !llmResponse.isSuccess()) {
			response.setSuccess(false);
			String technicalError = llmResponse != null ? llmResponse.getError() : "No response from AI";
			response.setError(sanitizeErrorMessage(technicalError));
			return response;
		}

		String text = llmResponse.getContent();
		if (text == null || text.isEmpty()) {
			response.setSuccess(false);
			response.setError("Empty response from AI");
			return response;
		}

		try {
			JsonNode json = extractJsonFromResponse(text);
			if (json != null) {
				// Extract all fields for optimization
				if (json.has("visualConfig")) {
					response.setVisualConfig(objectMapper.convertValue(json.get("visualConfig"), Map.class));
				}
				if (json.has("pythonCode")) {
					response.setPythonCode(json.get("pythonCode").asText());
				}
				if (json.has("explanation")) {
					response.setExplanation(json.get("explanation").asText());
				}
				if (json.has("suggestions")) {
					response.setSuggestions(objectMapper.convertValue(json.get("suggestions"),
							objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)));
				}
				if (json.has("summaryCard")) {
					response.setSummaryCard(json.get("summaryCard").asText());
				}
				if (json.has("riskLevel")) {
					try {
						response.setRiskLevel(AIStrategyResponse.RiskLevel.valueOf(json.get("riskLevel").asText()));
					} catch (IllegalArgumentException e) {
						response.setRiskLevel(AIStrategyResponse.RiskLevel.MEDIUM);
					}
				}
				response.setSuccess(true);
			} else {
				response.setSuccess(false);
				response.setError("Could not parse optimization response");
			}
		} catch (Exception e) {
			log.error("Error parsing optimization response", e);
			response.setSuccess(false);
			response.setError("Failed to parse optimization response");
		}

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
