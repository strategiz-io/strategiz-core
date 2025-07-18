package io.strategiz.data.session.repository;

import io.strategiz.data.session.entity.UserSessionEntity;
import io.strategiz.data.base.repository.BaseRepository;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Implementation of SessionRepository using BaseRepository
 * Implements Spring Data method naming conventions
 */
@Repository
public class SessionRepositoryImpl extends BaseRepository<UserSessionEntity> implements SessionRepository {

    @Autowired
    public SessionRepositoryImpl(Firestore firestore) {
        super(firestore, UserSessionEntity.class);
    }

    // Spring Data Repository methods

    @Override
    public UserSessionEntity save(UserSessionEntity session, String userId) {
        // Use the BaseRepository save method
        return super.save(session, userId);
    }

    @Override
    public Optional<UserSessionEntity> findById(String sessionId) {
        // Use the BaseRepository findById method
        return super.findById(sessionId);
    }

    @Override
    public List<UserSessionEntity> findByUserIdAndActive(String userId, boolean active) {
        try {
            Query query = getCollection()
                .whereEqualTo("userId", userId)
                .whereEqualTo("active", active)
                .whereEqualTo("auditFields.isActive", true);
            
            List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();
            
            return docs.stream()
                .map(doc -> {
                    UserSessionEntity session = doc.toObject(UserSessionEntity.class);
                    session.setId(doc.getId());
                    return session;
                })
                .collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to find sessions by userId and active: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<UserSessionEntity> findBySessionIdAndActive(String sessionId, boolean active) {
        Optional<UserSessionEntity> sessionOpt = findById(sessionId);
        if (sessionOpt.isPresent()) {
            UserSessionEntity session = sessionOpt.get();
            if (session.isActive() == active && session.isValid()) {
                return Optional.of(session);
            }
        }
        return Optional.empty();
    }

    @Override
    public List<UserSessionEntity> findByUserIdAndDeviceFingerprint(String userId, String deviceFingerprint) {
        try {
            Query query = getCollection()
                .whereEqualTo("userId", userId)
                .whereEqualTo("deviceFingerprint", deviceFingerprint)
                .whereEqualTo("auditFields.isActive", true);
            
            List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();
            
            return docs.stream()
                .map(doc -> {
                    UserSessionEntity session = doc.toObject(UserSessionEntity.class);
                    session.setId(doc.getId());
                    return session;
                })
                .collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to find sessions by device fingerprint: " + e.getMessage(), e);
        }
    }

    @Override
    public List<UserSessionEntity> findByIpAddress(String ipAddress) {
        try {
            Query query = getCollection()
                .whereEqualTo("ipAddress", ipAddress)
                .whereEqualTo("auditFields.isActive", true);
            
            List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();
            
            return docs.stream()
                .map(doc -> {
                    UserSessionEntity session = doc.toObject(UserSessionEntity.class);
                    session.setId(doc.getId());
                    return session;
                })
                .collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to find sessions by IP address: " + e.getMessage(), e);
        }
    }

    @Override
    public List<UserSessionEntity> findByExpiresAtBefore(Instant expiresAt) {
        try {
            Query query = getCollection()
                .whereLessThan("expiresAt", expiresAt)
                .whereEqualTo("auditFields.isActive", true);
            
            List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();
            
            return docs.stream()
                .map(doc -> {
                    UserSessionEntity session = doc.toObject(UserSessionEntity.class);
                    session.setId(doc.getId());
                    return session;
                })
                .collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to find expired sessions: " + e.getMessage(), e);
        }
    }

    @Override
    public long countByUserIdAndActive(String userId, boolean active) {
        return findByUserIdAndActive(userId, active).size();
    }

    @Override
    public void deleteById(String sessionId) {
        // Use the BaseRepository delete method (soft delete)
        super.delete(sessionId, "system");
    }

    @Override
    public boolean existsById(String sessionId) {
        // Use the BaseRepository exists method
        return super.exists(sessionId);
    }

    // Custom business logic methods

    @Override
    public int terminateAllSessionsForUser(String userId, String reason) {
        List<UserSessionEntity> activeSessions = findByUserIdAndActive(userId, true);
        int terminated = 0;
        
        for (UserSessionEntity session : activeSessions) {
            session.terminate(reason);
            save(session, userId);
            terminated++;
        }
        
        return terminated;
    }

    @Override
    public int cleanupExpiredSessions(Instant beforeTime) {
        List<UserSessionEntity> expiredSessions = findByExpiresAtBefore(beforeTime);
        int cleaned = 0;
        
        for (UserSessionEntity session : expiredSessions) {
            deleteById(session.getId());
            cleaned++;
        }
        
        return cleaned;
    }
}