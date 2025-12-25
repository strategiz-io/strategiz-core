package io.strategiz.client.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.strategiz.client.base.llm.LLMProvider;
import io.strategiz.client.base.llm.model.LLMMessage;
import io.strategiz.client.base.llm.model.LLMResponse;
import io.strategiz.client.openai.config.OpenAIDirectConfig;
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
 * Direct client for OpenAI API. Supports all OpenAI models: GPT-4o, GPT-4 Turbo, o1,
 * o1-mini Bypasses Vertex AI for full model access.
 */
@Component
@ConditionalOnProperty(name = "openai.direct.enabled", havingValue = "true", matchIfMissing = false)
public class OpenAIDirectClient implements LLMProvider {

	private static final Logger logger = LoggerFactory.getLogger(OpenAIDirectClient.class);

	private static final String PROVIDER_NAME = "openai";

	// OpenAI models available via direct API
	private static final List<String> SUPPORTED_MODELS = List.of("gpt-4o", "gpt-4o-mini", "gpt-4-turbo", "o1",
			"o1-mini");

	private final OpenAIDirectConfig config;

	private final ObjectMapper objectMapper;

	private final WebClient webClient;

	public OpenAIDirectClient(OpenAIDirectConfig config) {
		this.config = config;
		this.objectMapper = new ObjectMapper();
		this.webClient = WebClient.builder()
			.baseUrl(config.getApiUrl())
			.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
			.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApiKey())
			.build();

		logger.info("OpenAIDirectClient initialized with base URL: {}", config.getApiUrl());
	}

	@Override
	public Mono<LLMResponse> generateContent(String prompt, List<LLMMessage> history, String model) {
		logger.debug("Generating content with OpenAI, model: {}", model);

		if (!config.isEnabled()) {
			return Mono.just(LLMResponse.error("OpenAI Direct API is not enabled"));
		}

		if (config.getApiKey() == null || config.getApiKey().isEmpty()) {
			return Mono.just(LLMResponse.error("OpenAI API key is not configured"));
		}

		try {
			String apiModel = resolveModel(model);
			String requestBody = buildRequestBody(prompt, history, apiModel);

			return webClient.post()
				.uri("/v1/chat/completions")
				.bodyValue(requestBody)
				.retrieve()
				.onStatus(status -> status.isError(),
						response -> response.bodyToMono(String.class)
							.flatMap(errorBody -> Mono
								.error(new RuntimeException("OpenAI API error: " + response.statusCode() + " - " + errorBody))))
				.bodyToMono(String.class)
				.timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
				.map(responseBody -> parseResponse(responseBody, model))
				.doOnSuccess(response -> logger.debug("Received response from OpenAI API"))
				.doOnError(error -> logger.error("Error calling OpenAI API", error))
				.onErrorResume(error -> Mono.just(LLMResponse.error("Failed to call OpenAI: " + error.getMessage())));
		}
		catch (Exception e) {
			logger.error("Error preparing OpenAI request", e);
			return Mono.just(LLMResponse.error("Failed to prepare request: " + e.getMessage()));
		}
	}

	@Override
	public Flux<LLMResponse> generateContentStream(String prompt, List<LLMMessage> history, String model) {
		logger.debug("Generating streaming content with OpenAI, model: {}", model);

		if (!config.isEnabled()) {
			return Flux.just(LLMResponse.error("OpenAI Direct API is not enabled"));
		}

		try {
			String apiModel = resolveModel(model);
			String requestBody = buildStreamingRequestBody(prompt, history, apiModel);

			return webClient.post()
				.uri("/v1/chat/completions")
				.accept(MediaType.TEXT_EVENT_STREAM)
				.bodyValue(requestBody)
				.retrieve()
				.bodyToFlux(String.class)
				.timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
				.filter(chunk -> chunk != null && !chunk.isEmpty())
				.map(chunk -> parseStreamChunk(chunk, model))
				.filter(response -> response.getContent() != null && !response.getContent().isEmpty())
				.doOnError(error -> logger.error("Error in OpenAI streaming", error))
				.onErrorResume(error -> Flux.just(LLMResponse.error("Stream error: " + error.getMessage())));
		}
		catch (Exception e) {
			logger.error("Error preparing OpenAI streaming request", e);
			return Flux.just(LLMResponse.error("Failed to prepare streaming request: " + e.getMessage()));
		}
	}

	@Override
	public String getProviderName() {
		return PROVIDER_NAME;
	}

	@Override
	public List<String> getSupportedModels() {
		return SUPPORTED_MODELS;
	}

	/**
	 * Resolve model name
	 */
	private String resolveModel(String model) {
		if (model == null || model.isEmpty()) {
			return config.getDefaultModel();
		}
		return model;
	}

	/**
	 * Build the request body for OpenAI API
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
				messageNode.put("role", msg.getRole());
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
				messageNode.put("role", msg.getRole());
				messageNode.put("content", msg.getContent());
			}
		}

		ObjectNode promptMessage = messages.addObject();
		promptMessage.put("role", "user");
		promptMessage.put("content", prompt);

		return objectMapper.writeValueAsString(request);
	}

	/**
	 * Parse OpenAI API response
	 */
	private LLMResponse parseResponse(String responseBody, String model) {
		try {
			JsonNode root = objectMapper.readTree(responseBody);

			LLMResponse response = new LLMResponse();
			response.setModel(model);
			response.setProvider(PROVIDER_NAME);
			response.setSuccess(true);

			// Extract content from OpenAI response
			if (root.has("choices") && root.get("choices").isArray()) {
				JsonNode firstChoice = root.get("choices").get(0);
				if (firstChoice.has("message") && firstChoice.get("message").has("content")) {
					response.setContent(firstChoice.get("message").get("content").asText());
				}
			}

			// Extract usage info
			if (root.has("usage")) {
				JsonNode usage = root.get("usage");
				if (usage.has("prompt_tokens")) {
					response.setPromptTokens(usage.get("prompt_tokens").asInt());
				}
				if (usage.has("completion_tokens")) {
					response.setCompletionTokens(usage.get("completion_tokens").asInt());
				}
				if (usage.has("total_tokens")) {
					response.setTotalTokens(usage.get("total_tokens").asInt());
				}
			}

			return response;
		}
		catch (Exception e) {
			logger.error("Error parsing OpenAI response", e);
			return LLMResponse.error("Failed to parse response: " + e.getMessage());
		}
	}

	/**
	 * Parse streaming chunk
	 */
	private LLMResponse parseStreamChunk(String chunk, String model) {
		try {
			// SSE format: "data: {...}"
			String jsonData = chunk;
			if (chunk.startsWith("data: ")) {
				jsonData = chunk.substring(6);
			}

			if (jsonData.trim().equals("[DONE]")) {
				return new LLMResponse();
			}

			JsonNode root = objectMapper.readTree(jsonData);

			LLMResponse response = new LLMResponse();
			response.setModel(model);
			response.setProvider(PROVIDER_NAME);
			response.setSuccess(true);

			// Handle delta content
			if (root.has("choices") && root.get("choices").isArray()) {
				JsonNode firstChoice = root.get("choices").get(0);
				if (firstChoice.has("delta") && firstChoice.get("delta").has("content")) {
					response.setContent(firstChoice.get("delta").get("content").asText());
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
