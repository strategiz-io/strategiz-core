package io.strategiz.data.strategy.repository;

import io.strategiz.data.strategy.entity.Strategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of DeleteStrategyRepository using BaseRepository
 */
@Repository
public class DeleteStrategyRepositoryImpl implements DeleteStrategyRepository {
    
    private final StrategyBaseRepository baseRepository;
    
    @Autowired
    public DeleteStrategyRepositoryImpl(StrategyBaseRepository baseRepository) {
        this.baseRepository = baseRepository;
    }
    
    @Override
    public boolean deleteByIdAndUserId(String id, String userId) {
        Optional<Strategy> existing = baseRepository.findById(id);
        if (existing.isEmpty() || !userId.equals(existing.get().getUserId()) || !existing.get().isActive()) {
            return false;
        }
        
        // Use BaseRepository's soft delete
        return baseRepository.delete(id, userId);
    }
    
    @Override
    public int deleteAllByUserId(String userId) {
        List<Strategy> strategies = baseRepository.findAllByUserId(userId);
        int deletedCount = 0;
        
        for (Strategy strategy : strategies) {
            if (strategy.isActive()) {
                if (baseRepository.delete(strategy.getId(), userId)) {
                    deletedCount++;
                }
            }
        }
        
        return deletedCount;
    }
    
    @Override
    public boolean hardDeleteByIdAndUserId(String id, String userId) {
        // TODO: Implement hard delete if needed
        // For now, just do soft delete
        return deleteByIdAndUserId(id, userId);
    }
}