package io.strategiz.client.openai;

import io.strategiz.client.base.llm.LLMProvider;
import io.strategiz.client.base.llm.model.LLMMessage;
import io.strategiz.client.base.llm.model.LLMResponse;
import io.strategiz.client.openai.config.OpenAIConfig;
import io.strategiz.client.openai.model.OpenAIRequest;
import io.strategiz.client.openai.model.OpenAIResponse;
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
 * Client for interacting with OpenAI Chat Completions API. Implements LLMProvider for
 * unified LLM access.
 *
 * Authentication: Uses API key authentication (Authorization: Bearer <api-key>)
 *
 * Note: OpenAI currently uses API key authentication. OAuth 2.1 support can be added
 * in the future if OpenAI provides an OAuth flow.
 */
@Component
public class OpenAIClient implements LLMProvider {

	private static final Logger logger = LoggerFactory.getLogger(OpenAIClient.class);

	private static final String PROVIDER_NAME = "openai";

	private static final List<String> SUPPORTED_MODELS = List.of("gpt-4o", "gpt-4o-mini", "o1", "o1-mini");

	private final OpenAIConfig config;

	private final WebClient webClient;

	public OpenAIClient(OpenAIConfig config) {
		this.config = config;

		// Build WebClient with Authorization header
		this.webClient = WebClient.builder()
			.baseUrl(config.getApiUrl())
			.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
			.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApiKey())
			.build();
	}

	// ========================
	// LLMProvider Interface Implementation
	// ========================

	@Override
	public Mono<LLMResponse> generateContent(String prompt, List<LLMMessage> history, String model) {
		logger.debug("Generating content with OpenAI, model: {}", model);

		// Convert LLMMessage to OpenAI messages
		List<OpenAIRequest.Message> messages = convertToOpenAIMessages(history, prompt);

		// Determine which model to use
		String targetModel = (model != null && !model.isEmpty()) ? model : config.getModel();

		return generateContentWithModel(messages, targetModel, false).map(openAIResponse -> {
			LLMResponse response = new LLMResponse();

			// Extract content from first choice
			if (openAIResponse.getChoices() != null && !openAIResponse.getChoices().isEmpty()) {
				OpenAIResponse.Choice firstChoice = openAIResponse.getChoices().get(0);
				if (firstChoice.getMessage() != null) {
					response.setContent(firstChoice.getMessage().getContent());
				}
			}

			response.setModel(targetModel);
			response.setProvider(PROVIDER_NAME);
			response.setSuccess(true);

			// Add usage metadata
			if (openAIResponse.getUsage() != null) {
				response.setPromptTokens(openAIResponse.getUsage().getPromptTokens());
				response.setCompletionTokens(openAIResponse.getUsage().getCompletionTokens());
				response.setTotalTokens(openAIResponse.getUsage().getTotalTokens());
			}

			return response;
		}).onErrorResume(error -> {
			logger.error("Error generating content with OpenAI", error);
			return Mono.just(LLMResponse.error("Failed to generate content: " + error.getMessage()));
		});
	}

	@Override
	public Flux<LLMResponse> generateContentStream(String prompt, List<LLMMessage> history, String model) {
		logger.debug("Generating streaming content with OpenAI, model: {}", model);

		List<OpenAIRequest.Message> messages = convertToOpenAIMessages(history, prompt);
		String targetModel = (model != null && !model.isEmpty()) ? model : config.getModel();

		return generateContentStreamWithModel(messages, targetModel).map(openAIResponse -> {
			LLMResponse response = new LLMResponse();

			// Extract content from delta in streaming response
			if (openAIResponse.getChoices() != null && !openAIResponse.getChoices().isEmpty()) {
				OpenAIResponse.Choice firstChoice = openAIResponse.getChoices().get(0);
				if (firstChoice.getDelta() != null && firstChoice.getDelta().getContent() != null) {
					response.setContent(firstChoice.getDelta().getContent());
				}
			}

			response.setModel(targetModel);
			response.setProvider(PROVIDER_NAME);
			response.setSuccess(true);

			return response;
		}).onErrorResume(error -> {
			logger.error("Error in OpenAI streaming", error);
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
	// Internal Methods
	// ========================

	private Mono<OpenAIResponse> generateContentWithModel(List<OpenAIRequest.Message> messages, String model,
			boolean stream) {
		logger.debug("Calling OpenAI API with model: {}, stream: {}", model, stream);

		if (config.getApiKey() == null || config.getApiKey().isEmpty()) {
			logger.error("OpenAI API key is not configured");
			return Mono.error(new IllegalStateException("OpenAI API key is not configured"));
		}

		// Build request
		OpenAIRequest request = new OpenAIRequest(model, messages, config.getTemperature(), config.getMaxTokens(),
				stream);

		// Make API call
		String url = "/chat/completions";

		return webClient.post()
			.uri(url)
			.bodyValue(request)
			.retrieve()
			.onStatus(status -> status.isError(),
					response -> response.bodyToMono(String.class)
						.flatMap(errorBody -> Mono
							.error(new RuntimeException("OpenAI API error: " + response.statusCode() + " - " + errorBody))))
			.bodyToMono(OpenAIResponse.class)
			.doOnSuccess(response -> {
				String preview = "";
				if (response.getChoices() != null && !response.getChoices().isEmpty()
						&& response.getChoices().get(0).getMessage() != null) {
					String content = response.getChoices().get(0).getMessage().getContent();
					preview = content != null ? content.substring(0, Math.min(100, content.length())) : "empty";
				}
				logger.debug("Received response from OpenAI: {}", preview);
			})
			.doOnError(error -> logger.error("Error calling OpenAI API", error));
	}

	private Flux<OpenAIResponse> generateContentStreamWithModel(List<OpenAIRequest.Message> messages, String model) {
		logger.debug("Calling OpenAI streaming API with model: {}", model);

		if (config.getApiKey() == null || config.getApiKey().isEmpty()) {
			logger.error("OpenAI API key is not configured");
			return Flux.error(new IllegalStateException("OpenAI API key is not configured"));
		}

		// Build request with stream=true
		OpenAIRequest request = new OpenAIRequest(model, messages, config.getTemperature(), config.getMaxTokens(),
				true);

		// Make streaming API call
		String url = "/chat/completions";

		return webClient.post()
			.uri(url)
			.bodyValue(request)
			.accept(MediaType.TEXT_EVENT_STREAM)
			.retrieve()
			.bodyToFlux(OpenAIResponse.class)
			.doOnNext(response -> logger.debug("Received stream chunk from OpenAI"))
			.doOnError(error -> logger.error("Error in OpenAI API stream", error));
	}

	/**
	 * Convert LLMMessage list to OpenAI message list. Adds conversation history plus the
	 * new user prompt.
	 */
	private List<OpenAIRequest.Message> convertToOpenAIMessages(List<LLMMessage> history, String prompt) {
		List<OpenAIRequest.Message> messages = new ArrayList<>();

		// Add conversation history
		if (history != null && !history.isEmpty()) {
			for (LLMMessage msg : history) {
				messages.add(new OpenAIRequest.Message(msg.getRole(), msg.getContent()));
			}
		}

		// Add new user prompt
		messages.add(new OpenAIRequest.Message("user", prompt));

		return messages;
	}

}
