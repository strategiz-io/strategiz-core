package io.strategiz.service.labs.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.strategiz.business.aichat.prompt.AIStrategyPrompts;
import io.strategiz.client.gemini.GeminiClient;
import io.strategiz.client.gemini.model.GeminiRequest;
import io.strategiz.client.gemini.model.GeminiResponse;
import io.strategiz.service.labs.model.AIStrategyRequest;
import io.strategiz.service.labs.model.AIStrategyResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for AI-powered strategy generation, explanation, and optimization.
 */
@Service
public class AIStrategyService {

	private static final Logger logger = LoggerFactory.getLogger(AIStrategyService.class);

	private final GeminiClient geminiClient;

	private final ObjectMapper objectMapper;

	@Autowired
	public AIStrategyService(GeminiClient geminiClient) {
		this.geminiClient = geminiClient;
		this.objectMapper = new ObjectMapper();
	}

	/**
	 * Generate a new strategy from a natural language prompt.
	 */
	public AIStrategyResponse generateStrategy(AIStrategyRequest request) {
		logger.info("Generating strategy from prompt: {}",
				request.getPrompt().substring(0, Math.min(50, request.getPrompt().length())));

		try {
			// Build the system prompt with context
			String symbols = request.getContext() != null && request.getContext().getSymbols() != null
					? String.join(", ", request.getContext().getSymbols()) : null;
			String timeframe = request.getContext() != null ? request.getContext().getTimeframe() : null;

			String systemPrompt = AIStrategyPrompts.buildGenerationPrompt(symbols, timeframe);

			// Build conversation history for Gemini
			List<GeminiRequest.Content> history = buildConversationHistory(systemPrompt,
					request.getConversationHistory());

			// Call Gemini (blocking)
			GeminiResponse geminiResponse = geminiClient.generateContent(request.getPrompt(), history).block();
			return parseGenerationResponse(geminiResponse);
		}
		catch (Exception e) {
			logger.error("Error generating strategy", e);
			return AIStrategyResponse.error("Failed to generate strategy: " + e.getMessage());
		}
	}

