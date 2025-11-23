package io.strategiz.data.strategy.repository;

import io.strategiz.data.strategy.entity.StrategyAlert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

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
}
