package io.strategiz.data.provider.repository;

import io.strategiz.data.provider.entity.PortfolioHistoryEntity;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;

/**
 * Implementation of CreatePortfolioHistoryRepository for Firestore operations.
 */
@Repository
public class CreatePortfolioHistoryRepositoryImpl extends PortfolioHistoryBaseRepository
        implements CreatePortfolioHistoryRepository {

    public CreatePortfolioHistoryRepositoryImpl(Firestore firestore) {
        super(firestore);
    }

    @Override
    public PortfolioHistoryEntity createSnapshot(PortfolioHistoryEntity snapshot) {
        if (snapshot.getUserId() == null) {
            throw new IllegalArgumentException("userId is required to create portfolio history snapshot");
        }
        return save(snapshot, snapshot.getUserId());
    }

    @Override
    public PortfolioHistoryEntity saveSnapshot(PortfolioHistoryEntity snapshot) {
        if (snapshot.getUserId() == null) {
            throw new IllegalArgumentException("userId is required to save portfolio history snapshot");
        }
        return save(snapshot, snapshot.getUserId());
    }
}
