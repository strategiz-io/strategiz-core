package io.strategiz.data.session.repository;

import io.strategiz.data.session.entity.SessionEntity;
import io.strategiz.data.base.exception.DataRepositoryErrorDetails;
import io.strategiz.data.base.repository.BaseRepository;
import io.strategiz.framework.exception.StrategizException;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Firestore implementation of SessionRepository
 * Stores sessions in the 'sessions' collection for persistent storage
 * Following SOLID principles - Single Responsibility: Only handles Firestore persistence
 */
@Repository
@Primary
public class FirestoreSessionRepository extends BaseRepository<SessionEntity> implements SessionRepository {

    @Autowired
    public FirestoreSessionRepository(Firestore firestore) {
        super(firestore, SessionEntity.class);
    }

    // ===============================
    // CrudRepository Methods
    // ===============================

    @Override
    public <S extends SessionEntity> S save(S entity) {
        if (entity.getSessionId() == null) {
            entity.setSessionId("sess_" + System.currentTimeMillis() + "_" + System.nanoTime());
        }
        // Save and return the same instance
        SessionEntity saved = super.save(entity, entity.getUserId());
        // Copy the ID back to the original entity
        entity.setSessionId(saved.getSessionId());
        return entity;
    }

    @Override
    public <S extends SessionEntity> Iterable<S> saveAll(Iterable<S> entities) {
        List<S> saved = new java.util.ArrayList<>();
        for (S entity : entities) {
            saved.add(save(entity));
        }
        return saved;
    }

    @Override
    public Optional<SessionEntity> findById(String id) {
        return super.findById(id);
    }

    @Override
    public boolean existsById(String id) {
        return findById(id).isPresent();
    }

