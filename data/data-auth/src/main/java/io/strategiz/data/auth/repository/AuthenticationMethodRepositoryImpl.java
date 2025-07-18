package io.strategiz.data.auth.repository;

import io.strategiz.data.auth.entity.AuthenticationMethodEntity;
import io.strategiz.data.auth.entity.totp.TotpAuthenticationMethodEntity;
import io.strategiz.data.base.repository.BaseRepository;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.CollectionReference;
import com.google.api.core.ApiFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Firestore implementation of AuthenticationMethodRepository
 * Provides CRUD operations for authentication methods stored in Firestore
 */
@Repository
public class AuthenticationMethodRepositoryImpl implements AuthenticationMethodRepository {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationMethodRepositoryImpl.class);
    
    private final BaseRepository<AuthenticationMethodEntity> baseRepository;
    private final Firestore firestore;

    @Autowired
    public AuthenticationMethodRepositoryImpl(Firestore firestore) {
        this.firestore = firestore;
        this.baseRepository = new BaseRepository<AuthenticationMethodEntity>(firestore, AuthenticationMethodEntity.class) {};
    }

    // ===============================
    // CrudRepository Implementation
    // ===============================

    @Override
    public <S extends AuthenticationMethodEntity> S save(S entity) {
        // Use a default user ID for system operations, or get from security context
        String userId = "system"; // TODO: Get from SecurityContext when available
        return (S) baseRepository.save(entity, userId);
    }

    @Override
    public <S extends AuthenticationMethodEntity> Iterable<S> saveAll(Iterable<S> entities) {
        return StreamSupport.stream(entities.spliterator(), false)
                .map(this::save)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<AuthenticationMethodEntity> findById(String id) {
        return baseRepository.findById(id);
    }

    @Override
    public boolean existsById(String id) {
        return findById(id).isPresent();
    }

    @Override
    public Iterable<AuthenticationMethodEntity> findAll() {
        return baseRepository.findAll();
    }

    @Override
    public Iterable<AuthenticationMethodEntity> findAllById(Iterable<String> ids) {
        return StreamSupport.stream(ids.spliterator(), false)
                .map(this::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    @Override
    public long count() {
        return baseRepository.findAll().size();
    }

    @Override
    public void deleteById(String id) {
        String userId = "system"; // TODO: Get from SecurityContext when available
        baseRepository.delete(id, userId);
    }

    @Override
    public void delete(AuthenticationMethodEntity entity) {
        deleteById(entity.getId());
    }

    @Override
    public void deleteAllById(Iterable<? extends String> ids) {
        ids.forEach(this::deleteById);
    }

    @Override
    public void deleteAll(Iterable<? extends AuthenticationMethodEntity> entities) {
        entities.forEach(this::delete);
    }

    @Override
    public void deleteAll() {
        List<AuthenticationMethodEntity> all = baseRepository.findAll();
        all.forEach(this::delete);
    }

    // ===============================
    // Custom Query Methods
    // ===============================

    @Override
    public List<AuthenticationMethodEntity> findByType(String type) {
        try {
            Query query = getCollection()
                    .whereEqualTo("type", type)
                    .whereEqualTo("auditFields.isActive", true);
            return executeQuery(query);
        } catch (Exception e) {
            throw new RuntimeException("Failed to find authentication methods by type", e);
        }
    }

    @Override
    public List<AuthenticationMethodEntity> findByName(String name) {
        try {
            Query query = getCollection()
                    .whereEqualTo("name", name)
                    .whereEqualTo("auditFields.isActive", true);
            return executeQuery(query);
        } catch (Exception e) {
            throw new RuntimeException("Failed to find authentication methods by name", e);
        }
    }

    @Override
    public List<AuthenticationMethodEntity> findByNameIgnoreCase(String name) {
        // Firestore doesn't support case-insensitive queries natively
        // We'll need to implement this by filtering in memory
        List<AuthenticationMethodEntity> all = baseRepository.findAll();
        return all.stream()
                .filter(auth -> auth.getName() != null && 
                       auth.getName().toLowerCase().equals(name.toLowerCase()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByType(String type) {
        return !findByType(type).isEmpty();
    }

    @Override
    public boolean existsByName(String name) {
        return !findByName(name).isEmpty();
    }

    @Override
    public long countByType(String type) {
        return findByType(type).size();
    }

    @Override
    public void deleteByType(String type) {
        List<AuthenticationMethodEntity> entities = findByType(type);
        entities.forEach(this::delete);
    }

    @Override
    public List<AuthenticationMethodEntity> findAllByOrderByLastVerifiedAtDesc() {
        try {
            Query query = getCollection()
                    .whereEqualTo("auditFields.isActive", true)
                    .orderBy("lastVerifiedAt", Query.Direction.DESCENDING);
            return executeQuery(query);
        } catch (Exception e) {
            throw new RuntimeException("Failed to find authentication methods ordered by last verified", e);
        }
    }

    @Override
    public List<AuthenticationMethodEntity> findByUserId(String userId) {
        try {
            Query query = getCollection()
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("auditFields.isActive", true);
            return executeQuery(query);
        } catch (Exception e) {
            throw new RuntimeException("Failed to find authentication methods by user ID", e);
        }
    }

    @Override
    public Optional<AuthenticationMethodEntity> findByUserIdAndProviderAndProviderId(String userId, String provider, String providerId) {
        try {
            Query query = getCollection()
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("provider", provider)
                    .whereEqualTo("providerId", providerId)
                    .whereEqualTo("auditFields.isActive", true)
                    .limit(1);
            List<AuthenticationMethodEntity> results = executeQuery(query);
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (Exception e) {
            throw new RuntimeException("Failed to find authentication method by user, provider and provider ID", e);
        }
    }

    @Override
    public List<AuthenticationMethodEntity> findByUserIdAndType(String userId, String type) {
        try {
            Query query = getCollection()
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("type", type)
                    .whereEqualTo("auditFields.isActive", true);
            return executeQuery(query);
        } catch (Exception e) {
            throw new RuntimeException("Failed to find authentication methods by user ID and type", e);
        }
    }

    @Override
    public boolean existsByUserIdAndType(String userId, String type) {
        return !findByUserIdAndType(userId, type).isEmpty();
    }

    @Override
    public void deleteByUserIdAndType(String userId, String type) {
        List<AuthenticationMethodEntity> entities = findByUserIdAndType(userId, type);
        entities.forEach(this::delete);
    }

    @Override
    public List<TotpAuthenticationMethodEntity> findTotpByUserId(String userId) {
        try {
            Query query = getCollection()
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("type", "TOTP")
                    .whereEqualTo("auditFields.isActive", true);
            
            List<AuthenticationMethodEntity> results = executeQuery(query);
            return results.stream()
                    .filter(entity -> entity instanceof TotpAuthenticationMethodEntity)
                    .map(entity -> (TotpAuthenticationMethodEntity) entity)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Failed to find TOTP authentication methods by user ID", e);
        }
    }

    // ===============================
    // Helper Methods
    // ===============================

    protected CollectionReference getCollection() {
        return firestore.collection("authentication_methods");
    }

    private List<AuthenticationMethodEntity> executeQuery(Query query) {
        try {
            ApiFuture<QuerySnapshot> future = query.get();
            QuerySnapshot querySnapshot = future.get();

            return querySnapshot.getDocuments().stream()
                    .map(doc -> {
                        AuthenticationMethodEntity entity = doc.toObject(AuthenticationMethodEntity.class);
                        entity.setId(doc.getId());
                        return entity;
                    })
                    .collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to execute Firestore query", e);
        }
    }
}