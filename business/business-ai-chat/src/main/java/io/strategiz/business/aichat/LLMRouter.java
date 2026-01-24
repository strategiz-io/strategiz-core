package io.strategiz.business.aichat;

import io.strategiz.client.base.llm.LLMProvider;
import io.strategiz.client.base.llm.model.LLMMessage;
import io.strategiz.client.base.llm.model.LLMResponse;
import io.strategiz.client.base.llm.model.ModelInfo;
import io.strategiz.data.featureflags.service.FeatureFlagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Routes LLM requests to the appropriate provider based on model selection. Provides a
 * unified interface for generating content across multiple LLM providers.
 */
@Service
public class LLMRouter {

	private static final Logger logger = LoggerFactory.getLogger(LLMRouter.class);

	private static final String DEFAULT_MODEL = "gemini-2.5-flash";

	private final Map<String, LLMProvider> providersByModel;

	private final List<LLMProvider> providers;

	private final FeatureFlagService featureFlagService;

	public LLMRouter(List<LLMProvider> providers, FeatureFlagService featureFlagService) {
		this.providers = providers != null ? providers : new ArrayList<>();
		this.featureFlagService = featureFlagService;
		this.providersByModel = buildModelProviderMap();
		logger.info("LLMRouter initialized with {} providers: {}", this.providers.size(),
				this.providers.stream().map(LLMProvider::getProviderName).toList());
	}

	/**
	 * Generate content using the specified model
	 * @param prompt the user prompt
	 * @param history conversation history
	 * @param model the model to use (e.g., "gemini-1.5-flash", "claude-3-5-sonnet")
	 * @return LLMResponse containing the generated content
	 */
	public Mono<LLMResponse> generateContent(String prompt, List<LLMMessage> history, String model) {
		String targetModel = resolveModel(model);

		// Check if model is enabled via feature flags (provider + model level)
		if (!featureFlagService.isAIModelEnabled(targetModel)) {
			logger.warn("Model {} is disabled via feature flags", targetModel);
			return Mono.just(LLMResponse.error("Model " + targetModel + " is currently unavailable. Please select a different model."));
		}

		LLMProvider provider = getProviderForModel(targetModel);

		if (provider == null) {
			logger.warn("No provider found for model: {}, falling back to default", targetModel);
			return Mono.just(LLMResponse.error("No provider available for model: " + targetModel));
		}

		logger.debug("Routing request to {} provider for model {}", provider.getProviderName(), targetModel);
		return provider.generateContent(prompt, history, targetModel);
	}

	/**
	 * Generate content with streaming using the specified model
	 * @param prompt the user prompt
	 * @param history conversation history
	 * @param model the model to use
	 * @return Flux of LLMResponse chunks
	 */
	public Flux<LLMResponse> generateContentStream(String prompt, List<LLMMessage> history, String model) {
		String targetModel = resolveModel(model);

		// Check if model is enabled via feature flags (provider + model level)
		if (!featureFlagService.isAIModelEnabled(targetModel)) {
			logger.warn("Model {} is disabled via feature flags", targetModel);
			return Flux.just(LLMResponse.error("Model " + targetModel + " is currently unavailable. Please select a different model."));
		}

		LLMProvider provider = getProviderForModel(targetModel);

		if (provider == null) {
			logger.warn("No provider found for model: {}", targetModel);
			return Flux.just(LLMResponse.error("No provider available for model: " + targetModel));
		}

		logger.debug("Routing streaming request to {} provider for model {}", provider.getProviderName(), targetModel);
		return provider.generateContentStream(prompt, history, targetModel);
	}

