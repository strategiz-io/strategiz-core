package io.strategiz.data.preferences.repository;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import io.strategiz.data.base.repository.BaseRepository;
import io.strategiz.data.preferences.entity.AiModelConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Repository for AiModelConfig stored at config/ai-models/{modelId}.
 *
 * <p>This repository manages configurable AI model costs and tier requirements.</p>
 */
@Repository
public class AiModelConfigRepository extends BaseRepository<AiModelConfig> {

	private static final Logger logger = LoggerFactory.getLogger(AiModelConfigRepository.class);

	// In-memory cache for model configs
	private final Map<String, AiModelConfig> cache = new ConcurrentHashMap<>();

	private volatile long cacheTimestamp = 0;

	private static final long CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutes

	public AiModelConfigRepository(Firestore firestore) {
		super(firestore, AiModelConfig.class);
	}

	@Override
	protected String getModuleName() {
		return "data-preferences";
	}

	/**
	 * Get model config by ID.
	 *
	 * @param modelId The model ID
	 * @return The model config if found
	 */
	public Optional<AiModelConfig> getByModelId(String modelId) {
		// Check cache first
		if (isCacheValid() && cache.containsKey(modelId)) {
			return Optional.of(cache.get(modelId));
		}

		Optional<AiModelConfig> existing = findById(modelId);
		existing.ifPresent(model -> cache.put(modelId, model));
		return existing;
	}

	/**
	 * Get all enabled AI models, sorted by provider and sortOrder.
	 *
	 * @return List of enabled models
	 */
	public List<AiModelConfig> getEnabledModels() {
		return getAllModels().stream()
			.filter(model -> Boolean.TRUE.equals(model.getEnabled()))
			.sorted(Comparator.comparing(AiModelConfig::getProvider)
				.thenComparing(m -> m.getSortOrder() != null ? m.getSortOrder() : 999))
			.collect(Collectors.toList());
	}

	/**
	 * Get all enabled models for a specific tier.
	 *
	 * @param tierLevel The tier level (0=explorer, 1=strategist, 2=quant)
	 * @return List of models accessible to this tier
	 */
	public List<AiModelConfig> getModelsForTier(int tierLevel) {
		return getEnabledModels().stream().filter(model -> model.getMinTierLevel() <= tierLevel).toList();
	}

	/**
	 * Get all models by provider.
	 *
	 * @param provider The provider name (openai, anthropic, google, etc.)
	 * @return List of models from that provider
	 */
	public List<AiModelConfig> getByProvider(String provider) {
		return getEnabledModels().stream()
			.filter(model -> provider.equals(model.getProvider()))
			.collect(Collectors.toList());
	}

	/**
	 * Get all AI models (including disabled).
	 *
	 * @return List of all models
	 */
	public List<AiModelConfig> getAllModels() {
		if (isCacheValid() && !cache.isEmpty()) {
			return List.copyOf(cache.values());
		}

		try {
			QuerySnapshot snapshot = getCollection().get().get();
			List<AiModelConfig> configs = snapshot.getDocuments()
				.stream()
				.map(doc -> doc.toObject(AiModelConfig.class))
				.collect(Collectors.toList());

			// If no models exist, seed with defaults
			if (configs.isEmpty()) {
				logger.info("No AI models found, returning defaults");
				configs = AiModelConfig.createDefaults();
			}

			// Update cache
			configs.forEach(c -> cache.put(c.getModelId(), c));
			cacheTimestamp = System.currentTimeMillis();

			return configs;
		}
		catch (Exception e) {
			logger.error("Failed to get all AI models", e);
			return AiModelConfig.createDefaults();
		}
	}

	/**
	 * Get STRAT cost for a model and token usage.
	 *
	 * @param modelId The model ID
	 * @param promptTokens Number of prompt tokens
	 * @param completionTokens Number of completion tokens
	 * @return The STRAT cost (0 if model not found)
	 */
	public int getStratCost(String modelId, int promptTokens, int completionTokens) {
		return getByModelId(modelId).map(model -> model.calculateStratCost(promptTokens, completionTokens)).orElse(0);
	}

	/**
	 * Save model config.
	 *
	 * @param config The config to save
	 * @param updatedBy The user making the change
	 * @return The saved config
	 */
	public AiModelConfig saveConfig(AiModelConfig config, String updatedBy) {
		AiModelConfig saved = save(config, updatedBy);
		cache.put(saved.getModelId(), saved);
		return saved;
	}

	/**
	 * Seed default models if none exist.
	 *
	 * @param userId The admin user seeding the models
	 */
	public void seedDefaultsIfEmpty(String userId) {
		try {
			QuerySnapshot snapshot = getCollection().limit(1).get().get();
			if (snapshot.isEmpty()) {
				logger.info("Seeding default AI models");
				for (AiModelConfig model : AiModelConfig.createDefaults()) {
					save(model, userId);
				}
				clearCache();
			}
		}
		catch (Exception e) {
			logger.error("Failed to seed default AI models", e);
		}
	}

	/**
	 * Clear the cache.
	 */
	public void clearCache() {
		cache.clear();
		cacheTimestamp = 0;
	}

	private boolean isCacheValid() {
		return System.currentTimeMillis() - cacheTimestamp < CACHE_TTL_MS;
	}

}
