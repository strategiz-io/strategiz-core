package io.strategiz.data.strategy.repository;

import io.strategiz.data.strategy.entity.StrategyCommentEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of ReadStrategyCommentRepository.
 */
@Repository
public class ReadStrategyCommentRepositoryImpl implements ReadStrategyCommentRepository {

    private final StrategyCommentBaseRepository baseRepository;

    @Autowired
    public ReadStrategyCommentRepositoryImpl(StrategyCommentBaseRepository baseRepository) {
        this.baseRepository = baseRepository;
    }

    @Override
    public Optional<StrategyCommentEntity> findById(String id) {
        return baseRepository.findById(id);
    }

    @Override
    public List<StrategyCommentEntity> findByStrategyId(String strategyId) {
        return baseRepository.findByStrategyId(strategyId);
    }

    @Override
    public List<StrategyCommentEntity> findTopLevelByStrategyId(String strategyId, int limit) {
        return baseRepository.findByStrategyIdOrderByDate(strategyId, limit);
    }

    @Override
    public List<StrategyCommentEntity> findReplies(String parentCommentId) {
        return baseRepository.findReplies(parentCommentId);
    }

    @Override
    public List<StrategyCommentEntity> findByUserId(String userId) {
        return baseRepository.findByUserId(userId);
    }

    @Override
    public int countByStrategyId(String strategyId) {
        return baseRepository.countByStrategyId(strategyId);
    }

    @Override
    public boolean existsById(String id) {
        return baseRepository.findById(id).isPresent();
    }
}
