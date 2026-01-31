package io.strategiz.data.strategy.repository;

import io.strategiz.data.strategy.entity.AlertDeploymentHistory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of DeleteAlertDeploymentHistoryRepository using BaseRepository
 */
@Repository
public class DeleteAlertDeploymentHistoryRepositoryImpl implements DeleteAlertDeploymentHistoryRepository {

	private final AlertDeploymentHistoryBaseRepository baseRepository;

	@Autowired
	public DeleteAlertDeploymentHistoryRepositoryImpl(AlertDeploymentHistoryBaseRepository baseRepository) {
		this.baseRepository = baseRepository;
	}

	@Override
	public boolean delete(String id, String userId) {
		// Verify ownership before deleting
		Optional<AlertDeploymentHistory> existing = baseRepository.findById(id);
		if (existing.isEmpty() || !userId.equals(existing.get().getUserId())) {
			return false;
		}

		return baseRepository.delete(id, userId);
	}

	@Override
	public int deleteByAlertId(String alertId, String userId) {
		List<AlertDeploymentHistory> historyRecords = baseRepository.findAllByAlertId(alertId);

		int deleted = 0;
		for (AlertDeploymentHistory history : historyRecords) {
			// Verify ownership
			if (userId.equals(history.getUserId())) {
				if (baseRepository.delete(history.getId(), userId)) {
					deleted++;
				}
			}
		}

		return deleted;
	}

	@Override
	public boolean restore(String id, String userId) {
		// Use BaseRepository's restore method
		Optional<AlertDeploymentHistory> restored = baseRepository.restore(id, userId);
		return restored.isPresent();
	}

}
