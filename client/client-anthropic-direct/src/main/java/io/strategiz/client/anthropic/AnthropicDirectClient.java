package io.strategiz.client.anthropic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.strategiz.client.anthropic.config.AnthropicDirectConfig;
import io.strategiz.client.base.llm.LLMProvider;
import io.strategiz.client.base.llm.model.LLMMessage;
import io.strategiz.client.base.llm.model.LLMResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

/**
 * Direct client for Anthropic Claude API. Supports latest Claude 4.5 models: Opus 4.5,
 * Sonnet 4.5, Haiku 4.5 Bypasses Vertex AI for direct access to all Claude models.
 */
@Component
@ConditionalOnProperty(name = "anthropic.direct.enabled", havingValue = "true", matchIfMissing = false)
public class AnthropicDirectClient implements LLMProvider {

	private static final Logger logger = LoggerFactory.getLogger(AnthropicDirectClient.class);

	private static final String PROVIDER_NAME = "anthropic";

	private static final String API_VERSION = "2023-06-01";

	// Claude models (all available via direct API)
	// Claude 4.5 (latest)
	// Claude 3.5 (previous generation)
	// Claude 3 (older generation)
	private static final List<String> SUPPORTED_MODELS = List.of(
			// Claude 4.5
			"claude-opus-4-5-20251101", "claude-sonnet-4-5-20250514", "claude-haiku-4-5-20250514",
			// Claude 3.5
			"claude-3-5-sonnet-20241022", "claude-3-5-haiku-20241022",
			// Claude 3
			"claude-3-opus-20240229", "claude-3-sonnet-20240229", "claude-3-haiku-20240307");

	// User-friendly model IDs
	private static final List<String> USER_MODEL_IDS = List.of(
			// Claude 4.5
			"claude-opus-4-5", "claude-sonnet-4-5", "claude-haiku-4-5",
			// Claude 3.5
			"claude-3-5-sonnet", "claude-3-5-haiku",
			// Claude 3
			"claude-3-opus", "claude-3-sonnet", "claude-3-haiku");

	private final AnthropicDirectConfig config;

	private final ObjectMapper objectMapper;

	private final WebClient webClient;

	public AnthropicDirectClient(AnthropicDirectConfig config) {
		this.config = config;
		this.objectMapper = new ObjectMapper();
		this.webClient = WebClient.builder().baseUrl(config.getApiUrl())
			.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
			.defaultHeader("x-api-key", config.getApiKey())
			.defaultHeader("anthropic-version", API_VERSION)
			.build();

		logger.info("AnthropicDirectClient initialized with base URL: {}", config.getApiUrl());
	}

	@Override
	public Mono<LLMResponse> generateContent(String prompt, List<LLMMessage> history, String model) {
		logger.debug("Generating content with Anthropic Claude, model: {}", model);

		if (!config.isEnabled()) {
			return Mono.just(LLMResponse.error("Anthropic Direct API is not enabled"));
		}

		if (config.getApiKey() == null || config.getApiKey().isEmpty()) {
			return Mono.just(LLMResponse.error("Anthropic API key is not configured"));
		}

		try {
			String apiModel = mapToApiModel(model);
			String requestBody = buildRequestBody(prompt, history, apiModel);

			return webClient.post()
				.uri("/v1/messages")
				.bodyValue(requestBody)
				.retrieve()
				.onStatus(status -> status.isError(),
						response -> response.bodyToMono(String.class)
							.flatMap(errorBody -> Mono.error(new RuntimeException(
									"Anthropic API error: " + response.statusCode() + " - " + errorBody))))
				.bodyToMono(String.class)
				.timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
				.map(responseBody -> parseResponse(responseBody, model))
				.doOnSuccess(response -> logger.debug("Received response from Anthropic API"))
				.doOnError(error -> logger.error("Error calling Anthropic API", error))
				.onErrorResume(error -> Mono.just(LLMResponse.error("Failed to call Anthropic: " + error.getMessage())));
		}
		catch (Exception e) {
			logger.error("Error preparing Anthropic request", e);
			return Mono.just(LLMResponse.error("Failed to prepare request: " + e.getMessage()));
		}
	}

	@Override
	public Flux<LLMResponse> generateContentStream(String prompt, List<LLMMessage> history, String model) {
		logger.debug("Generating streaming content with Anthropic Claude, model: {}", model);

		if (!config.isEnabled()) {
			return Flux.just(LLMResponse.error("Anthropic Direct API is not enabled"));
		}

		try {
			String apiModel = mapToApiModel(model);
			String requestBody = buildStreamingRequestBody(prompt, history, apiModel);

			return webClient.post()
				.uri("/v1/messages")
				.accept(MediaType.TEXT_EVENT_STREAM)
				.bodyValue(requestBody)
				.retrieve()
				.bodyToFlux(String.class)
				.timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
				.filter(chunk -> chunk != null && !chunk.isEmpty())
				.map(chunk -> parseStreamChunk(chunk, model))
				.filter(response -> response.getContent() != null && !response.getContent().isEmpty())
				.doOnError(error -> logger.error("Error in Anthropic streaming", error))
				.onErrorResume(error -> Flux.just(LLMResponse.error("Stream error: " + error.getMessage())));
		}
		catch (Exception e) {
			logger.error("Error preparing Anthropic streaming request", e);
			return Flux.just(LLMResponse.error("Failed to prepare streaming request: " + e.getMessage()));
		}
	}

	@Override
	public String getProviderName() {
		return PROVIDER_NAME;
	}

	@Override
	public List<String> getSupportedModels() {
		return USER_MODEL_IDS;
	}

