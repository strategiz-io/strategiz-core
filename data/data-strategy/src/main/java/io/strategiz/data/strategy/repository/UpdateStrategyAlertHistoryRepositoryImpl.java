package io.strategiz.data.strategy.repository;

import io.strategiz.data.strategy.entity.StrategyAlertHistory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation of UpdateStrategyAlertHistoryRepository using BaseRepository
 */
@Repository
public class UpdateStrategyAlertHistoryRepositoryImpl implements UpdateStrategyAlertHistoryRepository {

    private final StrategyAlertHistoryBaseRepository baseRepository;

    @Autowired
    public UpdateStrategyAlertHistoryRepositoryImpl(StrategyAlertHistoryBaseRepository baseRepository) {
        this.baseRepository = baseRepository;
    }

    @Override
    public StrategyAlertHistory update(String id, String userId, StrategyAlertHistory alertHistory) {
        // Verify ownership
        Optional<StrategyAlertHistory> existing = baseRepository.findById(id);
        if (existing.isEmpty() || !userId.equals(existing.get().getUserId()) || !Boolean.TRUE.equals(existing.get().getIsActive())) {
            throw new RuntimeException("Alert history not found or unauthorized");
        }

        // Ensure ID and userId are set
        alertHistory.setId(id);
        alertHistory.setUserId(userId);

        return baseRepository.save(alertHistory, userId);
    }

    @Override
    public Optional<StrategyAlertHistory> markNotificationSent(String id, String userId) {
        return updateField(id, userId, history -> history.setNotificationSent(true));
    }

    @Override
    public Optional<StrategyAlertHistory> updateMetadata(String id, String userId, Map<String, Object> metadata) {
        return updateField(id, userId, history -> history.setMetadata(metadata));
    }

    @Override
    public Optional<StrategyAlertHistory> addMetadataField(String id, String userId, String key, Object value) {
        return updateField(id, userId, history -> {
            Map<String, Object> metadata = history.getMetadata();
            if (metadata == null) {
                metadata = new HashMap<>();
            }
            metadata.put(key, value);
            history.setMetadata(metadata);
        });
    }

    private Optional<StrategyAlertHistory> updateField(String id, String userId, java.util.function.Consumer<StrategyAlertHistory> updater) {
        Optional<StrategyAlertHistory> existing = baseRepository.findById(id);
        if (existing.isEmpty() || !userId.equals(existing.get().getUserId()) || !Boolean.TRUE.equals(existing.get().getIsActive())) {
            return Optional.empty();
        }

        StrategyAlertHistory history = existing.get();
        updater.accept(history);

        return Optional.of(baseRepository.save(history, userId));
    }
}
