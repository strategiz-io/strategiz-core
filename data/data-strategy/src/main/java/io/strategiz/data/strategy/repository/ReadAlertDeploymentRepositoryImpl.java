package io.strategiz.data.strategy.repository;

import io.strategiz.data.strategy.entity.DeploymentType;
import io.strategiz.data.strategy.entity.AlertDeployment;
import com.google.cloud.Timestamp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of ReadAlertDeploymentRepository using BaseRepository
 */
@Repository
public class ReadAlertDeploymentRepositoryImpl implements ReadAlertDeploymentRepository {

    private final AlertDeploymentBaseRepository baseRepository;

    @Autowired
    public ReadAlertDeploymentRepositoryImpl(AlertDeploymentBaseRepository baseRepository) {
        this.baseRepository = baseRepository;
    }

    @Override
    public Optional<AlertDeployment> findById(String id) {
        return baseRepository.findById(id);
    }

    @Override
    public List<AlertDeployment> findByUserId(String userId) {
        return baseRepository.findAllByUserId(userId);
    }

    @Override
    public List<AlertDeployment> findByUserIdAndStatus(String userId, String status) {
        return baseRepository.findAllByUserId(userId).stream()
                .filter(alert -> status.equals(alert.getStatus()))
                .collect(Collectors.toList());
    }

    @Override
    public List<AlertDeployment> findByStrategyId(String strategyId) {
        return baseRepository.findAllByStrategyId(strategyId);
    }

    @Override
    public List<AlertDeployment> findActiveByUserId(String userId) {
        return baseRepository.findAllByUserId(userId).stream()
                .filter(alert -> "ACTIVE".equals(alert.getStatus()))
                .collect(Collectors.toList());
    }

    @Override
    public List<AlertDeployment> findByProviderId(String userId, String providerId) {
        return baseRepository.findAllByUserId(userId).stream()
                .filter(alert -> providerId.equals(alert.getProviderId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<AlertDeployment> findBySubscriptionTier(String userId, String subscriptionTier) {
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
    public List<AlertDeployment> findAllActive() {
        return baseRepository.findAllByStatus("ACTIVE");
    }

    @Override
    public List<AlertDeployment> findActiveAlertsByTier(String subscriptionTier) {
        // Only return ALERT deployment types (not BOT or PAPER)
        return baseRepository.findActiveAlertsByTierAndType(subscriptionTier, DeploymentType.ALERT.name());
    }

    @Override
    public List<AlertDeployment> findActiveAlertsDueForEvaluation(int maxMinutesSinceLastCheck) {
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