	/**
	 * Map user-friendly model ID to Anthropic API model name
	 */
	private String mapToApiModel(String model) {
		if (model == null || model.isEmpty()) {
			return config.getDefaultModel();
		}

		// Map user-friendly names to API model IDs with date suffixes
		return switch (model.toLowerCase()) {
			// Claude 4.5 models
			case "claude-opus-4-5" -> "claude-opus-4-5-20251101";
			case "claude-sonnet-4-5" -> "claude-sonnet-4-5-20250514";
			case "claude-haiku-4-5" -> "claude-haiku-4-5-20250514";
			// Claude 3.5 models
			case "claude-3-5-sonnet" -> "claude-3-5-sonnet-20241022";
			case "claude-3-5-haiku" -> "claude-3-5-haiku-20241022";
			// Claude 3 models
			case "claude-3-opus" -> "claude-3-opus-20240229";
			case "claude-3-sonnet" -> "claude-3-sonnet-20240229";
			case "claude-3-haiku" -> "claude-3-haiku-20240307";
			// Already has date suffix
			default -> model.contains("-202") ? model : config.getDefaultModel();
		};
	}

	/**
	 * Build the request body for Anthropic Messages API
	 */
	private String buildRequestBody(String prompt, List<LLMMessage> history, String model)
			throws JsonProcessingException {
		ObjectNode request = objectMapper.createObjectNode();
		request.put("model", model);
		request.put("max_tokens", config.getMaxTokens());
		request.put("temperature", config.getTemperature());

		// Build messages array
		ArrayNode messages = request.putArray("messages");

		// Add conversation history
		if (history != null) {
			for (LLMMessage msg : history) {
				ObjectNode messageNode = messages.addObject();
				// Claude uses "user" and "assistant" roles
				String role = "assistant".equals(msg.getRole()) ? "assistant" : "user";
				messageNode.put("role", role);
				messageNode.put("content", msg.getContent());
			}
		}

		// Add current prompt as user message
		ObjectNode promptMessage = messages.addObject();
		promptMessage.put("role", "user");
		promptMessage.put("content", prompt);

		return objectMapper.writeValueAsString(request);
	}

	/**
	 * Build streaming request body
	 */
	private String buildStreamingRequestBody(String prompt, List<LLMMessage> history, String model)
			throws JsonProcessingException {
		ObjectNode request = objectMapper.createObjectNode();
		request.put("model", model);
		request.put("max_tokens", config.getMaxTokens());
		request.put("temperature", config.getTemperature());
		request.put("stream", true);

		ArrayNode messages = request.putArray("messages");

		if (history != null) {
			for (LLMMessage msg : history) {
				ObjectNode messageNode = messages.addObject();
				String role = "assistant".equals(msg.getRole()) ? "assistant" : "user";
				messageNode.put("role", role);
				messageNode.put("content", msg.getContent());
			}
		}

		ObjectNode promptMessage = messages.addObject();
		promptMessage.put("role", "user");
		promptMessage.put("content", prompt);

		return objectMapper.writeValueAsString(request);
	}

	/**
	 * Parse Anthropic API response
	 */
	private LLMResponse parseResponse(String responseBody, String model) {
		try {
			JsonNode root = objectMapper.readTree(responseBody);

			LLMResponse response = new LLMResponse();
			response.setModel(model);
			response.setProvider(PROVIDER_NAME);
			response.setSuccess(true);

			// Extract content from Claude response
			if (root.has("content") && root.get("content").isArray()) {
				StringBuilder content = new StringBuilder();
				for (JsonNode contentBlock : root.get("content")) {
					if (contentBlock.has("text")) {
						content.append(contentBlock.get("text").asText());
					}
				}
				response.setContent(content.toString());
			}

			// Extract usage info
			if (root.has("usage")) {
				JsonNode usage = root.get("usage");
				if (usage.has("input_tokens")) {
					response.setPromptTokens(usage.get("input_tokens").asInt());
				}
				if (usage.has("output_tokens")) {
					response.setCompletionTokens(usage.get("output_tokens").asInt());
				}
				if (response.getPromptTokens() != null && response.getCompletionTokens() != null) {
					response.setTotalTokens(response.getPromptTokens() + response.getCompletionTokens());
				}
			}

			return response;
		}
		catch (Exception e) {
			logger.error("Error parsing Anthropic response", e);
			return LLMResponse.error("Failed to parse response: " + e.getMessage());
		}
	}

	/**
	 * Parse streaming chunk
	 */
	private LLMResponse parseStreamChunk(String chunk, String model) {
		try {
			// SSE format: "data: {...}" or "event: message_start\ndata: {...}"
			String jsonData = chunk;
			if (chunk.contains("data: ")) {
				// Extract JSON after "data: "
				int dataIndex = chunk.indexOf("data: ");
				jsonData = chunk.substring(dataIndex + 6).trim();
			}

			if (jsonData.isEmpty() || jsonData.equals("[DONE]")) {
				return new LLMResponse();
			}

			JsonNode root = objectMapper.readTree(jsonData);

			LLMResponse response = new LLMResponse();
			response.setModel(model);
			response.setProvider(PROVIDER_NAME);
			response.setSuccess(true);

			// Handle content_block_delta events
			if (root.has("type") && "content_block_delta".equals(root.get("type").asText())) {
				if (root.has("delta") && root.get("delta").has("text")) {
					response.setContent(root.get("delta").get("text").asText());
				}
			}

			return response;
		}
		catch (Exception e) {
			logger.debug("Error parsing stream chunk: {}", chunk);
			return new LLMResponse();
		}
	}

}
