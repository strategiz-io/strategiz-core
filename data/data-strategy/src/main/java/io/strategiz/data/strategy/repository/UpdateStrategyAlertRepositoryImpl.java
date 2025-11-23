package io.strategiz.data.strategy.repository;

import io.strategiz.data.strategy.entity.StrategyAlert;
import com.google.cloud.Timestamp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of UpdateStrategyAlertRepository using BaseRepository
 */
@Repository
public class UpdateStrategyAlertRepositoryImpl implements UpdateStrategyAlertRepository {

    private final StrategyAlertBaseRepository baseRepository;

    @Autowired
    public UpdateStrategyAlertRepositoryImpl(StrategyAlertBaseRepository baseRepository) {
        this.baseRepository = baseRepository;
    }

    @Override
    public StrategyAlert update(String id, String userId, StrategyAlert strategyAlert) {
        // Verify ownership
        Optional<StrategyAlert> existing = baseRepository.findById(id);
        if (existing.isEmpty() || !userId.equals(existing.get().getUserId()) || !Boolean.TRUE.equals(existing.get().getIsActive())) {
            throw new RuntimeException("Strategy alert not found or unauthorized");
        }

        // Ensure ID and userId are set
        strategyAlert.setId(id);
        strategyAlert.setUserId(userId);

        return baseRepository.save(strategyAlert, userId);
    }

    @Override
    public boolean updateStatus(String id, String userId, String status) {
        Optional<StrategyAlert> existing = baseRepository.findById(id);
        if (existing.isEmpty() || !userId.equals(existing.get().getUserId()) || !Boolean.TRUE.equals(existing.get().getIsActive())) {
            return false;
        }

        StrategyAlert alert = existing.get();
        alert.setStatus(status);
        baseRepository.save(alert, userId);
        return true;
    }

    @Override
    public Optional<StrategyAlert> updateSymbols(String id, String userId, List<String> symbols) {
        return updateField(id, userId, alert -> alert.setSymbols(symbols));
    }

    @Override
    public Optional<StrategyAlert> updateNotificationChannels(String id, String userId, List<String> notificationChannels) {
        return updateField(id, userId, alert -> alert.setNotificationChannels(notificationChannels));
    }

    @Override
    public Optional<StrategyAlert> updateLastCheckedAt(String id, String userId, Timestamp timestamp) {
        return updateField(id, userId, alert -> alert.setLastCheckedAt(timestamp));
    }

    @Override
    public Optional<StrategyAlert> recordTrigger(String id, String userId) {
        return updateField(id, userId, alert -> {
            alert.setLastTriggeredAt(Timestamp.now());
            alert.setTriggerCount(alert.getTriggerCount() + 1);
        });
    }

    @Override
    public Optional<StrategyAlert> updateErrorMessage(String id, String userId, String errorMessage) {
        return updateField(id, userId, alert -> {
            alert.setErrorMessage(errorMessage);
            alert.setStatus("ERROR");
        });
    }

    @Override
    public Optional<StrategyAlert> clearErrorMessage(String id, String userId) {
        return updateField(id, userId, alert -> {
            alert.setErrorMessage(null);
            if ("ERROR".equals(alert.getStatus())) {
                alert.setStatus("ACTIVE");
            }
        });
    }

    @Override
    public boolean pauseAlert(String id, String userId) {
        return updateStatus(id, userId, "PAUSED");
    }

    @Override
    public boolean resumeAlert(String id, String userId) {
        Optional<StrategyAlert> existing = baseRepository.findById(id);
        if (existing.isEmpty() || !userId.equals(existing.get().getUserId())) {
            return false;
        }

        // Clear error message if present when resuming
        StrategyAlert alert = existing.get();
        alert.setStatus("ACTIVE");
        alert.setErrorMessage(null);
        baseRepository.save(alert, userId);
        return true;
    }

    private Optional<StrategyAlert> updateField(String id, String userId, java.util.function.Consumer<StrategyAlert> updater) {
        Optional<StrategyAlert> existing = baseRepository.findById(id);
        if (existing.isEmpty() || !userId.equals(existing.get().getUserId()) || !Boolean.TRUE.equals(existing.get().getIsActive())) {
            return Optional.empty();
        }

        StrategyAlert alert = existing.get();
        updater.accept(alert);

        return Optional.of(baseRepository.save(alert, userId));
    }
}
