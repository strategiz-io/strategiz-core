package io.strategiz.data.strategy.repository;

import io.strategiz.data.base.exception.DataRepositoryErrorDetails;
import io.strategiz.data.base.exception.DataRepositoryException;
import io.strategiz.data.strategy.entity.AlertDeploymentHistory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation of UpdateAlertDeploymentHistoryRepository using BaseRepository
 */
@Repository
public class UpdateAlertDeploymentHistoryRepositoryImpl implements UpdateAlertDeploymentHistoryRepository {

	private final AlertDeploymentHistoryBaseRepository baseRepository;

	@Autowired
	public UpdateAlertDeploymentHistoryRepositoryImpl(AlertDeploymentHistoryBaseRepository baseRepository) {
		this.baseRepository = baseRepository;
	}

	@Override
	public AlertDeploymentHistory update(String id, String userId, AlertDeploymentHistory alertHistory) {
		// Verify ownership
		Optional<AlertDeploymentHistory> existing = baseRepository.findById(id);
		if (existing.isEmpty() || !userId.equals(existing.get().getUserId())
				|| !Boolean.TRUE.equals(existing.get().getIsActive())) {
			throw new DataRepositoryException(DataRepositoryErrorDetails.ENTITY_NOT_FOUND_OR_UNAUTHORIZED,
					"AlertDeploymentHistory", id);
		}

		// Ensure ID and userId are set
		alertHistory.setId(id);
		alertHistory.setUserId(userId);

		return baseRepository.save(alertHistory, userId);
	}

	@Override
	public Optional<AlertDeploymentHistory> markNotificationSent(String id, String userId) {
		return updateField(id, userId, history -> history.setNotificationSent(true));
	}

	@Override
	public Optional<AlertDeploymentHistory> updateMetadata(String id, String userId, Map<String, Object> metadata) {
		return updateField(id, userId, history -> history.setMetadata(metadata));
	}

	@Override
	public Optional<AlertDeploymentHistory> addMetadataField(String id, String userId, String key, Object value) {
		return updateField(id, userId, history -> {
			Map<String, Object> metadata = history.getMetadata();
			if (metadata == null) {
				metadata = new HashMap<>();
			}
			metadata.put(key, value);
			history.setMetadata(metadata);
		});
	}

	private Optional<AlertDeploymentHistory> updateField(String id, String userId,
			java.util.function.Consumer<AlertDeploymentHistory> updater) {
		Optional<AlertDeploymentHistory> existing = baseRepository.findById(id);
		if (existing.isEmpty() || !userId.equals(existing.get().getUserId())
				|| !Boolean.TRUE.equals(existing.get().getIsActive())) {
			return Optional.empty();
		}

		AlertDeploymentHistory history = existing.get();
		updater.accept(history);

		return Optional.of(baseRepository.save(history, userId));
	}

}
