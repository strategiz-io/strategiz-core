package io.strategiz.data.strategy.repository;

import io.strategiz.data.strategy.entity.DeploymentType;
import io.strategiz.data.strategy.entity.StrategyAlert;
import com.google.cloud.Timestamp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of ReadStrategyAlertRepository using BaseRepository
 */
@Repository
public class ReadStrategyAlertRepositoryImpl implements ReadStrategyAlertRepository {

    private final StrategyAlertBaseRepository baseRepository;

    @Autowired
    public ReadStrategyAlertRepositoryImpl(StrategyAlertBaseRepository baseRepository) {
        this.baseRepository = baseRepository;
    }

    @Override
    public Optional<StrategyAlert> findById(String id) {
        return baseRepository.findById(id);
    }

    @Override
    public List<StrategyAlert> findByUserId(String userId) {
        return baseRepository.findAllByUserId(userId);
    }

    @Override
    public List<StrategyAlert> findByUserIdAndStatus(String userId, String status) {
        return baseRepository.findAllByUserId(userId).stream()
                .filter(alert -> status.equals(alert.getStatus()))
                .collect(Collectors.toList());
    }

    @Override
    public List<StrategyAlert> findByStrategyId(String strategyId) {
        return baseRepository.findAllByStrategyId(strategyId);
    }

    @Override
    public List<StrategyAlert> findActiveByUserId(String userId) {
        return baseRepository.findAllByUserId(userId).stream()
                .filter(alert -> "ACTIVE".equals(alert.getStatus()))
                .collect(Collectors.toList());
    }

    @Override
    public List<StrategyAlert> findByProviderId(String userId, String providerId) {
        return baseRepository.findAllByUserId(userId).stream()
                .filter(alert -> providerId.equals(alert.getProviderId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<StrategyAlert> findBySubscriptionTier(String userId, String subscriptionTier) {
        return baseRepository.findAllByUserId(userId).stream()
                .filter(alert -> subscriptionTier.equals(alert.getSubscriptionTier()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsById(String id) {
        return baseRepository.findById(id).isPresent();
    }

    @Override
    public int countActiveByUserId(String userId) {
        return (int) baseRepository.findAllByUserId(userId).stream()
                .filter(alert -> "ACTIVE".equals(alert.getStatus()))
                .count();
    }

    @Override
    public List<StrategyAlert> findAllActive() {
        return baseRepository.findAllByStatus("ACTIVE");
    }

    @Override
    public List<StrategyAlert> findActiveAlertsByTier(String subscriptionTier) {
        // Only return ALERT deployment types (not BOT or PAPER)
        return baseRepository.findActiveAlertsByTierAndType(subscriptionTier, DeploymentType.ALERT.name());
    }

    @Override
    public List<StrategyAlert> findActiveAlertsDueForEvaluation(int maxMinutesSinceLastCheck) {
        // Get all active alerts and filter by last checked time
        // Note: For better performance with large datasets, this should be done
        // at the database level with a Firestore query using timestamps
        Instant cutoff = Instant.now().minus(maxMinutesSinceLastCheck, ChronoUnit.MINUTES);
        Timestamp cutoffTimestamp = Timestamp.ofTimeSecondsAndNanos(cutoff.getEpochSecond(), cutoff.getNano());

        return baseRepository.findAllByStatus("ACTIVE").stream()
                .filter(alert -> {
                    // Include if never checked OR last checked before cutoff
                    if (alert.getLastCheckedAt() == null) {
                        return true;
                    }
                    return alert.getLastCheckedAt().compareTo(cutoffTimestamp) < 0;
                })
                .collect(Collectors.toList());
    }
}
