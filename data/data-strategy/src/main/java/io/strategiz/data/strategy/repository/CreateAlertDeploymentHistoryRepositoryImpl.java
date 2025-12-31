package io.strategiz.data.strategy.repository;

import io.strategiz.data.strategy.entity.StrategyAlertHistory;
import com.google.cloud.Timestamp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Implementation of CreateStrategyAlertHistoryRepository using BaseRepository
 */
@Repository
public class CreateStrategyAlertHistoryRepositoryImpl implements CreateStrategyAlertHistoryRepository {

    private final StrategyAlertHistoryBaseRepository baseRepository;

    @Autowired
    public CreateStrategyAlertHistoryRepositoryImpl(StrategyAlertHistoryBaseRepository baseRepository) {
        this.baseRepository = baseRepository;
    }

    @Override
    public StrategyAlertHistory create(StrategyAlertHistory alertHistory) {
        // Generate ID if not provided
        if (alertHistory.getId() == null || alertHistory.getId().isEmpty()) {
            alertHistory.setId(UUID.randomUUID().toString());
        }

        // Set timestamp if not provided
        if (alertHistory.getTimestamp() == null) {
            alertHistory.setTimestamp(Timestamp.now());
        }

        // Set notificationSent default if not provided
        if (alertHistory.getNotificationSent() == null) {
            alertHistory.setNotificationSent(false);
        }

        // Use BaseRepository's save method (requires userId)
        return baseRepository.save(alertHistory, alertHistory.getUserId());
    }

    @Override
    public StrategyAlertHistory createWithUserId(StrategyAlertHistory alertHistory, String userId) {
        alertHistory.setUserId(userId);
        return create(alertHistory);
    }
}
