package io.strategiz.data.provider.repository;

import io.strategiz.data.provider.entity.PortfolioHistoryEntity;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of ReadPortfolioHistoryRepository for Firestore operations.
 */
@Repository
public class ReadPortfolioHistoryRepositoryImpl extends PortfolioHistoryBaseRepository
        implements ReadPortfolioHistoryRepository {

    public ReadPortfolioHistoryRepositoryImpl(Firestore firestore) {
        super(firestore);
    }

    @Override
    public Optional<PortfolioHistoryEntity> findByUserIdAndDate(String userId, LocalDate date) {
        return super.findByUserIdAndDate(userId, date);
    }

    @Override
    public List<PortfolioHistoryEntity> findByUserId(String userId) {
        return super.findByUserId(userId);
    }

    @Override
    public List<PortfolioHistoryEntity> findByUserIdAndDateRange(String userId, LocalDate startDate, LocalDate endDate) {
        return super.findByUserIdAndDateRange(userId, startDate, endDate);
    }

    @Override
    public Optional<PortfolioHistoryEntity> findLatestByUserId(String userId) {
        return super.findLatestByUserId(userId);
    }

    @Override
    public List<PortfolioHistoryEntity> findRecentByUserId(String userId, int limit) {
        return super.findRecentByUserId(userId, limit);
    }
}
