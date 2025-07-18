package io.strategiz.data.auth.repository.session;

import io.strategiz.data.auth.entity.session.PasetoTokenEntity;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation of PasetoTokenRepository for development
 * TODO: Replace with Firestore implementation
 */
@Repository
public class PasetoTokenRepositoryImpl implements PasetoTokenRepository {
    
    private final ConcurrentHashMap<String, PasetoTokenEntity> storage = new ConcurrentHashMap<>();
    
    @Override
    public <S extends PasetoTokenEntity> S save(S entity) {
        if (entity.getTokenId() == null) {
            entity.setTokenId("token_" + System.currentTimeMillis());
        }
        storage.put(entity.getTokenId(), entity);
        return entity;
    }
    
    @Override
    public <S extends PasetoTokenEntity> Iterable<S> saveAll(Iterable<S> entities) {
        List<S> saved = new ArrayList<>();
        for (S entity : entities) {
            saved.add(save(entity));
        }
        return saved;
    }
    
    @Override
    public Optional<PasetoTokenEntity> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }
    
    @Override
    public boolean existsById(String id) {
        return storage.containsKey(id);
    }
    
    @Override
    public Iterable<PasetoTokenEntity> findAll() {
        return new ArrayList<>(storage.values());
    }
    
    @Override
    public Iterable<PasetoTokenEntity> findAllById(Iterable<String> ids) {
        List<PasetoTokenEntity> result = new ArrayList<>();
        for (String id : ids) {
            storage.computeIfPresent(id, (k, v) -> {
                result.add(v);
                return v;
            });
        }
        return result;
    }
    
    @Override
    public long count() {
        return storage.size();
    }
    
    @Override
    public void deleteById(String id) {
        storage.remove(id);
    }
    
    @Override
    public void delete(PasetoTokenEntity entity) {
        if (entity != null && entity.getTokenId() != null) {
            storage.remove(entity.getTokenId());
        }
    }
    
    @Override
    public void deleteAllById(Iterable<? extends String> ids) {
        for (String id : ids) {
            storage.remove(id);
        }
    }
    
    @Override
    public void deleteAll(Iterable<? extends PasetoTokenEntity> entities) {
        for (PasetoTokenEntity entity : entities) {
            delete(entity);
        }
    }
    
    @Override
    public void deleteAll() {
        storage.clear();
    }
    
    // Custom query methods
    
    @Override
    public Optional<PasetoTokenEntity> findByTokenValue(String tokenValue) {
        return storage.values().stream()
                .filter(token -> tokenValue.equals(token.getTokenValue()))
                .findFirst();
    }
    
    @Override
    public List<PasetoTokenEntity> findByUserId(String userId) {
        return storage.values().stream()
                .filter(token -> userId.equals(token.getUserId()))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<PasetoTokenEntity> findByUserIdAndRevokedFalse(String userId) {
        return storage.values().stream()
                .filter(token -> userId.equals(token.getUserId()) && !token.isRevoked())
                .collect(Collectors.toList());
    }
    
    @Override
    public List<PasetoTokenEntity> findByUserIdAndTokenTypeAndRevokedFalse(String userId, String tokenType) {
        return storage.values().stream()
                .filter(token -> userId.equals(token.getUserId()) 
                        && tokenType.equals(token.getTokenType()) 
                        && !token.isRevoked())
                .collect(Collectors.toList());
    }
    
    @Override
    public List<PasetoTokenEntity> findByDeviceId(String deviceId) {
        return storage.values().stream()
                .filter(token -> deviceId.equals(token.getDeviceId()))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<PasetoTokenEntity> findByRevokedTrue() {
        return storage.values().stream()
                .filter(PasetoTokenEntity::isRevoked)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<PasetoTokenEntity> findByExpiresAtBefore(Instant expiresBefore) {
        return storage.values().stream()
                .filter(token -> token.getExpiresAt() != null && token.getExpiresAt().isBefore(expiresBefore))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<PasetoTokenEntity> findByExpiresAtBeforeOrRevokedTrue(Instant expiresBefore) {
        return storage.values().stream()
                .filter(token -> token.isRevoked() || 
                        (token.getExpiresAt() != null && token.getExpiresAt().isBefore(expiresBefore)))
                .collect(Collectors.toList());
    }
    
    @Override
    public boolean existsByTokenValue(String tokenValue) {
        return storage.values().stream()
                .anyMatch(token -> tokenValue.equals(token.getTokenValue()));
    }
    
    @Override
    public long countByUserIdAndRevokedFalse(String userId) {
        return storage.values().stream()
                .filter(token -> userId.equals(token.getUserId()) && !token.isRevoked())
                .count();
    }
    
    @Override
    public long countByUserIdAndTokenTypeAndRevokedFalse(String userId, String tokenType) {
        return storage.values().stream()
                .filter(token -> userId.equals(token.getUserId()) 
                        && tokenType.equals(token.getTokenType()) 
                        && !token.isRevoked())
                .count();
    }
    
    @Override
    public void deleteByExpiresAtBefore(Instant expiresBefore) {
        List<String> toDelete = storage.values().stream()
                .filter(token -> token.getExpiresAt() != null && token.getExpiresAt().isBefore(expiresBefore))
                .map(PasetoTokenEntity::getTokenId)
                .collect(Collectors.toList());
        toDelete.forEach(storage::remove);
    }
    
    @Override
    public void deleteByRevokedTrue() {
        List<String> toDelete = storage.values().stream()
                .filter(PasetoTokenEntity::isRevoked)
                .map(PasetoTokenEntity::getTokenId)
                .collect(Collectors.toList());
        toDelete.forEach(storage::remove);
    }
    
    @Override
    public void deleteByUserId(String userId) {
        List<String> toDelete = storage.values().stream()
                .filter(token -> userId.equals(token.getUserId()))
                .map(PasetoTokenEntity::getTokenId)
                .collect(Collectors.toList());
        toDelete.forEach(storage::remove);
    }
}