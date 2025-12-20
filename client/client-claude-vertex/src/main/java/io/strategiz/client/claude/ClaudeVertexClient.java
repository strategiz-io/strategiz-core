package io.strategiz.client.claude;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.auth.oauth2.GoogleCredentials;
import io.strategiz.client.base.llm.LLMProvider;
import io.strategiz.client.base.llm.model.LLMMessage;
import io.strategiz.client.base.llm.model.LLMResponse;
import io.strategiz.client.claude.config.ClaudeVertexConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Client for Claude AI via Google Vertex AI.
 * Implements LLMProvider interface for unified LLM access.
 */
@Component
@ConditionalOnProperty(name = "claude.vertex.enabled", havingValue = "true", matchIfMissing = true)
public class ClaudeVertexClient implements LLMProvider {

	private static final Logger logger = LoggerFactory.getLogger(ClaudeVertexClient.class);

	private static final String PROVIDER_NAME = "anthropic";

	// Claude models available on Vertex AI (latest 2025 models)
	private static final List<String> SUPPORTED_MODELS = List.of("claude-opus-4-5", "claude-sonnet-4", "claude-haiku-4-5",
			"claude-opus-4-1", "claude-opus-4");

	// User-friendly model IDs that map to Vertex AI model names
	private static final List<String> USER_MODEL_IDS = List.of("claude-opus-4-5", "claude-sonnet-4", "claude-haiku-4-5");

	private final ClaudeVertexConfig config;

	private final ObjectMapper objectMapper;

	private final WebClient webClient;

	private String cachedAccessToken;

	private long tokenExpirationTime;

	public ClaudeVertexClient(ClaudeVertexConfig config) {
		this.config = config;
		this.objectMapper = new ObjectMapper();
		this.webClient = WebClient.builder().defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
			.build();
	}

	@Override
	public Mono<LLMResponse> generateContent(String prompt, List<LLMMessage> history, String model) {
		logger.debug("Generating content with Claude AI, model: {}", model);

		if (!config.isEnabled()) {
			return Mono.just(LLMResponse.error("Claude Vertex AI is not enabled"));
		}

		if (config.getProjectId() == null || config.getProjectId().isEmpty()) {
			return Mono.just(LLMResponse.error("Claude Vertex AI project ID is not configured"));
		}

		try {
			String accessToken = getAccessToken();
			String vertexModel = mapToVertexModel(model);
			String requestBody = buildRequestBody(prompt, history);

			String url = buildEndpointUrl(vertexModel);

			return webClient.post().uri(url).header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
				.bodyValue(requestBody).retrieve()
				.onStatus(status -> status.isError(),
						response -> response.bodyToMono(String.class)
							.flatMap(errorBody -> Mono
								.error(new RuntimeException("Claude API error: " + response.statusCode() + " - " + errorBody))))
				.bodyToMono(String.class).map(responseBody -> parseResponse(responseBody, model))
				.doOnSuccess(response -> logger.debug("Received response from Claude AI"))
				.doOnError(error -> logger.error("Error calling Claude API", error))
				.onErrorResume(error -> Mono.just(LLMResponse.error("Failed to call Claude: " + error.getMessage())));
		}
		catch (Exception e) {
			logger.error("Error preparing Claude request", e);
			return Mono.just(LLMResponse.error("Failed to prepare request: " + e.getMessage()));
		}
	}

