package io.strategiz.data.strategy.repository;

import io.strategiz.data.strategy.entity.StrategyAlert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Implementation of CreateStrategyAlertRepository using BaseRepository
 */
@Repository
public class CreateStrategyAlertRepositoryImpl implements CreateStrategyAlertRepository {

    private final StrategyAlertBaseRepository baseRepository;

    @Autowired
    public CreateStrategyAlertRepositoryImpl(StrategyAlertBaseRepository baseRepository) {
        this.baseRepository = baseRepository;
    }

    @Override
    public StrategyAlert create(StrategyAlert strategyAlert) {
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
    public StrategyAlert createWithUserId(StrategyAlert strategyAlert, String userId) {
        strategyAlert.setUserId(userId);
        return create(strategyAlert);
    }
}
