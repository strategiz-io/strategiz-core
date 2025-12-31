package io.strategiz.data.strategy.repository;

import io.strategiz.data.base.exception.DataRepositoryErrorDetails;
import io.strategiz.data.base.exception.DataRepositoryException;
import io.strategiz.data.strategy.entity.Strategy;
import io.strategiz.data.strategy.entity.StrategyPerformance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation of UpdateStrategyRepository using BaseRepository
 */
@Repository
public class UpdateStrategyRepositoryImpl implements UpdateStrategyRepository {
    
    private final StrategyBaseRepository baseRepository;
    
    @Autowired
    public UpdateStrategyRepositoryImpl(StrategyBaseRepository baseRepository) {
        this.baseRepository = baseRepository;
    }
    
    @Override
    public Strategy update(String id, String userId, Strategy strategy) {
        // Verify ownership (strategies are stored in user subcollections, so userId scoping is enforced)
        Optional<Strategy> existing = baseRepository.findById(id);
        if (existing.isEmpty() || !Boolean.TRUE.equals(existing.get().getIsActive())) {
            throw new DataRepositoryException(DataRepositoryErrorDetails.ENTITY_NOT_FOUND_OR_UNAUTHORIZED, "Strategy", id);
        }

        // Ensure ID is set
        strategy.setId(id);

        return baseRepository.save(strategy, userId);
    }
    
    @Override
    public boolean updateStatus(String id, String userId, String status) {
        Optional<Strategy> existing = baseRepository.findById(id);
        if (existing.isEmpty() || !Boolean.TRUE.equals(existing.get().getIsActive())) {
            return false;
        }

        Strategy strategy = existing.get();
        strategy.setPublishStatus(status);
        baseRepository.save(strategy, userId);
        return true;
    }
    
    // For now, implement basic field update methods - these can be enhanced later
    @Override
    public Optional<Strategy> updateCode(String id, String userId, String code) {
        return updateField(id, userId, strategy -> strategy.setCode(code));
    }
    
    @Override
    public Optional<Strategy> updateTags(String id, String userId, List<String> tags) {
        return updateField(id, userId, strategy -> strategy.setTags(tags));
    }
    
    @Override
    public Optional<Strategy> updatePerformance(String id, String userId, StrategyPerformance performance) {
        return updateField(id, userId, strategy -> strategy.setPerformance(performance));
    }
    
    @Override
    public Optional<Strategy> updateBacktestResults(String id, String userId, Map<String, Object> backtestResults) {
        return updateField(id, userId, strategy -> strategy.setBacktestResults(backtestResults));
    }
    
    @Override
    public Optional<Strategy> updateVisibility(String id, String userId, boolean isPublic) {
        // Convert boolean to publicStatus string: true → PUBLIC, false → PRIVATE
        return updateField(id, userId, strategy -> strategy.setPublicStatus(isPublic ? "PUBLIC" : "PRIVATE"));
    }
    
    @Override
    public Optional<Strategy> updateParameters(String id, String userId, Map<String, Object> parameters) {
        return updateField(id, userId, strategy -> strategy.setParameters(parameters));
    }
    
    @Override
    public Optional<Strategy> updateDescription(String id, String userId, String description) {
        return updateField(id, userId, strategy -> strategy.setDescription(description));
    }

    @Override
    public Optional<Strategy> updateDeploymentStatus(String id, String userId, String deploymentType, String deploymentId) {
        // NOTE: This method is deprecated. Deployment instances are now tracked separately
        // in AlertDeployment and BotDeployment entities. Strategy entity no longer has
        // deploymentType, deploymentId, or deployedAt fields.
        // Use deploymentCount field instead if you need to track how many times deployed.
        return updateField(id, userId, strategy -> {
            // No-op for now - deployment tracking happens in separate deployment entities
        });
    }

    private Optional<Strategy> updateField(String id, String userId, java.util.function.Consumer<Strategy> updater) {
        Optional<Strategy> existing = baseRepository.findById(id);
        if (existing.isEmpty() || !Boolean.TRUE.equals(existing.get().getIsActive())) {
            return Optional.empty();
        }

        Strategy strategy = existing.get();
        updater.accept(strategy);

        return Optional.of(baseRepository.save(strategy, userId));
    }
}