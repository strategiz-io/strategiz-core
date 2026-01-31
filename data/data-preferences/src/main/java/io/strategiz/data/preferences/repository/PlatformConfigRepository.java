package io.strategiz.data.preferences.repository;

import com.google.cloud.firestore.Firestore;
import io.strategiz.data.base.repository.BaseRepository;
import io.strategiz.data.preferences.entity.PlatformConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for PlatformConfig stored at config/platform/current.
 *
 * <p>
 * This is a singleton document - there's only one platform config at any time.
 * </p>
 */
@Repository
public class PlatformConfigRepository extends BaseRepository<PlatformConfig> {

	private static final Logger logger = LoggerFactory.getLogger(PlatformConfigRepository.class);

	public PlatformConfigRepository(Firestore firestore) {
		super(firestore, PlatformConfig.class);
	}

	@Override
	protected String getModuleName() {
		return "data-preferences";
	}

	/**
	 * Get the current platform configuration. Returns default config if none exists.
	 * @return The current platform config
	 */
	public PlatformConfig getCurrent() {
		Optional<PlatformConfig> existing = findById(PlatformConfig.CONFIG_ID);

		if (existing.isPresent()) {
			return existing.get();
		}

		// Return default config (not persisted until explicitly saved)
		logger.info("No platform config found, returning defaults");
		PlatformConfig defaults = new PlatformConfig();
		defaults.setId(PlatformConfig.CONFIG_ID);
		return defaults;
	}

	/**
	 * Save the platform configuration.
	 * @param config The config to save
	 * @param updatedBy The user making the change
	 * @return The saved config
	 */
	public PlatformConfig saveConfig(PlatformConfig config, String updatedBy) {
		config.setId(PlatformConfig.CONFIG_ID);
		config.setUpdatedAt(java.time.Instant.now());
		config.setUpdatedBy(updatedBy);

		try {
			getCollection().document(config.getId()).set(config).get();
			logger.info("Saved PlatformConfig by user {}", updatedBy);
			return config;
		}
		catch (Exception e) {
			throw new io.strategiz.data.base.exception.DataRepositoryException(
					io.strategiz.data.base.exception.DataRepositoryErrorDetails.ENTITY_SAVE_FAILED, getModuleName(), e,
					"PlatformConfig", config.getId());
		}
	}

}
