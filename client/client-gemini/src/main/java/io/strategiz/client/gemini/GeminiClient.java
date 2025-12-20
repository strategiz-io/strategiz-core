package io.strategiz.client.gemini;

import io.strategiz.client.base.llm.LLMProvider;
import io.strategiz.client.base.llm.model.LLMMessage;
import io.strategiz.client.base.llm.model.LLMResponse;
import io.strategiz.client.gemini.config.GeminiConfig;
import io.strategiz.client.gemini.model.GeminiRequest;
import io.strategiz.client.gemini.model.GeminiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * Client for interacting with Google Gemini AI API. Implements LLMProvider for unified
 * LLM access.
 */
@Component
public class GeminiClient implements LLMProvider {

	private static final Logger logger = LoggerFactory.getLogger(GeminiClient.class);

	private static final String PROVIDER_NAME = "google";

	private static final List<String> SUPPORTED_MODELS = List.of("gemini-1.5-flash", "gemini-1.5-pro", "gemini-2.0-flash-exp");

	private final GeminiConfig config;

	private final WebClient webClient;

	public GeminiClient(GeminiConfig config) {
		this.config = config;
		this.webClient = WebClient.builder().baseUrl(config.getApiUrl())
			.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).build();
	}

	// ========================
	// LLMProvider Interface Implementation
	// ========================

	@Override
	public Mono<LLMResponse> generateContent(String prompt, List<LLMMessage> history, String model) {
		logger.debug("Generating content with Gemini AI, model: {}", model);

		// Convert LLMMessage to GeminiRequest.Content
		List<GeminiRequest.Content> geminiHistory = convertToGeminiHistory(history);

		// Determine which model to use
		String targetModel = (model != null && !model.isEmpty()) ? model : config.getModel();

		return generateContentWithModel(prompt, geminiHistory, targetModel).map(geminiResponse -> {
			LLMResponse response = new LLMResponse();
			response.setContent(geminiResponse.getText());
			response.setModel(targetModel);
			response.setProvider(PROVIDER_NAME);
			response.setSuccess(true);

			if (geminiResponse.getUsageMetadata() != null) {
				response.setPromptTokens(geminiResponse.getUsageMetadata().getPromptTokenCount());
				response.setCompletionTokens(geminiResponse.getUsageMetadata().getCandidatesTokenCount());
				response.setTotalTokens(geminiResponse.getUsageMetadata().getTotalTokenCount());
			}

			return response;
		}).onErrorResume(error -> {
			logger.error("Error generating content with Gemini", error);
			return Mono.just(LLMResponse.error("Failed to generate content: " + error.getMessage()));
		});
	}

	@Override
	public Flux<LLMResponse> generateContentStream(String prompt, List<LLMMessage> history, String model) {
		logger.debug("Generating streaming content with Gemini AI, model: {}", model);

		List<GeminiRequest.Content> geminiHistory = convertToGeminiHistory(history);
		String targetModel = (model != null && !model.isEmpty()) ? model : config.getModel();

		return generateContentStreamWithModel(prompt, geminiHistory, targetModel).map(geminiResponse -> {
			LLMResponse response = new LLMResponse();
			response.setContent(geminiResponse.getText());
			response.setModel(targetModel);
			response.setProvider(PROVIDER_NAME);
			response.setSuccess(true);
			return response;
		}).onErrorResume(error -> {
			logger.error("Error in Gemini streaming", error);
			return Flux.just(LLMResponse.error("Stream error: " + error.getMessage()));
		});
	}

	@Override
	public String getProviderName() {
		return PROVIDER_NAME;
	}

	@Override
	public List<String> getSupportedModels() {
		return SUPPORTED_MODELS;
	}

	// ========================
	// Legacy Methods (for backward compatibility)
	// ========================

	/**
	 * Generate content from a single prompt
	 * @param prompt the user prompt
	 * @return GeminiResponse containing the generated content
	 */
	public Mono<GeminiResponse> generateContent(String prompt) {
		return generateContent(prompt, null);
	}

	/**
	 * Generate content with conversation history
	 * @param prompt the user prompt
	 * @param conversationHistory previous messages in the conversation
	 * @return GeminiResponse containing the generated content
	 */
	public Mono<GeminiResponse> generateContent(String prompt, List<GeminiRequest.Content> conversationHistory) {
		return generateContentWithModel(prompt, conversationHistory, config.getModel());
	}

	/**
	 * Generate content with streaming support
	 * @param prompt the user prompt
	 * @param conversationHistory previous messages in the conversation
	 * @return Flux of GeminiResponse chunks
	 */
	public Flux<GeminiResponse> generateContentStream(String prompt, List<GeminiRequest.Content> conversationHistory) {
		return generateContentStreamWithModel(prompt, conversationHistory, config.getModel());
	}

	// ========================
	// Internal Methods
	// ========================

	private Mono<GeminiResponse> generateContentWithModel(String prompt, List<GeminiRequest.Content> conversationHistory,
			String model) {
		logger.debug("Generating content with Gemini AI for prompt: {}", prompt);

		if (config.getApiKey() == null || config.getApiKey().isEmpty()) {
			logger.error("Gemini API key is not configured");
			return Mono.error(new IllegalStateException("Gemini API key is not configured"));
		}

		// Build contents list with conversation history + new prompt
		List<GeminiRequest.Content> contents = new ArrayList<>();
		if (conversationHistory != null && !conversationHistory.isEmpty()) {
			contents.addAll(conversationHistory);
		}
		contents.add(new GeminiRequest.Content("user", prompt));

		// Build request
		GeminiRequest request = new GeminiRequest(contents);
		request.setGenerationConfig(new GeminiRequest.GenerationConfig(config.getTemperature(), config.getMaxTokens()));

		// Make API call
		String url = String.format("/v1beta/models/%s:generateContent?key=%s", model, config.getApiKey());

		return webClient.post().uri(url).bodyValue(request).retrieve()
			.onStatus(status -> status.isError(),
					response -> response.bodyToMono(String.class)
						.flatMap(errorBody -> Mono
							.error(new RuntimeException("Gemini API error: " + response.statusCode() + " - " + errorBody))))
			.bodyToMono(GeminiResponse.class)
			.doOnSuccess(response -> logger.debug("Received response from Gemini AI: {}",
					response.getText() != null
							? response.getText().substring(0, Math.min(100, response.getText().length())) : "empty"))
			.doOnError(error -> logger.error("Error calling Gemini API", error));
	}

	private Flux<GeminiResponse> generateContentStreamWithModel(String prompt,
			List<GeminiRequest.Content> conversationHistory, String model) {
		logger.debug("Generating streaming content with Gemini AI for prompt: {}", prompt);

		if (config.getApiKey() == null || config.getApiKey().isEmpty()) {
			logger.error("Gemini API key is not configured");
			return Flux.error(new IllegalStateException("Gemini API key is not configured"));
		}

		// Build contents list
		List<GeminiRequest.Content> contents = new ArrayList<>();
		if (conversationHistory != null && !conversationHistory.isEmpty()) {
			contents.addAll(conversationHistory);
		}
		contents.add(new GeminiRequest.Content("user", prompt));

		// Build request
		GeminiRequest request = new GeminiRequest(contents);
		request.setGenerationConfig(new GeminiRequest.GenerationConfig(config.getTemperature(), config.getMaxTokens()));

		// Make streaming API call
		String url = String.format("/v1beta/models/%s:streamGenerateContent?key=%s&alt=sse", model, config.getApiKey());

		return webClient.post().uri(url).bodyValue(request).accept(MediaType.TEXT_EVENT_STREAM).retrieve()
			.bodyToFlux(GeminiResponse.class)
			.doOnNext(response -> logger.debug("Received stream chunk from Gemini AI"))
			.doOnError(error -> logger.error("Error in Gemini API stream", error));
	}

	/**
	 * Convert LLMMessage list to GeminiRequest.Content list
	 */
	private List<GeminiRequest.Content> convertToGeminiHistory(List<LLMMessage> history) {
		if (history == null || history.isEmpty()) {
			return new ArrayList<>();
		}

		List<GeminiRequest.Content> geminiHistory = new ArrayList<>();
		for (LLMMessage msg : history) {
			// Gemini uses "user" and "model" roles
			String role = "assistant".equals(msg.getRole()) ? "model" : "user";
			geminiHistory.add(new GeminiRequest.Content(role, msg.getContent()));
		}
		return geminiHistory;
	}

}
