package io.strategiz.service.labs.service;

import io.strategiz.data.strategy.entity.Strategy;
import io.strategiz.data.strategy.repository.CreateStrategyRepository;
import io.strategiz.data.strategy.repository.ReadStrategyRepository;
import io.strategiz.service.labs.exception.ServiceStrategyErrorDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import io.strategiz.service.base.BaseService;

/**
 * Service for managing strategy versions.
 * When a user edits a deployed strategy, a new version is created
 * while the original stays active with the existing deployment.
 */
@Service
public class StrategyVersioningService extends BaseService {

    @Override
    protected String getModuleName() {
        return "unknown";
    }
    private final ReadStrategyRepository readStrategyRepository;
    private final CreateStrategyRepository createStrategyRepository;

    @Autowired
    public StrategyVersioningService(
            ReadStrategyRepository readStrategyRepository,
            CreateStrategyRepository createStrategyRepository) {
        this.readStrategyRepository = readStrategyRepository;
        this.createStrategyRepository = createStrategyRepository;
    }

    /**
     * Create a new version of a deployed strategy for editing.
     * The original strategy remains deployed; a new draft version is created.
     *
     * @param strategyId The ID of the strategy to version
     * @param userId The user ID (for authorization)
     * @return The new draft strategy version
     * @throws IllegalArgumentException if strategy not found or not owned by user
     * @throws IllegalStateException if strategy is not deployed
     */
    public Strategy createVersion(String strategyId, String userId) {
        log.info("Creating new version of strategy {} for user {}", strategyId, userId);

        // Load the original strategy
        Optional<Strategy> originalOpt = readStrategyRepository.findById(strategyId);
        if (originalOpt.isEmpty()) {
            throwModuleException(ServiceStrategyErrorDetails.STRATEGY_NOT_FOUND,
                "Strategy not found: " + strategyId);
        }

        Strategy original = originalOpt.get();

        // Verify ownership
        if (!userId.equals(original.getOwnerId())) {
            throwModuleException(ServiceStrategyErrorDetails.STRATEGY_ACCESS_DENIED,
                "Access denied to strategy: " + strategyId);
        }

        // Verify the strategy is deployed
        if (!original.isDeployed()) {
            throwModuleException(ServiceStrategyErrorDetails.STRATEGY_VERSION_INVALID_STATE,
                "Cannot create version of non-deployed strategy. Use the update endpoint instead.");
        }

        // Determine the new version number
        long newVersion = (original.getVersion() != null ? original.getVersion() : 1L) + 1L;

        // Check if there are already newer versions
        List<Strategy> existingVersions = readStrategyRepository.findVersionsByParentId(
                original.getParentStrategyId() != null ? original.getParentStrategyId() : strategyId
        );
        if (!existingVersions.isEmpty()) {
            long maxVersion = existingVersions.stream()
                    .map(s -> s.getVersion() != null ? s.getVersion() : 1L)
                    .max(Long::compareTo)
                    .orElse(1L);
            newVersion = Math.max(newVersion, maxVersion + 1L);
        }

        // Create new strategy with copied fields
        Strategy newStrategy = new Strategy();
        newStrategy.setName(original.getName() + " (v" + newVersion + ")");
        newStrategy.setDescription(original.getDescription());
        newStrategy.setCode(original.getCode());
        newStrategy.setLanguage(original.getLanguage());
        newStrategy.setType(original.getType());
        newStrategy.setTags(original.getTags() != null ? List.copyOf(original.getTags()) : null);
        newStrategy.setParameters(original.getParameters());

        // Set versioning fields
        newStrategy.setVersion(newVersion);
        newStrategy.setParentStrategyId(original.getParentStrategyId() != null ? original.getParentStrategyId() : strategyId);

        // New version is a draft (not deployed)
        newStrategy.setIsPublished(false);
        newStrategy.setIsPublic(false);
        newStrategy.setIsListed(false);
        // Note: Deployment fields removed - tracked in BotDeployment/AlertDeployment entities

        // Save the new version
        Strategy created = createStrategyRepository.createWithUserId(newStrategy, userId);

        log.info("Created new version {} of strategy {} as {}", newVersion, strategyId, created.getId());
        return created;
    }

    /**
     * Get the version history of a strategy.
     *
     * @param strategyId The ID of any version in the strategy family
     * @param userId The user ID (for authorization)
     * @return List of all versions, sorted by version number descending
     */
    public List<Strategy> getVersionHistory(String strategyId, String userId) {
        log.info("Fetching version history for strategy {} for user {}", strategyId, userId);

        // Load the requested strategy to get the parent ID
        Optional<Strategy> strategyOpt = readStrategyRepository.findById(strategyId);
        if (strategyOpt.isEmpty()) {
            throwModuleException(ServiceStrategyErrorDetails.STRATEGY_NOT_FOUND,
                "Strategy not found: " + strategyId);
        }

        Strategy strategy = strategyOpt.get();

        // Verify ownership
        if (!userId.equals(strategy.getOwnerId())) {
            throwModuleException(ServiceStrategyErrorDetails.STRATEGY_ACCESS_DENIED,
                "Access denied to strategy: " + strategyId);
        }

        // Determine the root strategy ID
        String rootId = strategy.getParentStrategyId() != null ? strategy.getParentStrategyId() : strategyId;

        // Fetch all versions
        List<Strategy> versions = readStrategyRepository.findVersionsByParentId(rootId);

        // Also include the root strategy if it's not in the list
        Optional<Strategy> rootOpt = readStrategyRepository.findById(rootId);
        if (rootOpt.isPresent() && versions.stream().noneMatch(s -> rootId.equals(s.getId()))) {
            versions.add(rootOpt.get());
        }

        // Sort by version number descending (newest first)
        return versions.stream()
                .sorted(Comparator.comparingLong((Strategy s) -> s.getVersion() != null ? s.getVersion() : 1L).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Get the latest version of a strategy.
     *
     * @param strategyId The ID of any version in the strategy family
     * @param userId The user ID (for authorization)
     * @return The latest version
     */
    public Optional<Strategy> getLatestVersion(String strategyId, String userId) {
        List<Strategy> versions = getVersionHistory(strategyId, userId);
        return versions.isEmpty() ? Optional.empty() : Optional.of(versions.get(0));
    }
}