	/**
	 * Get all available models from all providers
	 * @return list of ModelInfo objects
	 */
	public List<ModelInfo> getAvailableModels() {
		List<ModelInfo> models = new ArrayList<>();

		// Gemini models (GA stable versions)
		models.add(new ModelInfo("gemini-2.5-flash", "Gemini 2.5 Flash", "google",
				"Fast and capable, balanced performance"));
		models.add(new ModelInfo("gemini-2.5-pro", "Gemini 2.5 Pro", "google",
				"High-capability model for complex reasoning"));
		models.add(new ModelInfo("gemini-1.5-pro", "Gemini 1.5 Pro", "google",
				"Stable production model"));
		models.add(new ModelInfo("gemini-1.5-flash", "Gemini 1.5 Flash", "google",
				"Fast and efficient"));

		// OpenAI models (via Vertex AI Model Garden)
		// NOTE: Only gpt-4o-mini is available in us-east1 via Vertex AI
		models.add(new ModelInfo("gpt-4o-mini", "GPT-4o Mini", "openai", "Fast & affordable"));

		// Claude 4.5 models (latest, via direct Anthropic API)
		models.add(new ModelInfo("claude-haiku-4-5", "Claude Haiku 4.5", "anthropic", "Fast & affordable"));
		models.add(new ModelInfo("claude-sonnet-4-5", "Claude Sonnet 4.5", "anthropic", "Best balanced model"));
		models.add(new ModelInfo("claude-opus-4-5", "Claude Opus 4.5", "anthropic",
				"Most capable for complex tasks"));

		// Claude 3.5 models (previous generation, great for lower tiers)
		models.add(new ModelInfo("claude-3-5-haiku", "Claude 3.5 Haiku", "anthropic", "Fast & very affordable"));
		models.add(new ModelInfo("claude-3-5-sonnet", "Claude 3.5 Sonnet", "anthropic", "Excellent balanced model"));

		// Claude 3 models (older generation, budget-friendly)
		models.add(new ModelInfo("claude-3-haiku", "Claude 3 Haiku", "anthropic", "Budget-friendly, fast"));
		models.add(new ModelInfo("claude-3-sonnet", "Claude 3 Sonnet", "anthropic", "Budget-friendly, balanced"));
		models.add(new ModelInfo("claude-3-opus", "Claude 3 Opus", "anthropic", "Previous flagship model"));

		// Llama models (Meta)
		models.add(new ModelInfo("llama-3.1-8b-instruct-maas", "Llama 3.1 8B", "meta", "Fast & efficient"));
		models.add(new ModelInfo("llama-3.1-70b-instruct-maas", "Llama 3.1 70B", "meta", "Balanced Llama model"));
		models.add(new ModelInfo("llama-3.1-405b-instruct-maas", "Llama 3.1 405B", "meta",
				"Largest Llama, best reasoning"));

		// Mistral models
		models.add(new ModelInfo("mistral-nemo", "Mistral Nemo", "mistral", "Fast & affordable"));
		models.add(new ModelInfo("mistral-small", "Mistral Small", "mistral", "Balanced performance"));
		models.add(new ModelInfo("mistral-large-2", "Mistral Large 2", "mistral", "Flagship Mistral model"));

		// Cohere models
		models.add(new ModelInfo("command-r", "Command R", "cohere", "Balanced Cohere model"));
		models.add(new ModelInfo("command-r-plus", "Command R+", "cohere", "Best Cohere model"));

		// xAI Grok models (via direct API)
		models.add(new ModelInfo("grok-3-mini", "Grok 3 Mini", "xai", "Fast & economical"));
		models.add(new ModelInfo("grok-3", "Grok 3", "xai", "General purpose model"));
		models.add(new ModelInfo("grok-4", "Grok 4", "xai", "Advanced reasoning & coding"));
		models.add(new ModelInfo("grok-4.1-fast", "Grok 4.1 Fast", "xai", "Best for tool-calling, 2M context"));

		// Mark models as available based on registered providers AND feature flags
		for (ModelInfo model : models) {
			boolean providerRegistered = providersByModel.containsKey(model.getId());
			boolean flagEnabled = featureFlagService.isAIModelEnabled(model.getId());
			model.setAvailable(providerRegistered && flagEnabled);
		}

		return models;
	}

	/**
	 * Get the default model
	 */
	public String getDefaultModel() {
		return DEFAULT_MODEL;
	}

	/**
	 * Check if a model is available
	 */
	public boolean isModelAvailable(String model) {
		return providersByModel.containsKey(model);
	}

	/**
	 * Get provider for a specific model
	 */
	private LLMProvider getProviderForModel(String model) {
		return providersByModel.get(model);
	}

	/**
	 * Resolve model name (handle null/empty, apply defaults)
	 */
	private String resolveModel(String model) {
		if (model == null || model.isEmpty()) {
			return DEFAULT_MODEL;
		}
		return model;
	}

	/**
	 * Build a map of model -> provider for quick lookups
	 */
	private Map<String, LLMProvider> buildModelProviderMap() {
		Map<String, LLMProvider> map = new HashMap<>();
		for (LLMProvider provider : providers) {
			for (String model : provider.getSupportedModels()) {
				map.put(model, provider);
				logger.debug("Registered model {} with provider {}", model, provider.getProviderName());
			}
		}
		return map;
	}

}
