package io.strategiz.data.strategy.repository;

import io.strategiz.data.strategy.entity.AlertDeploymentHistory;
import com.google.cloud.Timestamp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Implementation of CreateAlertDeploymentHistoryRepository using BaseRepository
 */
@Repository
public class CreateAlertDeploymentHistoryRepositoryImpl implements CreateAlertDeploymentHistoryRepository {

    private final AlertDeploymentHistoryBaseRepository baseRepository;

    @Autowired
    public CreateAlertDeploymentHistoryRepositoryImpl(AlertDeploymentHistoryBaseRepository baseRepository) {
        this.baseRepository = baseRepository;
    }

    @Override
    public AlertDeploymentHistory create(AlertDeploymentHistory alertHistory) {
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
    public AlertDeploymentHistory createWithUserId(AlertDeploymentHistory alertHistory, String userId) {
        alertHistory.setUserId(userId);
        return create(alertHistory);
    }
}