	/**
	 * Refine an existing strategy based on user feedback.
	 */
	public AIStrategyResponse refineStrategy(AIStrategyRequest request) {
		logger.info("Refining strategy with prompt: {}",
				request.getPrompt().substring(0, Math.min(50, request.getPrompt().length())));

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

			String refinementPrompt = AIStrategyPrompts.buildRefinementPrompt(visualConfigJson,
					request.getContext().getCurrentCode(), request.getPrompt());

			// Build conversation history
			List<GeminiRequest.Content> history = buildConversationHistory(AIStrategyPrompts.STRATEGY_GENERATION_SYSTEM,
					request.getConversationHistory());

			GeminiResponse geminiResponse = geminiClient.generateContent(refinementPrompt, history).block();
			return parseGenerationResponse(geminiResponse);
		}
		catch (Exception e) {
			logger.error("Error refining strategy", e);
			return AIStrategyResponse.error("Failed to refine strategy: " + e.getMessage());
		}
	}

	/**
	 * Parse Python code to extract visual configuration.
	 */
	public AIStrategyResponse parseCodeToVisual(String code) {
		logger.info("Parsing code to visual config");

		try {
			String prompt = AIStrategyPrompts.buildCodeToVisualPrompt(code);
			GeminiResponse geminiResponse = geminiClient.generateContent(prompt).block();

			AIStrategyResponse result = new AIStrategyResponse();
			String text = geminiResponse.getText();

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
				logger.error("Error parsing code-to-visual response", e);
				result.setSuccess(false);
				result.setError("Failed to parse code: " + e.getMessage());
				result.setCanRepresentVisually(false);
			}

			return result;
		}
		catch (Exception e) {
			logger.error("Error parsing code to visual", e);
			return AIStrategyResponse.error("Failed to parse code: " + e.getMessage());
		}
	}

	/**
	 * Explain a specific element (rule, condition, or code section).
	 */
	public AIStrategyResponse explainElement(AIStrategyRequest request) {
		logger.info("Explaining element: {}",
				request.getElementToExplain().substring(0, Math.min(50, request.getElementToExplain().length())));

		try {
			// Serialize context if available
			String contextJson = null;
			if (request.getContext() != null && request.getContext().getCurrentCode() != null) {
				contextJson = request.getContext().getCurrentCode();
			}

			String prompt = AIStrategyPrompts.buildExplainPrompt(request.getElementToExplain(), contextJson);
			GeminiResponse geminiResponse = geminiClient.generateContent(prompt).block();

			AIStrategyResponse result = new AIStrategyResponse();
			String text = geminiResponse.getText();

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
			logger.error("Error explaining element", e);
			return AIStrategyResponse.error("Failed to explain element: " + e.getMessage());
		}
	}

	/**
	 * Get optimization suggestions based on backtest results.
	 */
	public AIStrategyResponse optimizeFromBacktest(AIStrategyRequest request) {
		logger.info("Generating optimization suggestions from backtest results");

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

			GeminiResponse geminiResponse = geminiClient.generateContent(prompt).block();

			AIStrategyResponse result = new AIStrategyResponse();
			String text = geminiResponse.getText();

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
				logger.error("Error parsing optimization response", e);
				result.setExplanation(text);
				result.setSuccess(true);
			}

			return result;
		}
		catch (Exception e) {
			logger.error("Error generating optimizations", e);
			return AIStrategyResponse.error("Failed to generate optimizations: " + e.getMessage());
		}
	}

	/**
	 * Detect indicators from a partial prompt (for live preview).
	 */
	public AIStrategyResponse previewIndicators(String partialPrompt) {
		logger.debug("Previewing indicators for partial prompt: {}",
				partialPrompt.substring(0, Math.min(30, partialPrompt.length())));

		try {
			String prompt = AIStrategyPrompts.buildIndicatorPreviewPrompt(partialPrompt);
			GeminiResponse geminiResponse = geminiClient.generateContent(prompt).block();

			AIStrategyResponse result = new AIStrategyResponse();
			String text = geminiResponse.getText();

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
				logger.error("Error parsing indicator preview response", e);
				result.setDetectedIndicators(new ArrayList<>());
				result.setSuccess(true);
			}

			return result;
		}
		catch (Exception e) {
			logger.error("Error previewing indicators", e);
			AIStrategyResponse result = new AIStrategyResponse();
			result.setDetectedIndicators(new ArrayList<>());
			result.setSuccess(true);
			return result;
		}
	}

	/**
	 * Parse a natural language backtest query to extract date parameters.
	 */
	public Map<String, Object> parseBacktestQuery(String query) {
		logger.info("Parsing backtest query: {}", query);

		try {
			String currentDate = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
			String prompt = AIStrategyPrompts.buildBacktestQueryPrompt(query, currentDate);

			GeminiResponse geminiResponse = geminiClient.generateContent(prompt).block();
			String text = geminiResponse.getText();

			try {
				JsonNode json = extractJsonFromResponse(text);
				if (json != null) {
					return objectMapper.convertValue(json, Map.class);
				}
			}
			catch (Exception e) {
				logger.error("Error parsing backtest query response", e);
			}
		}
		catch (Exception e) {
			logger.error("Error parsing backtest query", e);
		}

		return new HashMap<>();
	}

	// Helper methods

	private List<GeminiRequest.Content> buildConversationHistory(String systemPrompt,
			List<AIStrategyRequest.ChatMessage> conversationHistory) {
		List<GeminiRequest.Content> history = new ArrayList<>();

		// Add system prompt as first user message
		history.add(new GeminiRequest.Content("user", systemPrompt));
		history.add(new GeminiRequest.Content("model",
				"I understand. I will generate trading strategies as structured JSON with both visual configuration and Python code."));

		// Add conversation history
		if (conversationHistory != null) {
			for (AIStrategyRequest.ChatMessage msg : conversationHistory) {
				String role = "user".equals(msg.getRole()) ? "user" : "model";
				history.add(new GeminiRequest.Content(role, msg.getContent()));
			}
		}

		return history;
	}

	private AIStrategyResponse parseGenerationResponse(GeminiResponse geminiResponse) {
		AIStrategyResponse result = new AIStrategyResponse();

		if (geminiResponse == null) {
			result.setSuccess(false);
			result.setError("No response from AI");
			return result;
		}

		String text = geminiResponse.getText();

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
			logger.error("Error parsing generation response", e);
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
				logger.debug("Failed to parse JSON from code block", e);
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
				logger.debug("Failed to parse raw JSON", e);
			}
		}

		// Try parsing the whole text as JSON
		try {
			return objectMapper.readTree(text);
		}
		catch (JsonProcessingException e) {
			logger.debug("Failed to parse text as JSON", e);
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

}
