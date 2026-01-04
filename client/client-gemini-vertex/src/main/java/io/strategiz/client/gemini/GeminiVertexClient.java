package io.strategiz.client.gemini;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.auth.oauth2.GoogleCredentials;
import io.strategiz.client.base.llm.LLMProvider;
import io.strategiz.client.base.llm.model.LLMMessage;
import io.strategiz.client.base.llm.model.LLMResponse;
import io.strategiz.client.gemini.config.GeminiVertexConfig;
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
 * Client for Google Gemini via Google Vertex AI.
 * Implements LLMProvider interface for unified LLM access.
 */
@Component
@ConditionalOnProperty(name = "gemini.vertex.enabled", havingValue = "true", matchIfMissing = true)
public class GeminiVertexClient implements LLMProvider {

	private static final Logger logger = LoggerFactory.getLogger(GeminiVertexClient.class);

	private static final String PROVIDER_NAME = "google";

	// Gemini models available on Vertex AI (GA stable versions only)
	private static final List<String> SUPPORTED_MODELS = List.of("gemini-2.5-flash", "gemini-2.5-pro",
			"gemini-1.5-pro", "gemini-1.5-flash");

	private final GeminiVertexConfig config;

	private final ObjectMapper objectMapper;

	private final WebClient webClient;

	private GoogleCredentials credentials;

	public GeminiVertexClient(GeminiVertexConfig config) {
		this.config = config;
		this.objectMapper = new ObjectMapper();
		this.webClient = WebClient.builder().defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
			.build();

		// Initialize Google credentials once during construction
		try {
			this.credentials = GoogleCredentials.getApplicationDefault()
				.createScoped("https://www.googleapis.com/auth/cloud-platform");
			logger.info("Initialized Google Cloud credentials for Vertex AI");
		} catch (IOException e) {
			logger.error("Failed to initialize Google Cloud credentials. Vertex AI calls will fail.", e);
			this.credentials = null;
		}
	}

	@Override
	public Mono<LLMResponse> generateContent(String prompt, List<LLMMessage> history, String model) {
		logger.debug("Generating content with Gemini AI, model: {}", model);

		if (!config.isEnabled()) {
			return Mono.just(LLMResponse.error("Gemini Vertex AI is not enabled"));
		}

		if (config.getProjectId() == null || config.getProjectId().isEmpty()) {
			return Mono.just(LLMResponse.error("Gemini Vertex AI project ID is not configured"));
		}

		try {
			String accessToken = getAccessToken();
			String vertexModel = resolveModel(model);
			String requestBody = buildRequestBody(prompt, history);

			String url = buildEndpointUrl(vertexModel);

			return webClient.post().uri(url).header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
				.bodyValue(requestBody).retrieve()
				.onStatus(status -> status.isError(),
						response -> response.bodyToMono(String.class)
							.flatMap(errorBody -> Mono
								.error(new RuntimeException("Gemini API error: " + response.statusCode() + " - " + errorBody))))
				.bodyToMono(String.class).map(responseBody -> parseResponse(responseBody, model))
				.doOnSuccess(response -> logger.debug("Received response from Gemini AI"))
				.doOnError(error -> logger.error("Error calling Gemini API", error))
				.onErrorResume(error -> Mono.just(LLMResponse.error("Failed to call Gemini: " + error.getMessage())));
		}
		catch (Exception e) {
			logger.error("Error preparing Gemini request", e);
			return Mono.just(LLMResponse.error("Failed to prepare request: " + e.getMessage()));
		}
	}