    @Override
    public List<SessionEntity> findAll() {
        try {
            Query query = getCollection()
                .whereEqualTo("isActive", true);
            
            List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();
            
            return docs.stream()
                .map(doc -> {
                    SessionEntity session = doc.toObject(SessionEntity.class);
                    session.setSessionId(doc.getId());
                    return session;
                })
                .collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new StrategizException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, "data-session", e, "findAll");
        }
    }

    @Override
    public Iterable<SessionEntity> findAllById(Iterable<String> ids) {
        List<SessionEntity> results = new java.util.ArrayList<>();
        for (String id : ids) {
            findById(id).ifPresent(results::add);
        }
        return results;
    }

    @Override
    public long count() {
        try {
            return getCollection().get().get().size();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new StrategizException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, "data-session", e, "count");
        }
    }

    @Override
    public void deleteById(String id) {
        try {
            getCollection().document(id).delete().get();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new StrategizException(DataRepositoryErrorDetails.ENTITY_DELETE_FAILED, "data-session", e, "SessionEntity", id);
        }
    }

    @Override
    public void delete(SessionEntity entity) {
        if (entity.getSessionId() != null) {
            deleteById(entity.getSessionId());
        }
    }

    @Override
    public void deleteAllById(Iterable<? extends String> ids) {
        for (String id : ids) {
            deleteById(id);
        }
    }

    @Override
    public void deleteAll(Iterable<? extends SessionEntity> entities) {
        for (SessionEntity entity : entities) {
            delete(entity);
        }
    }

    @Override
    public void deleteAll() {
        try {
            List<QueryDocumentSnapshot> docs = getCollection().get().get().getDocuments();
            for (QueryDocumentSnapshot doc : docs) {
                doc.getReference().delete();
            }
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new StrategizException(DataRepositoryErrorDetails.BULK_OPERATION_FAILED, "data-session", e, "deleteAll");
        }
    }

    // ===============================
    // Custom Query Methods
    // ===============================

    @Override
    public Optional<SessionEntity> findByTokenValue(String tokenValue) {
        try {
            Query query = getCollection()
                .whereEqualTo("tokenValue", tokenValue)
                .whereEqualTo("isActive", true)
                .limit(1);
            
            List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();
            
            if (!docs.isEmpty()) {
                SessionEntity session = docs.get(0).toObject(SessionEntity.class);
                session.setSessionId(docs.get(0).getId());
                return Optional.of(session);
            }
            return Optional.empty();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new StrategizException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, "data-session", e, "findByTokenValue");
        }
    }

    @Override
    public List<SessionEntity> findByUserId(String userId) {
        try {
            Query query = getCollection()
                .whereEqualTo("userId", userId)
                .whereEqualTo("isActive", true);
            
            return executeQuery(query);
        } catch (Exception e) {
            throw new StrategizException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, "data-session", e, "findByUserId", userId);
        }
    }

    @Override
    public List<SessionEntity> findByUserIdAndRevokedFalse(String userId) {
        try {
            Query query = getCollection()
                .whereEqualTo("userId", userId)
                .whereEqualTo("revoked", false)
                .whereEqualTo("isActive", true);
            
            return executeQuery(query);
        } catch (Exception e) {
            throw new StrategizException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, "data-session", e, "findByUserIdAndRevokedFalse", userId);
        }
    }

    @Override
    public List<SessionEntity> findByUserIdAndTokenType(String userId, String tokenType) {
        try {
            Query query = getCollection()
                .whereEqualTo("userId", userId)
                .whereEqualTo("tokenType", tokenType)
                .whereEqualTo("isActive", true);
            
            return executeQuery(query);
        } catch (Exception e) {
            throw new StrategizException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, "data-session", e, "findByUserIdAndTokenType", userId, tokenType);
        }
    }

    @Override
    public List<SessionEntity> findByUserIdAndTokenTypeAndRevokedFalse(String userId, String tokenType) {
        try {
            Query query = getCollection()
                .whereEqualTo("userId", userId)
                .whereEqualTo("tokenType", tokenType)
                .whereEqualTo("revoked", false)
                .whereEqualTo("isActive", true);
            
            return executeQuery(query);
        } catch (Exception e) {
            throw new StrategizException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, "data-session", e, "findByUserIdAndTokenTypeAndRevokedFalse", userId, tokenType);
        }
    }

    @Override
    public List<SessionEntity> findByDeviceId(String deviceId) {
        try {
            Query query = getCollection()
                .whereEqualTo("deviceId", deviceId)
                .whereEqualTo("isActive", true);
            
            return executeQuery(query);
        } catch (Exception e) {
            throw new StrategizException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, "data-session", e, "findByDeviceId", deviceId);
        }
    }

    @Override
    public List<SessionEntity> findByDeviceIdAndRevokedFalse(String deviceId) {
        try {
            Query query = getCollection()
                .whereEqualTo("deviceId", deviceId)
                .whereEqualTo("revoked", false)
                .whereEqualTo("isActive", true);
            
            return executeQuery(query);
        } catch (Exception e) {
            throw new StrategizException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, "data-session", e, "findByDeviceIdAndRevokedFalse", deviceId);
        }
    }

    @Override
    public List<SessionEntity> findByExpiresAtBefore(Instant expiresBefore) {
        try {
            Query query = getCollection()
                .whereLessThan("expiresAt", expiresBefore)
                .whereEqualTo("isActive", true);
            
            return executeQuery(query);
        } catch (Exception e) {
            throw new StrategizException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, "data-session", e, "findByExpiresAtBefore");
        }
    }

    @Override
    public List<SessionEntity> findByRevokedTrue() {
        try {
            Query query = getCollection()
                .whereEqualTo("revoked", true)
                .whereEqualTo("isActive", true);
            
            return executeQuery(query);
        } catch (Exception e) {
            throw new StrategizException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, "data-session", e, "findByRevokedTrue");
        }
    }

    @Override
    public List<SessionEntity> findByExpiresAtBeforeOrRevokedTrue(Instant expiresBefore) {
        try {
            // Firestore doesn't support OR queries directly, so we need to run two queries
            List<SessionEntity> expired = findByExpiresAtBefore(expiresBefore);
            List<SessionEntity> revoked = findByRevokedTrue();
            
            // Combine and deduplicate
            java.util.Set<String> sessionIds = new java.util.HashSet<>();
            List<SessionEntity> combined = new java.util.ArrayList<>();
            
            for (SessionEntity session : expired) {
                if (sessionIds.add(session.getSessionId())) {
                    combined.add(session);
                }
            }
            
            for (SessionEntity session : revoked) {
                if (sessionIds.add(session.getSessionId())) {
                    combined.add(session);
                }
            }
            
            return combined;
        } catch (Exception e) {
            throw new StrategizException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, "data-session", e, "findByExpiresAtBeforeOrRevokedTrue");
        }
    }

    @Override
    public boolean existsByTokenValue(String tokenValue) {
        return findByTokenValue(tokenValue).isPresent();
    }

    @Override
    public long countByUserIdAndRevokedFalse(String userId) {
        return findByUserIdAndRevokedFalse(userId).size();
    }

    @Override
    public long countByUserIdAndTokenTypeAndRevokedFalse(String userId, String tokenType) {
        return findByUserIdAndTokenTypeAndRevokedFalse(userId, tokenType).size();
    }

    @Override
    public void deleteByExpiresAtBefore(Instant expiresBefore) {
        List<SessionEntity> toDelete = findByExpiresAtBefore(expiresBefore);
        for (SessionEntity session : toDelete) {
            deleteById(session.getSessionId());
        }
    }

    @Override
    public void deleteByRevokedTrue() {
        List<SessionEntity> toDelete = findByRevokedTrue();
        for (SessionEntity session : toDelete) {
            deleteById(session.getSessionId());
        }
    }

    @Override
    public void deleteByUserId(String userId) {
        List<SessionEntity> toDelete = findByUserId(userId);
        for (SessionEntity session : toDelete) {
            deleteById(session.getSessionId());
        }
    }

    @Override
    public void deleteByDeviceId(String deviceId) {
        List<SessionEntity> toDelete = findByDeviceId(deviceId);
        for (SessionEntity session : toDelete) {
            deleteById(session.getSessionId());
        }
    }

    // ===============================
    // Helper Methods
    // ===============================

    private List<SessionEntity> executeQuery(Query query) {
        try {
            List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();
            
            return docs.stream()
                .map(doc -> {
                    SessionEntity session = doc.toObject(SessionEntity.class);
                    session.setSessionId(doc.getId());
                    return session;
                })
                .collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new StrategizException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, "data-session", e, "executeQuery");
        }
    }
}