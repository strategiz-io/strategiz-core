package io.strategiz.data.featureflags.service;

import io.strategiz.data.featureflags.entity.FeatureFlagEntity;
import io.strategiz.data.featureflags.repository.FeatureFlagRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing feature flags.
 * Provides caching for fast flag lookups and methods for CRUD operations.
 */
@Service
public class FeatureFlagService {

    private static final Logger log = LoggerFactory.getLogger(FeatureFlagService.class);

    // Cache for fast lookups (refreshed periodically or on update)
    private final Map<String, Boolean> flagCache = new ConcurrentHashMap<>();

    private final FeatureFlagRepository repository;

    // Well-known feature flag IDs
    public static final String FLAG_PLAID_ENABLED = "plaid_enabled";
    public static final String FLAG_ROBINHOOD_ENABLED = "robinhood_enabled";
    public static final String FLAG_AI_CHAT_ENABLED = "ai_chat_enabled";
    public static final String FLAG_TRADING_ENABLED = "trading_enabled";

    @Autowired
    public FeatureFlagService(FeatureFlagRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    public void initializeDefaultFlags() {
        log.info("Initializing default feature flags...");

        // Create default flags if they don't exist
        createDefaultFlag(FLAG_PLAID_ENABLED, "Plaid Integration",
            "Enable Plaid Link for connecting brokerage accounts", false, "providers");

        createDefaultFlag(FLAG_ROBINHOOD_ENABLED, "Robinhood Integration",
            "Enable direct Robinhood credential-based integration", true, "providers");

        createDefaultFlag(FLAG_AI_CHAT_ENABLED, "AI Chat",
            "Enable AI chat features", true, "ai");

        createDefaultFlag(FLAG_TRADING_ENABLED, "Live Trading",
            "Enable live trading (paper trading always available)", false, "trading");

        // Refresh cache after initialization
        refreshCache();
    }

    private void createDefaultFlag(String flagId, String name, String description,
                                    boolean defaultEnabled, String category) {
        if (repository.findById(flagId).isEmpty()) {
            FeatureFlagEntity flag = new FeatureFlagEntity(flagId, name, description, defaultEnabled, category);
            repository.save(flag);
            log.info("Created default feature flag: {} = {}", flagId, defaultEnabled);
        }
    }

    /**
     * Refresh the flag cache from the database.
     */
    public void refreshCache() {
        log.info("Refreshing feature flag cache...");
        flagCache.clear();
        List<FeatureFlagEntity> flags = repository.findAll();
        for (FeatureFlagEntity flag : flags) {
            flagCache.put(flag.getFlagId(), flag.isEnabled());
        }
        log.info("Feature flag cache refreshed with {} flags", flagCache.size());
    }

    /**
     * Check if a feature is enabled (uses cache for performance).
     */
    public boolean isEnabled(String flagId) {
        // Check cache first
        Boolean cached = flagCache.get(flagId);
        if (cached != null) {
            return cached;
        }

        // Fall back to database
        boolean enabled = repository.isEnabled(flagId);
        flagCache.put(flagId, enabled);
        return enabled;
    }

    /**
     * Check if Plaid integration is enabled.
     */
    public boolean isPlaidEnabled() {
        return isEnabled(FLAG_PLAID_ENABLED);
    }

    /**
     * Check if Robinhood integration is enabled.
     */
    public boolean isRobinhoodEnabled() {
        return isEnabled(FLAG_ROBINHOOD_ENABLED);
    }

    /**
     * Get all feature flags.
     */
    public List<FeatureFlagEntity> getAllFlags() {
        return repository.findAll();
    }

    /**
     * Get feature flags by category.
     */
    public List<FeatureFlagEntity> getFlagsByCategory(String category) {
        return repository.findByCategory(category);
    }

    /**
     * Get a specific feature flag.
     */
    public Optional<FeatureFlagEntity> getFlag(String flagId) {
        return repository.findById(flagId);
    }

    /**
     * Enable a feature flag.
     */
    public FeatureFlagEntity enableFlag(String flagId) {
        return setFlagEnabled(flagId, true);
    }

    /**
     * Disable a feature flag.
     */
    public FeatureFlagEntity disableFlag(String flagId) {
        return setFlagEnabled(flagId, false);
    }

    /**
     * Set a feature flag's enabled state.
     */
    public FeatureFlagEntity setFlagEnabled(String flagId, boolean enabled) {
        Optional<FeatureFlagEntity> optFlag = repository.findById(flagId);
        if (optFlag.isEmpty()) {
            throw new IllegalArgumentException("Feature flag not found: " + flagId);
        }

        FeatureFlagEntity flag = optFlag.get();
        flag.setEnabled(enabled);
        FeatureFlagEntity saved = repository.save(flag);

        // Update cache
        flagCache.put(flagId, enabled);
        log.info("Feature flag {} set to {}", flagId, enabled);

        return saved;
    }

    /**
     * Create a new feature flag.
     */
    public FeatureFlagEntity createFlag(FeatureFlagEntity flag) {
        if (repository.findById(flag.getFlagId()).isPresent()) {
            throw new IllegalArgumentException("Feature flag already exists: " + flag.getFlagId());
        }

        FeatureFlagEntity saved = repository.save(flag);
        flagCache.put(flag.getFlagId(), flag.isEnabled());
        return saved;
    }

    /**
     * Update a feature flag.
     */
    public FeatureFlagEntity updateFlag(FeatureFlagEntity flag) {
        if (repository.findById(flag.getFlagId()).isEmpty()) {
            throw new IllegalArgumentException("Feature flag not found: " + flag.getFlagId());
        }

        FeatureFlagEntity saved = repository.save(flag);
        flagCache.put(flag.getFlagId(), flag.isEnabled());
        return saved;
    }

    /**
     * Delete a feature flag.
     */
    public void deleteFlag(String flagId) {
        repository.delete(flagId);
        flagCache.remove(flagId);
        log.info("Feature flag deleted: {}", flagId);
    }
}
