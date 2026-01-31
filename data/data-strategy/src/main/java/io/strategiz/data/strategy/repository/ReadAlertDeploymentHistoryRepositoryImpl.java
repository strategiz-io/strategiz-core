package io.strategiz.data.strategy.repository;

import io.strategiz.data.strategy.entity.AlertDeploymentHistory;
import com.google.cloud.Timestamp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of ReadAlertDeploymentHistoryRepository using BaseRepository
 */
@Repository
public class ReadAlertDeploymentHistoryRepositoryImpl implements ReadAlertDeploymentHistoryRepository {

	private final AlertDeploymentHistoryBaseRepository baseRepository;

	@Autowired
	public ReadAlertDeploymentHistoryRepositoryImpl(AlertDeploymentHistoryBaseRepository baseRepository) {
		this.baseRepository = baseRepository;
	}

	@Override
	public Optional<AlertDeploymentHistory> findById(String id) {
		return baseRepository.findById(id);
	}

	@Override
	public List<AlertDeploymentHistory> findByUserId(String userId) {
		return baseRepository.findAllByUserId(userId);
	}

	@Override
	public List<AlertDeploymentHistory> findByAlertId(String alertId) {
		return baseRepository.findAllByAlertId(alertId);
	}

	@Override
	public List<AlertDeploymentHistory> findByAlertIdAndUserId(String alertId, String userId) {
		return baseRepository.findAllByAlertId(alertId)
			.stream()
			.filter(history -> userId.equals(history.getUserId()))
			.collect(Collectors.toList());
	}

	@Override
	public List<AlertDeploymentHistory> findBySymbol(String userId, String symbol) {
		return baseRepository.findAllByUserId(userId)
			.stream()
			.filter(history -> symbol.equals(history.getSymbol()))
			.collect(Collectors.toList());
	}

	@Override
	public List<AlertDeploymentHistory> findBySignal(String userId, String signal) {
		return baseRepository.findAllByUserId(userId)
			.stream()
			.filter(history -> signal.equals(history.getSignal()))
			.collect(Collectors.toList());
	}

	@Override
	public List<AlertDeploymentHistory> findRecentByUserId(String userId, int limit) {
		return baseRepository.findAllByUserId(userId).stream().sorted((h1, h2) -> {
			if (h2.getTimestamp() == null)
				return -1;
			if (h1.getTimestamp() == null)
				return 1;
			return h2.getTimestamp().compareTo(h1.getTimestamp());
		}).limit(limit).collect(Collectors.toList());
	}

	@Override
	public List<AlertDeploymentHistory> findByTimeRange(String userId, Timestamp startTime, Timestamp endTime) {
		return baseRepository.findAllByUserId(userId).stream().filter(history -> {
			Timestamp timestamp = history.getTimestamp();
			if (timestamp == null)
				return false;
			return !timestamp.toSqlTimestamp().before(startTime.toSqlTimestamp())
					&& !timestamp.toSqlTimestamp().after(endTime.toSqlTimestamp());
		}).collect(Collectors.toList());
	}

	@Override
	public List<AlertDeploymentHistory> findUnsentNotifications(String userId) {
		return baseRepository.findAllByUserId(userId)
			.stream()
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
