package io.strategiz.client.mistral;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.auth.oauth2.GoogleCredentials;
import io.strategiz.client.base.llm.LLMProvider;
import io.strategiz.client.base.llm.model.LLMMessage;
import io.strategiz.client.base.llm.model.LLMResponse;
import io.strategiz.client.mistral.config.MistralVertexConfig;
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
import java.util.List;

/**
 * Client for Mistral AI via Google Vertex AI.
 * Implements LLMProvider interface for unified LLM access.
 */
@Component
@ConditionalOnProperty(name = "mistral.vertex.enabled", havingValue = "true", matchIfMissing = true)
public class MistralVertexClient implements LLMProvider {

	private static final Logger logger = LoggerFactory.getLogger(MistralVertexClient.class);

	private static final String PROVIDER_NAME = "mistral";

	// Mistral models available on Vertex AI
	private static final List<String> SUPPORTED_MODELS = List.of("mistral-large-2", "mistral-small", "mistral-nemo",
			"codestral");

	private final MistralVertexConfig config;

	private final ObjectMapper objectMapper;

	private final WebClient webClient;

	private GoogleCredentials credentials;

	public MistralVertexClient(MistralVertexConfig config) {
		this.config = config;
		this.objectMapper = new ObjectMapper();
		this.webClient = WebClient.builder().defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
			.build();

		// Initialize Google credentials once during construction
		try {
			this.credentials = GoogleCredentials.getApplicationDefault()
				.createScoped("https://www.googleapis.com/auth/cloud-platform");
			logger.info("Initialized Google Cloud credentials for Vertex AI (Mistral)");
		} catch (IOException e) {
			logger.error("Failed to initialize Google Cloud credentials. Mistral Vertex AI calls will fail.", e);
			this.credentials = null;
		}
	}

	@Override
	public Mono<LLMResponse> generateContent(String prompt, List<LLMMessage> history, String model) {
		logger.debug("Generating content with Mistral AI, model: {}", model);

		if (!config.isEnabled()) {
			return Mono.just(LLMResponse.error("Mistral Vertex AI is not enabled"));
		}

		if (config.getProjectId() == null || config.getProjectId().isEmpty()) {
			return Mono.just(LLMResponse.error("Mistral Vertex AI project ID is not configured"));
		}

		try {
			String accessToken = getAccessToken();
			String vertexModel = resolveModel(model);
			String requestBody = buildRequestBody(prompt, history);

			String url = buildEndpointUrl(vertexModel);

			return webClient.post()
				.uri(url)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
				.bodyValue(requestBody)
				.retrieve()
				.onStatus(status -> status.isError(),
						response -> response.bodyToMono(String.class)
							.flatMap(errorBody -> Mono.error(new RuntimeException(
									"Mistral API error: " + response.statusCode() + " - " + errorBody))))
				.bodyToMono(String.class)
				.map(responseBody -> parseResponse(responseBody, model))
				.doOnSuccess(response -> logger.debug("Received response from Mistral AI"))
				.doOnError(error -> logger.error("Error calling Mistral API", error))
				.onErrorResume(error -> Mono.just(LLMResponse.error("Failed to call Mistral: " + error.getMessage())));
		}
		catch (Exception e) {
			logger.error("Error preparing Mistral request", e);
			return Mono.just(LLMResponse.error("Failed to prepare request: " + e.getMessage()));
		}
	}

	@Override
	public Flux<LLMResponse> generateContentStream(String prompt, List<LLMMessage> history, String model) {
		logger.debug("Generating streaming content with Mistral AI, model: {}", model);

		if (!config.isEnabled()) {
			return Flux.just(LLMResponse.error("Mistral Vertex AI is not enabled"));
		}

		try {
			String accessToken = getAccessToken();
			String vertexModel = resolveModel(model);
			String requestBody = buildStreamingRequestBody(prompt, history);

			String url = buildEndpointUrl(vertexModel);

			return webClient.post()
				.uri(url)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
				.accept(MediaType.TEXT_EVENT_STREAM)
				.bodyValue(requestBody)
				.retrieve()
				.bodyToFlux(String.class)
				.filter(chunk -> chunk != null && !chunk.isEmpty())
				.map(chunk -> parseStreamChunk(chunk, model))
				.filter(response -> response.getContent() != null && !response.getContent().isEmpty())
				.doOnError(error -> logger.error("Error in Mistral streaming", error))
				.onErrorResume(error -> Flux.just(LLMResponse.error("Stream error: " + error.getMessage())));
		}
		catch (Exception e) {
			logger.error("Error preparing Mistral streaming request", e);
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
	 * Build the Vertex AI endpoint URL for Mistral (uses OpenAI-compatible endpoint)
	 */
	private String buildEndpointUrl(String model) {
		return String.format(
				"https://%s-aiplatform.googleapis.com/v1/projects/%s/locations/%s/endpoints/openapi/chat/completions?model=%s",
				config.getLocation(), config.getProjectId(), config.getLocation(), model);
	}

	/**
	 * Build the request body for Mistral API (OpenAI-compatible format)
	 */
	private String buildRequestBody(String prompt, List<LLMMessage> history) throws JsonProcessingException {
		ObjectNode request = objectMapper.createObjectNode();

		// Build messages array (OpenAI format)
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

		// Add generation parameters
		request.put("temperature", config.getTemperature());
		request.put("max_tokens", config.getMaxTokens());

		return objectMapper.writeValueAsString(request);
	}

	/**
	 * Build streaming request body (same as non-streaming for Mistral)
	 */
	private String buildStreamingRequestBody(String prompt, List<LLMMessage> history)
			throws JsonProcessingException {
		ObjectNode request = (ObjectNode) objectMapper.readTree(buildRequestBody(prompt, history));
		request.put("stream", true);
		return objectMapper.writeValueAsString(request);
	}

	/**
	 * Parse Mistral API response (OpenAI-compatible format)
	 */
	private LLMResponse parseResponse(String responseBody, String model) {
		try {
			JsonNode root = objectMapper.readTree(responseBody);

			LLMResponse response = new LLMResponse();
			response.setModel(model);
			response.setProvider(PROVIDER_NAME);
			response.setSuccess(true);

			// Extract content from OpenAI-compatible response
			if (root.has("choices") && root.get("choices").isArray() && root.get("choices").size() > 0) {
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
			logger.error("Error parsing Mistral response", e);
			return LLMResponse.error("Failed to parse response: " + e.getMessage());
		}
	}

	/**
	 * Parse streaming chunk (OpenAI-compatible SSE format)
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

			// Handle streaming content (OpenAI format)
			if (root.has("choices") && root.get("choices").isArray() && root.get("choices").size() > 0) {
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

	/**
	 * Get access token for Vertex AI API calls.
	 * GoogleCredentials handles automatic token refresh and caching.
	 */
	private String getAccessToken() throws IOException {
		if (credentials == null) {
			throw new IOException("Google Cloud credentials not initialized");
		}

		// GoogleCredentials automatically refreshes expired tokens
		credentials.refreshIfExpired();

		if (credentials.getAccessToken() == null) {
			throw new IOException("Failed to get access token from Google Cloud credentials");
		}

		return credentials.getAccessToken().getTokenValue();
	}

}
