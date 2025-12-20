package io.strategiz.client.base.llm;

import io.strategiz.client.base.llm.model.LLMMessage;
import io.strategiz.client.base.llm.model.LLMResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Interface for Language Model providers (Gemini, Claude, etc.)
 * Provides a unified API for generating content across different LLM backends.
 */
public interface LLMProvider {

	/**
	 * Generate content from a prompt with conversation history
	 * @param prompt the user prompt
	 * @param history previous messages in the conversation
	 * @param model the specific model to use (e.g., "gemini-1.5-flash", "claude-3-5-sonnet")
	 * @return LLMResponse containing the generated content
	 */
	Mono<LLMResponse> generateContent(String prompt, List<LLMMessage> history, String model);

	/**
	 * Generate content with streaming support
	 * @param prompt the user prompt
	 * @param history previous messages in the conversation
	 * @param model the specific model to use
	 * @return Flux of LLMResponse chunks
	 */
	Flux<LLMResponse> generateContentStream(String prompt, List<LLMMessage> history, String model);

	/**
	 * Get the provider name (e.g., "google", "anthropic")
	 */
	String getProviderName();

	/**
	 * Get list of supported model IDs for this provider
	 */
	List<String> getSupportedModels();

	/**
	 * Check if this provider supports a specific model
	 */
	default boolean supportsModel(String model) {
		return getSupportedModels().contains(model);
	}

}
