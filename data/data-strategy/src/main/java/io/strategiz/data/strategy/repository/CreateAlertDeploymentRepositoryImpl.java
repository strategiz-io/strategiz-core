package io.strategiz.data.strategy.repository;

import io.strategiz.data.strategy.entity.AlertDeployment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Implementation of CreateAlertDeploymentRepository using BaseRepository
 */
@Repository
public class CreateAlertDeploymentRepositoryImpl implements CreateAlertDeploymentRepository {

    private final AlertDeploymentBaseRepository baseRepository;

    @Autowired
    public CreateAlertDeploymentRepositoryImpl(AlertDeploymentBaseRepository baseRepository) {
        this.baseRepository = baseRepository;
    }

    @Override
    public AlertDeployment create(AlertDeployment strategyAlert) {
        // Generate ID if not provided
        if (strategyAlert.getId() == null || strategyAlert.getId().isEmpty()) {
            strategyAlert.setId(UUID.randomUUID().toString());
        }

        // Set default status if not provided
        if (strategyAlert.getStatus() == null || strategyAlert.getStatus().isEmpty()) {
            strategyAlert.setStatus("ACTIVE");
        }

        // Set default trigger count if not provided
        if (strategyAlert.getTriggerCount() == null) {
            strategyAlert.setTriggerCount(0);
        }

        // Use BaseRepository's save method (requires userId)
        return baseRepository.save(strategyAlert, strategyAlert.getUserId());
    }

    @Override
    public AlertDeployment createWithUserId(AlertDeployment strategyAlert, String userId) {
        strategyAlert.setUserId(userId);
        return create(strategyAlert);
    }
}
