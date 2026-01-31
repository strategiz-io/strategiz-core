package io.strategiz.data.preferences.repository;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import io.strategiz.data.base.repository.BaseRepository;
import io.strategiz.data.preferences.entity.TierConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Repository for TierConfig stored at config/tiers/{tierId}.
 *
 * <p>
 * This repository manages configurable tier settings. Tier configs are cached in memory
 * for performance since they change infrequently.
 * </p>
 */
@Repository
public class TierConfigRepository extends BaseRepository<TierConfig> {

	private static final Logger logger = LoggerFactory.getLogger(TierConfigRepository.class);

	// In-memory cache for tier configs (they change rarely)
	private final Map<String, TierConfig> cache = new ConcurrentHashMap<>();

	private volatile long cacheTimestamp = 0;

	private static final long CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutes

	public TierConfigRepository(Firestore firestore) {
		super(firestore, TierConfig.class);
	}

	@Override
	protected String getModuleName() {
		return "data-preferences";
	}

	/**
	 * Get tier config by ID. Returns default if not found in database.
	 * @param tierId The tier ID (explorer, strategist, quant)
	 * @return The tier config
	 */
	public TierConfig getByTierId(String tierId) {
		// Check cache first
		if (isCacheValid() && cache.containsKey(tierId)) {
			return cache.get(tierId);
		}

		Optional<TierConfig> existing = findById(tierId);

		if (existing.isPresent()) {
			cache.put(tierId, existing.get());
			return existing.get();
		}

		// Return default config based on tier ID
		logger.info("No tier config found for {}, returning defaults", tierId);
		TierConfig defaults = getDefaultConfig(tierId);
		cache.put(tierId, defaults);
		return defaults;
	}

	/**
	 * Get all tier configs. Returns defaults for any missing tiers.
	 * @return List of all tier configs
	 */
	public List<TierConfig> getAllTiers() {
		if (isCacheValid() && cache.size() >= 3) {
			return List.copyOf(cache.values());
		}

		try {
			QuerySnapshot snapshot = getCollection().get().get();
			List<TierConfig> configs = snapshot.getDocuments()
				.stream()
				.map(doc -> doc.toObject(TierConfig.class))
				.collect(Collectors.toList());

			// Ensure all default tiers exist
			boolean hasExplorer = configs.stream().anyMatch(c -> TierConfig.TIER_EXPLORER.equals(c.getTierId()));
			boolean hasStrategist = configs.stream().anyMatch(c -> TierConfig.TIER_STRATEGIST.equals(c.getTierId()));
			boolean hasQuant = configs.stream().anyMatch(c -> TierConfig.TIER_QUANT.equals(c.getTierId()));

			if (!hasExplorer) {
				configs.add(TierConfig.createExplorerDefault());
			}
			if (!hasStrategist) {
				configs.add(TierConfig.createStrategistDefault());
			}
			if (!hasQuant) {
				configs.add(TierConfig.createQuantDefault());
			}

			// Update cache
			configs.forEach(c -> cache.put(c.getTierId(), c));
			cacheTimestamp = System.currentTimeMillis();

			return configs;
		}
		catch (Exception e) {
			logger.error("Failed to get all tier configs", e);
			// Return defaults
			return List.of(TierConfig.createExplorerDefault(), TierConfig.createStrategistDefault(),
					TierConfig.createQuantDefault());
		}
	}

	/**
	 * Save tier config.
	 * @param config The config to save
	 * @param updatedBy The user making the change
	 * @return The saved config
	 */
	public TierConfig saveConfig(TierConfig config, String updatedBy) {
		TierConfig saved = save(config, updatedBy);
		cache.put(saved.getTierId(), saved);
		return saved;
	}

	/**
	 * Clear the cache (useful after admin updates).
	 */
	public void clearCache() {
		cache.clear();
		cacheTimestamp = 0;
	}

	private boolean isCacheValid() {
		return System.currentTimeMillis() - cacheTimestamp < CACHE_TTL_MS;
	}

	private TierConfig getDefaultConfig(String tierId) {
		return switch (tierId) {
			case TierConfig.TIER_EXPLORER -> TierConfig.createExplorerDefault();
			case TierConfig.TIER_STRATEGIST -> TierConfig.createStrategistDefault();
			case TierConfig.TIER_QUANT -> TierConfig.createQuantDefault();
			default -> TierConfig.createExplorerDefault();
		};
	}

}
