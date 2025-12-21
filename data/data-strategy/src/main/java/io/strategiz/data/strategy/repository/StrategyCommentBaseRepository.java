package io.strategiz.data.strategy.repository;

import io.strategiz.data.base.repository.BaseRepository;
import io.strategiz.data.strategy.entity.StrategyCommentEntity;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Base repository for StrategyComment entities using Firestore.
 * Used internally by CRUD repository implementations.
 */
@Repository
public class StrategyCommentBaseRepository extends BaseRepository<StrategyCommentEntity> {

    public StrategyCommentBaseRepository(Firestore firestore) {
        super(firestore, StrategyCommentEntity.class);
    }

    /**
     * Find all comments for a strategy.
     */
    public List<StrategyCommentEntity> findByStrategyId(String strategyId) {
        return findByField("strategyId", strategyId);
    }

    /**
     * Find all comments by a user.
     */
    public List<StrategyCommentEntity> findByUserId(String userId) {
        return findByField("userId", userId);
    }

    /**
     * Find top-level comments for a strategy (no parent).
     */
    public List<StrategyCommentEntity> findTopLevelByStrategyId(String strategyId) {
        return findByStrategyId(strategyId).stream()
                .filter(c -> c.getParentCommentId() == null || c.getParentCommentId().isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * Find replies to a comment.
     */
    public List<StrategyCommentEntity> findReplies(String parentCommentId) {
        return findByField("parentCommentId", parentCommentId);
    }

    /**
     * Count comments for a strategy.
     */
    public int countByStrategyId(String strategyId) {
        return findByStrategyId(strategyId).size();
    }

    /**
     * Find comments for a strategy, ordered by date (newest first).
     */
    public List<StrategyCommentEntity> findByStrategyIdOrderByDate(String strategyId, int limit) {
        try {
            List<QueryDocumentSnapshot> docs = getCollection()
                    .whereEqualTo("strategyId", strategyId)
                    .whereEqualTo("isActive", true)
                    .orderBy("commentedAt", Query.Direction.DESCENDING)
                    .limit(limit)
                    .get()
                    .get()
                    .getDocuments();

            return docs.stream()
                    .map(doc -> {
                        StrategyCommentEntity entity = doc.toObject(StrategyCommentEntity.class);
                        entity.setId(doc.getId());
                        return entity;
                    })
                    .collect(Collectors.toList());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to query comments", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to query comments", e);
        }
    }
}
