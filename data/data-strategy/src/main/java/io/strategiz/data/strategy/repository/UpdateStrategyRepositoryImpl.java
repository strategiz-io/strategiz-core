package io.strategiz.data.strategy.repository;

import io.strategiz.data.base.exception.DataRepositoryErrorDetails;
import io.strategiz.data.base.exception.DataRepositoryException;
import io.strategiz.data.strategy.entity.Strategy;
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
        // Verify ownership
        Optional<Strategy> existing = baseRepository.findById(id);
        if (existing.isEmpty() || !userId.equals(existing.get().getUserId()) || !Boolean.TRUE.equals(existing.get().getIsActive())) {
            throw new DataRepositoryException(DataRepositoryErrorDetails.ENTITY_NOT_FOUND_OR_UNAUTHORIZED, "Strategy", id);
        }

        // Ensure ID and userId are set
        strategy.setId(id);
        strategy.setUserId(userId);
        
        return baseRepository.save(strategy, userId);
    }
    
    @Override
    public boolean updateStatus(String id, String userId, String status) {
        Optional<Strategy> existing = baseRepository.findById(id);
        if (existing.isEmpty() || !userId.equals(existing.get().getUserId()) || !Boolean.TRUE.equals(existing.get().getIsActive())) {
            return false;
        }
        
        Strategy strategy = existing.get();
        strategy.setStatus(status);
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
    public Optional<Strategy> updatePerformance(String id, String userId, Map<String, Object> performance) {
        return updateField(id, userId, strategy -> strategy.setPerformance(performance));
    }
    
    @Override
    public Optional<Strategy> updateBacktestResults(String id, String userId, Map<String, Object> backtestResults) {
        return updateField(id, userId, strategy -> strategy.setBacktestResults(backtestResults));
    }
    
    @Override
    public Optional<Strategy> updateVisibility(String id, String userId, boolean isPublic) {
        return updateField(id, userId, strategy -> strategy.setPublic(isPublic));
    }
    
    @Override
    public Optional<Strategy> updateParameters(String id, String userId, Map<String, Object> parameters) {
        return updateField(id, userId, strategy -> strategy.setParameters(parameters));
    }
    
    @Override
    public Optional<Strategy> updateDescription(String id, String userId, String description) {
        return updateField(id, userId, strategy -> strategy.setDescription(description));
    }
    
    private Optional<Strategy> updateField(String id, String userId, java.util.function.Consumer<Strategy> updater) {
        Optional<Strategy> existing = baseRepository.findById(id);
        if (existing.isEmpty() || !userId.equals(existing.get().getUserId()) || !Boolean.TRUE.equals(existing.get().getIsActive())) {
            return Optional.empty();
        }
        
        Strategy strategy = existing.get();
        updater.accept(strategy);
        
        return Optional.of(baseRepository.save(strategy, userId));
    }
}