package io.strategiz.data.strategy.repository;

import io.strategiz.data.strategy.entity.Strategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of ReadStrategyRepository using BaseRepository
 */
@Repository
public class ReadStrategyRepositoryImpl implements ReadStrategyRepository {
    
    private final StrategyBaseRepository baseRepository;
    
    @Autowired
    public ReadStrategyRepositoryImpl(StrategyBaseRepository baseRepository) {
        this.baseRepository = baseRepository;
    }
    
    @Override
    public Optional<Strategy> findById(String id) {
        return baseRepository.findById(id);
    }
    
    @Override
    public List<Strategy> findByUserId(String userId) {
        return baseRepository.findAllByUserId(userId);
    }
    
    @Override
    public List<Strategy> findByUserIdAndStatus(String userId, String status) {
        return baseRepository.findAllByUserId(userId).stream()
                .filter(s -> status.equals(s.getStatus()))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Strategy> findByUserIdAndLanguage(String userId, String language) {
        return baseRepository.findAllByUserId(userId).stream()
                .filter(s -> language.equals(s.getLanguage()))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Strategy> findPublicStrategies() {
        // TODO: Implement cross-user query for public strategies
        // For now return empty list - will need custom Firestore query
        return List.of();
    }
    
    @Override
    public List<Strategy> findPublicStrategiesByLanguage(String language) {
        // TODO: Implement cross-user query 
        return List.of();
    }
    
    @Override
    public List<Strategy> findPublicStrategiesByTags(List<String> tags) {
        // TODO: Implement cross-user query
        return List.of();
    }
    
    @Override
    public List<Strategy> searchByName(String userId, String searchTerm) {
        String lowerSearchTerm = searchTerm.toLowerCase();
        return baseRepository.findAllByUserId(userId).stream()
                .filter(s -> s.getName() != null && 
                        s.getName().toLowerCase().contains(lowerSearchTerm))
                .collect(Collectors.toList());
    }
    
    @Override
    public boolean existsById(String id) {
        return baseRepository.findById(id).isPresent();
    }

    @Override
    public List<Strategy> findVersionsByParentId(String parentStrategyId) {
        return baseRepository.findAllByParentStrategyId(parentStrategyId);
    }

    @Override
    public Optional<Strategy> findLatestVersion(String parentStrategyId) {
        List<Strategy> versions = findVersionsByParentId(parentStrategyId);
        return versions.stream()
                .max((s1, s2) -> {
                    Long v1 = s1.getVersion() != null ? s1.getVersion() : 1L;
                    Long v2 = s2.getVersion() != null ? s2.getVersion() : 1L;
                    return v1.compareTo(v2);
                });
    }
}