package io.strategiz.data.strategy.repository;

import io.strategiz.data.strategy.entity.AlertDeployment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Implementation of DeleteAlertDeploymentRepository using BaseRepository
 */
@Repository
public class DeleteAlertDeploymentRepositoryImpl implements DeleteAlertDeploymentRepository {

    private final AlertDeploymentBaseRepository baseRepository;

    @Autowired
    public DeleteAlertDeploymentRepositoryImpl(AlertDeploymentBaseRepository baseRepository) {
        this.baseRepository = baseRepository;
    }

    @Override
    public boolean delete(String id, String userId) {
        // Verify ownership before deleting
        Optional<AlertDeployment> existing = baseRepository.findById(id);
        if (existing.isEmpty() || !userId.equals(existing.get().getUserId())) {
            return false;
        }

        return baseRepository.delete(id, userId);
    }

    @Override
    public boolean stopAndDelete(String id, String userId) {
        // Verify ownership
        Optional<AlertDeployment> existing = baseRepository.findById(id);
        if (existing.isEmpty() || !userId.equals(existing.get().getUserId())) {
            return false;
        }

        // Set status to STOPPED before deleting
        AlertDeployment alert = existing.get();
        alert.setStatus("STOPPED");
        baseRepository.save(alert, userId);

        // Then soft delete
        return baseRepository.delete(id, userId);
    }

    @Override
    public boolean restore(String id, String userId) {
        // Use BaseRepository's restore method
        Optional<AlertDeployment> restored = baseRepository.restore(id, userId);
        return restored.isPresent();
    }
}