	@Override
	public Flux<LLMResponse> generateContentStream(String prompt, List<LLMMessage> history, String model) {
		logger.debug("Generating streaming content with Claude AI, model: {}", model);

		if (!config.isEnabled()) {
			return Flux.just(LLMResponse.error("Claude Vertex AI is not enabled"));
		}

		try {
			String accessToken = getAccessToken();
			String vertexModel = mapToVertexModel(model);
			String requestBody = buildStreamingRequestBody(prompt, history);

			String url = buildStreamingEndpointUrl(vertexModel);

			return webClient.post().uri(url).header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
				.accept(MediaType.TEXT_EVENT_STREAM).bodyValue(requestBody).retrieve().bodyToFlux(String.class)
				.filter(chunk -> chunk != null && !chunk.isEmpty()).map(chunk -> parseStreamChunk(chunk, model))
				.filter(response -> response.getContent() != null && !response.getContent().isEmpty())
				.doOnError(error -> logger.error("Error in Claude streaming", error))
				.onErrorResume(error -> Flux.just(LLMResponse.error("Stream error: " + error.getMessage())));
		}
		catch (Exception e) {
			logger.error("Error preparing Claude streaming request", e);
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
	 * Map user-friendly model ID to Vertex AI model name
	 */
	private String mapToVertexModel(String model) {
		if (model == null || model.isEmpty()) {
			return config.getDefaultModel();
		}

		// Claude 4.x models use the same name on Vertex AI (no @date suffix)
		if (model.startsWith("claude-opus-4") || model.startsWith("claude-sonnet-4")
				|| model.startsWith("claude-haiku-4")) {
			return model;
		}

		// Legacy Claude 3.x models with @date suffix
		return switch (model.toLowerCase()) {
			case "claude-3-5-sonnet" -> "claude-3-5-sonnet@20241022";
			case "claude-3-opus" -> "claude-3-opus@20240229";
			case "claude-3-haiku" -> "claude-3-haiku@20240307";
			default -> config.getDefaultModel();
		};
	}

	/**
	 * Build the Vertex AI endpoint URL for Claude
	 */
	private String buildEndpointUrl(String model) {
		return String.format(
				"https://%s-aiplatform.googleapis.com/v1/projects/%s/locations/%s/publishers/anthropic/models/%s:rawPredict",
				config.getLocation(), config.getProjectId(), config.getLocation(), model);
	}

	/**
	 * Build the streaming endpoint URL
	 */
	private String buildStreamingEndpointUrl(String model) {
		return String.format(
				"https://%s-aiplatform.googleapis.com/v1/projects/%s/locations/%s/publishers/anthropic/models/%s:streamRawPredict",
				config.getLocation(), config.getProjectId(), config.getLocation(), model);
	}

	/**
	 * Build the request body for Claude API
	 */
	private String buildRequestBody(String prompt, List<LLMMessage> history) throws JsonProcessingException {
		ObjectNode request = objectMapper.createObjectNode();
		request.put("anthropic_version", "vertex-2023-10-16");
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
	private String buildStreamingRequestBody(String prompt, List<LLMMessage> history) throws JsonProcessingException {
		ObjectNode request = objectMapper.createObjectNode();
		request.put("anthropic_version", "vertex-2023-10-16");
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
	 * Parse Claude API response
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
			logger.error("Error parsing Claude response", e);
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
			if (root.has("delta") && root.get("delta").has("text")) {
				response.setContent(root.get("delta").get("text").asText());
			}

			return response;
		}
		catch (Exception e) {
			logger.debug("Error parsing stream chunk: {}", chunk);
			return new LLMResponse();
		}
	}

	/**
	 * Get access token for Vertex AI API calls
	 */
	private String getAccessToken() throws IOException {
		// Return cached token if still valid
		if (cachedAccessToken != null && System.currentTimeMillis() < tokenExpirationTime) {
			return cachedAccessToken;
		}

		// Get new access token using Application Default Credentials
		GoogleCredentials credentials = GoogleCredentials.getApplicationDefault()
			.createScoped("https://www.googleapis.com/auth/cloud-platform");
		credentials.refreshIfExpired();

		cachedAccessToken = credentials.getAccessToken().getTokenValue();
		// Token expires in 1 hour, refresh 5 minutes early
		tokenExpirationTime = System.currentTimeMillis() + (55 * 60 * 1000);

		return cachedAccessToken;
	}

}
