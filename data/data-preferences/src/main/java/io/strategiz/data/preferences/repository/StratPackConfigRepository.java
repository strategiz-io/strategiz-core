package io.strategiz.data.preferences.repository;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import io.strategiz.data.base.repository.BaseRepository;
import io.strategiz.data.preferences.entity.StratPackConfig;
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
 * Repository for StratPackConfig stored at config/strat-packs/{packId}.
 *
 * <p>
 * This repository manages configurable STRAT pack definitions.
 * </p>
 */
@Repository
public class StratPackConfigRepository extends BaseRepository<StratPackConfig> {

	private static final Logger logger = LoggerFactory.getLogger(StratPackConfigRepository.class);

	// In-memory cache for pack configs
	private final Map<String, StratPackConfig> cache = new ConcurrentHashMap<>();

	private volatile long cacheTimestamp = 0;

	private static final long CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutes

	public StratPackConfigRepository(Firestore firestore) {
		super(firestore, StratPackConfig.class);
	}

	@Override
	protected String getModuleName() {
		return "data-preferences";
	}

	/**
	 * Get pack config by ID.
	 * @param packId The pack ID
	 * @return The pack config if found
	 */
	public Optional<StratPackConfig> getByPackId(String packId) {
		// Check cache first
		if (isCacheValid() && cache.containsKey(packId)) {
			return Optional.of(cache.get(packId));
		}

		Optional<StratPackConfig> existing = findById(packId);
		existing.ifPresent(pack -> cache.put(packId, pack));
		return existing;
	}

	/**
	 * Get all enabled STRAT packs, sorted by sortOrder.
	 * @return List of enabled packs
	 */
	public List<StratPackConfig> getEnabledPacks() {
		return getAllPacks().stream()
			.filter(pack -> Boolean.TRUE.equals(pack.getEnabled()))
			.sorted(Comparator.comparingInt(p -> p.getSortOrder() != null ? p.getSortOrder() : 999))
			.collect(Collectors.toList());
	}

	/**
	 * Get all STRAT packs (including disabled).
	 * @return List of all packs
	 */
	public List<StratPackConfig> getAllPacks() {
		if (isCacheValid() && !cache.isEmpty()) {
			return List.copyOf(cache.values());
		}

		try {
			QuerySnapshot snapshot = getCollection().get().get();
			List<StratPackConfig> configs = snapshot.getDocuments()
				.stream()
				.map(doc -> doc.toObject(StratPackConfig.class))
				.collect(Collectors.toList());

			// If no packs exist, seed with defaults
			if (configs.isEmpty()) {
				logger.info("No STRAT packs found, returning defaults");
				configs = StratPackConfig.createDefaults();
			}

			// Update cache
			configs.forEach(c -> cache.put(c.getPackId(), c));
			cacheTimestamp = System.currentTimeMillis();

			return configs;
		}
		catch (Exception e) {
			logger.error("Failed to get all STRAT packs", e);
			return StratPackConfig.createDefaults();
		}
	}

	/**
	 * Save pack config.
	 * @param config The config to save
	 * @param updatedBy The user making the change
	 * @return The saved config
	 */
	public StratPackConfig saveConfig(StratPackConfig config, String updatedBy) {
		StratPackConfig saved = save(config, updatedBy);
		cache.put(saved.getPackId(), saved);
		return saved;
	}

	/**
	 * Seed default packs if none exist.
	 * @param userId The admin user seeding the packs
	 */
	public void seedDefaultsIfEmpty(String userId) {
		try {
			QuerySnapshot snapshot = getCollection().limit(1).get().get();
			if (snapshot.isEmpty()) {
				logger.info("Seeding default STRAT packs");
				for (StratPackConfig pack : StratPackConfig.createDefaults()) {
					save(pack, userId);
				}
				clearCache();
			}
		}
		catch (Exception e) {
			logger.error("Failed to seed default STRAT packs", e);
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
