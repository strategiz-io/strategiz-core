package io.strategiz.data.strategy.repository;

import io.strategiz.data.strategy.entity.StrategyAlertHistory;
import com.google.cloud.Timestamp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of ReadStrategyAlertHistoryRepository using BaseRepository
 */
@Repository
public class ReadStrategyAlertHistoryRepositoryImpl implements ReadStrategyAlertHistoryRepository {

    private final StrategyAlertHistoryBaseRepository baseRepository;

    @Autowired
    public ReadStrategyAlertHistoryRepositoryImpl(StrategyAlertHistoryBaseRepository baseRepository) {
        this.baseRepository = baseRepository;
    }

    @Override
    public Optional<StrategyAlertHistory> findById(String id) {
        return baseRepository.findById(id);
    }

    @Override
    public List<StrategyAlertHistory> findByUserId(String userId) {
        return baseRepository.findAllByUserId(userId);
    }

    @Override
    public List<StrategyAlertHistory> findByAlertId(String alertId) {
        return baseRepository.findAllByAlertId(alertId);
    }

    @Override
    public List<StrategyAlertHistory> findByAlertIdAndUserId(String alertId, String userId) {
        return baseRepository.findAllByAlertId(alertId).stream()
                .filter(history -> userId.equals(history.getUserId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<StrategyAlertHistory> findBySymbol(String userId, String symbol) {
        return baseRepository.findAllByUserId(userId).stream()
                .filter(history -> symbol.equals(history.getSymbol()))
                .collect(Collectors.toList());
    }

    @Override
    public List<StrategyAlertHistory> findBySignal(String userId, String signal) {
        return baseRepository.findAllByUserId(userId).stream()
                .filter(history -> signal.equals(history.getSignal()))
                .collect(Collectors.toList());
    }

    @Override
    public List<StrategyAlertHistory> findRecentByUserId(String userId, int limit) {
        return baseRepository.findAllByUserId(userId).stream()
                .sorted((h1, h2) -> {
                    if (h2.getTimestamp() == null) return -1;
                    if (h1.getTimestamp() == null) return 1;
                    return h2.getTimestamp().compareTo(h1.getTimestamp());
                })
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<StrategyAlertHistory> findByTimeRange(String userId, Timestamp startTime, Timestamp endTime) {
        return baseRepository.findAllByUserId(userId).stream()
                .filter(history -> {
                    Timestamp timestamp = history.getTimestamp();
                    if (timestamp == null) return false;
                    return !timestamp.toSqlTimestamp().before(startTime.toSqlTimestamp()) &&
                           !timestamp.toSqlTimestamp().after(endTime.toSqlTimestamp());
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<StrategyAlertHistory> findUnsentNotifications(String userId) {
        return baseRepository.findAllByUserId(userId).stream()
                .filter(history -> Boolean.FALSE.equals(history.getNotificationSent()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsById(String id) {
        return baseRepository.findById(id).isPresent();
    }

    @Override
    public int countByAlertId(String alertId) {
        return baseRepository.findAllByAlertId(alertId).size();
    }
}
