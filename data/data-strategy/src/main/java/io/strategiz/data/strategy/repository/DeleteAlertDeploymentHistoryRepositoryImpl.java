package io.strategiz.data.strategy.repository;

import io.strategiz.data.strategy.entity.StrategyAlertHistory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of DeleteStrategyAlertHistoryRepository using BaseRepository
 */
@Repository
public class DeleteStrategyAlertHistoryRepositoryImpl implements DeleteStrategyAlertHistoryRepository {

    private final StrategyAlertHistoryBaseRepository baseRepository;

    @Autowired
    public DeleteStrategyAlertHistoryRepositoryImpl(StrategyAlertHistoryBaseRepository baseRepository) {
        this.baseRepository = baseRepository;
    }

    @Override
    public boolean delete(String id, String userId) {
        // Verify ownership before deleting
        Optional<StrategyAlertHistory> existing = baseRepository.findById(id);
        if (existing.isEmpty() || !userId.equals(existing.get().getUserId())) {
            return false;
        }

        return baseRepository.delete(id, userId);
    }

    @Override
    public int deleteByAlertId(String alertId, String userId) {
        List<StrategyAlertHistory> historyRecords = baseRepository.findAllByAlertId(alertId);

        int deleted = 0;
        for (StrategyAlertHistory history : historyRecords) {
            // Verify ownership
            if (userId.equals(history.getUserId())) {
                if (baseRepository.delete(history.getId(), userId)) {
                    deleted++;
                }
            }
        }

        return deleted;
    }

    @Override
    public boolean restore(String id, String userId) {
        // Use BaseRepository's restore method
        Optional<StrategyAlertHistory> restored = baseRepository.restore(id, userId);
        return restored.isPresent();
    }
}
