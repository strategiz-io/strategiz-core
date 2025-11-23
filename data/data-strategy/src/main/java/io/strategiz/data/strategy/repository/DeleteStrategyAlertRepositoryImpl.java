package io.strategiz.data.strategy.repository;

import io.strategiz.data.strategy.entity.StrategyAlert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Implementation of DeleteStrategyAlertRepository using BaseRepository
 */
@Repository
public class DeleteStrategyAlertRepositoryImpl implements DeleteStrategyAlertRepository {

    private final StrategyAlertBaseRepository baseRepository;

    @Autowired
    public DeleteStrategyAlertRepositoryImpl(StrategyAlertBaseRepository baseRepository) {
        this.baseRepository = baseRepository;
    }

    @Override
    public boolean delete(String id, String userId) {
        // Verify ownership before deleting
        Optional<StrategyAlert> existing = baseRepository.findById(id);
        if (existing.isEmpty() || !userId.equals(existing.get().getUserId())) {
            return false;
        }

        return baseRepository.delete(id, userId);
    }

    @Override
    public boolean stopAndDelete(String id, String userId) {
        // Verify ownership
        Optional<StrategyAlert> existing = baseRepository.findById(id);
        if (existing.isEmpty() || !userId.equals(existing.get().getUserId())) {
            return false;
        }

        // Set status to STOPPED before deleting
        StrategyAlert alert = existing.get();
        alert.setStatus("STOPPED");
        baseRepository.save(alert, userId);

        // Then soft delete
        return baseRepository.delete(id, userId);
    }

    @Override
    public boolean restore(String id, String userId) {
        // Use BaseRepository's restore method
        Optional<StrategyAlert> restored = baseRepository.restore(id, userId);
        return restored.isPresent();
    }
}
