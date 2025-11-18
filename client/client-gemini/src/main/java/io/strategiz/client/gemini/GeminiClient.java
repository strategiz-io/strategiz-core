package io.strategiz.client.gemini;

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
 * Client for interacting with Google Gemini AI API
 */
@Component
public class GeminiClient {

	private static final Logger logger = LoggerFactory.getLogger(GeminiClient.class);

	private final GeminiConfig config;

	private final WebClient webClient;

	public GeminiClient(GeminiConfig config) {
		this.config = config;
		this.webClient = WebClient.builder().baseUrl(config.getApiUrl())
			.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).build();
	}

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
		String url = String.format("/v1beta/models/%s:generateContent?key=%s", config.getModel(),
				config.getApiKey());

		return webClient.post()
			.uri(url)
			.bodyValue(request)
			.retrieve()
			.onStatus(status -> status.isError(),
					response -> response.bodyToMono(String.class)
						.flatMap(errorBody -> Mono.error(new RuntimeException(
								"Gemini API error: " + response.statusCode() + " - " + errorBody))))
			.bodyToMono(GeminiResponse.class)
			.doOnSuccess(response -> logger.debug("Received response from Gemini AI: {}",
					response.getText() != null ? response.getText().substring(0, Math.min(100, response.getText().length()))
							: "empty"))
			.doOnError(error -> logger.error("Error calling Gemini API", error));
	}

	/**
	 * Generate content with streaming support
	 * @param prompt the user prompt
	 * @param conversationHistory previous messages in the conversation
	 * @return Flux of GeminiResponse chunks
	 */
	public Flux<GeminiResponse> generateContentStream(String prompt, List<GeminiRequest.Content> conversationHistory) {
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
		String url = String.format("/v1beta/models/%s:streamGenerateContent?key=%s&alt=sse", config.getModel(),
				config.getApiKey());

		return webClient.post()
			.uri(url)
			.bodyValue(request)
			.accept(MediaType.TEXT_EVENT_STREAM)
			.retrieve()
			.bodyToFlux(GeminiResponse.class)
			.doOnNext(response -> logger.debug("Received stream chunk from Gemini AI"))
			.doOnError(error -> logger.error("Error in Gemini API stream", error));
	}

}