	@Override
	public Flux<LLMResponse> generateContentStream(String prompt, List<LLMMessage> history, String model) {
		logger.debug("Generating streaming content with Gemini AI, model: {}", model);

		if (!config.isEnabled()) {
			return Flux.just(LLMResponse.error("Gemini Vertex AI is not enabled"));
		}

		try {
			String accessToken = getAccessToken();
			String vertexModel = resolveModel(model);
			String requestBody = buildStreamingRequestBody(prompt, history);

			String url = buildStreamingEndpointUrl(vertexModel);

			return webClient.post().uri(url).header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
				.accept(MediaType.TEXT_EVENT_STREAM).bodyValue(requestBody).retrieve().bodyToFlux(String.class)
				.filter(chunk -> chunk != null && !chunk.isEmpty()).map(chunk -> parseStreamChunk(chunk, model))
				.filter(response -> response.getContent() != null && !response.getContent().isEmpty())
				.doOnError(error -> logger.error("Error in Gemini streaming", error))
				.onErrorResume(error -> Flux.just(LLMResponse.error("Stream error: " + error.getMessage())));
		}
		catch (Exception e) {
			logger.error("Error preparing Gemini streaming request", e);
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
	 * Build the Vertex AI endpoint URL for Gemini
	 */
	private String buildEndpointUrl(String model) {
		return String.format(
				"https://%s-aiplatform.googleapis.com/v1/projects/%s/locations/%s/publishers/google/models/%s:generateContent",
				config.getLocation(), config.getProjectId(), config.getLocation(), model);
	}

	/**
	 * Build the streaming endpoint URL
	 */
	private String buildStreamingEndpointUrl(String model) {
		return String.format(
				"https://%s-aiplatform.googleapis.com/v1/projects/%s/locations/%s/publishers/google/models/%s:streamGenerateContent",
				config.getLocation(), config.getProjectId(), config.getLocation(), model);
	}

	/**
	 * Build the request body for Gemini API
	 */
	private String buildRequestBody(String prompt, List<LLMMessage> history) throws JsonProcessingException {
		ObjectNode request = objectMapper.createObjectNode();

		// Build contents array
		ArrayNode contents = request.putArray("contents");

		// Add conversation history
		if (history != null) {
			for (LLMMessage msg : history) {
				ObjectNode contentNode = contents.addObject();
				// Gemini uses "user" and "model" roles
				String role = "assistant".equals(msg.getRole()) ? "model" : "user";
				contentNode.put("role", role);

				ArrayNode parts = contentNode.putArray("parts");
				ObjectNode part = parts.addObject();
				part.put("text", msg.getContent());
			}
		}

		// Add current prompt as user message
		ObjectNode promptContent = contents.addObject();
		promptContent.put("role", "user");
		ArrayNode promptParts = promptContent.putArray("parts");
		ObjectNode promptPart = promptParts.addObject();
		promptPart.put("text", prompt);

		// Add generation config
		ObjectNode generationConfig = request.putObject("generationConfig");
		generationConfig.put("temperature", config.getTemperature());
		generationConfig.put("maxOutputTokens", config.getMaxTokens());

		return objectMapper.writeValueAsString(request);
	}

	/**
	 * Build streaming request body
	 */
	private String buildStreamingRequestBody(String prompt, List<LLMMessage> history) throws JsonProcessingException {
		// Same as non-streaming for Gemini
		return buildRequestBody(prompt, history);
	}

	/**
	 * Parse Gemini API response
	 */
	private LLMResponse parseResponse(String responseBody, String model) {
		try {
			JsonNode root = objectMapper.readTree(responseBody);

			LLMResponse response = new LLMResponse();
			response.setModel(model);
			response.setProvider(PROVIDER_NAME);
			response.setSuccess(true);

			// Extract content from Gemini response
			if (root.has("candidates") && root.get("candidates").isArray()) {
				JsonNode firstCandidate = root.get("candidates").get(0);
				if (firstCandidate.has("content") && firstCandidate.get("content").has("parts")) {
					StringBuilder content = new StringBuilder();
					for (JsonNode part : firstCandidate.get("content").get("parts")) {
						if (part.has("text")) {
							content.append(part.get("text").asText());
						}
					}
					response.setContent(content.toString());
				}
			}

			// Extract usage info
			if (root.has("usageMetadata")) {
				JsonNode usage = root.get("usageMetadata");
				if (usage.has("promptTokenCount")) {
					response.setPromptTokens(usage.get("promptTokenCount").asInt());
				}
				if (usage.has("candidatesTokenCount")) {
					response.setCompletionTokens(usage.get("candidatesTokenCount").asInt());
				}
				if (usage.has("totalTokenCount")) {
					response.setTotalTokens(usage.get("totalTokenCount").asInt());
				}
			}

			return response;
		}
		catch (Exception e) {
			logger.error("Error parsing Gemini response", e);
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

			// Handle streaming content
			if (root.has("candidates") && root.get("candidates").isArray()) {
				JsonNode firstCandidate = root.get("candidates").get(0);
				if (firstCandidate.has("content") && firstCandidate.get("content").has("parts")) {
					StringBuilder content = new StringBuilder();
					for (JsonNode part : firstCandidate.get("content").get("parts")) {
						if (part.has("text")) {
							content.append(part.get("text").asText());
						}
					}
					response.setContent(content.toString());
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
		// No need for manual caching or expiration tracking
		credentials.refreshIfExpired();

		if (credentials.getAccessToken() == null) {
			throw new IOException("Failed to get access token from Google Cloud credentials");
		}

		String token = credentials.getAccessToken().getTokenValue();
		logger.debug("Retrieved Google Cloud access token (auto-refreshed if expired)");
		return token;
	}

}
