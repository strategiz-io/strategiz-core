package io.strategiz.data.auth.repository.passkey.credential;

import io.strategiz.data.auth.entity.passkey.PasskeyCredentialEntity;
import io.strategiz.data.base.exception.DataRepositoryErrorDetails;
import io.strategiz.data.base.exception.DataRepositoryException;
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

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Firestore implementation of PasskeyCredentialRepository
 * Provides CRUD operations for passkey credentials stored in Firestore
 */
@Repository
public class PasskeyCredentialRepositoryImpl implements PasskeyCredentialRepository {

    private static final Logger log = LoggerFactory.getLogger(PasskeyCredentialRepositoryImpl.class);
    
    private final BaseRepository<PasskeyCredentialEntity> baseRepository;
    private final Firestore firestore;

    @Autowired
    public PasskeyCredentialRepositoryImpl(Firestore firestore) {
        this.firestore = firestore;
        this.baseRepository = new BaseRepository<PasskeyCredentialEntity>(firestore, PasskeyCredentialEntity.class) {};
    }

    // ===============================
    // CrudRepository Implementation
    // ===============================

    @Override
    public <S extends PasskeyCredentialEntity> S save(S entity) {
        String userId = "system"; // TODO: Get from SecurityContext when available
        return (S) baseRepository.save(entity, userId);
    }

    @Override
    public <S extends PasskeyCredentialEntity> Iterable<S> saveAll(Iterable<S> entities) {
        return StreamSupport.stream(entities.spliterator(), false)
                .map(this::save)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<PasskeyCredentialEntity> findById(String id) {
        return baseRepository.findById(id);
    }

    @Override
    public boolean existsById(String id) {
        return findById(id).isPresent();
    }

    @Override
    public Iterable<PasskeyCredentialEntity> findAll() {
        return baseRepository.findAll();
    }

    @Override
    public Iterable<PasskeyCredentialEntity> findAllById(Iterable<String> ids) {
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
    public void delete(PasskeyCredentialEntity entity) {
        deleteById(entity.getId());
    }

    @Override
    public void deleteAllById(Iterable<? extends String> ids) {
        ids.forEach(this::deleteById);
    }

    @Override
    public void deleteAll(Iterable<? extends PasskeyCredentialEntity> entities) {
        entities.forEach(this::delete);
    }

    @Override
    public void deleteAll() {
        List<PasskeyCredentialEntity> all = baseRepository.findAll();
        all.forEach(this::delete);
    }

    // ===============================
    // Custom Query Methods
    // ===============================

    @Override
    public Optional<PasskeyCredentialEntity> findByCredentialId(String credentialId) {
        try {
            Query query = getCollection()
                    .whereEqualTo("credentialId", credentialId)
                    .whereEqualTo("isActive", true)
                    .limit(1);
            List<PasskeyCredentialEntity> results = executeQuery(query);
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (DataRepositoryException e) {
            throw e;
        } catch (Exception e) {
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "PasskeyCredentialEntity", credentialId);
        }
    }

    @Override
    public List<PasskeyCredentialEntity> findByUserId(String userId) {
        try {
            Query query = getCollection()
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("isActive", true);
            return executeQuery(query);
        } catch (DataRepositoryException e) {
            throw e;
        } catch (Exception e) {
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "PasskeyCredentialEntity", userId);
        }
    }

    @Override
    public List<PasskeyCredentialEntity> findByDevice(String device) {
        try {
            Query query = getCollection()
                    .whereEqualTo("device", device)
                    .whereEqualTo("isActive", true);
            return executeQuery(query);
        } catch (DataRepositoryException e) {
            throw e;
        } catch (Exception e) {
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "PasskeyCredentialEntity", device);
        }
    }

    @Override
    public List<PasskeyCredentialEntity> findByVerifiedTrue() {
        try {
            Query query = getCollection()
                    .whereEqualTo("verified", true)
                    .whereEqualTo("isActive", true);
            return executeQuery(query);
        } catch (DataRepositoryException e) {
            throw e;
        } catch (Exception e) {
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "PasskeyCredentialEntity");
        }
    }

    @Override
    public List<PasskeyCredentialEntity> findByCreatedAtBefore(Instant before) {
        try {
            Query query = getCollection()
                    .whereLessThan("createdDate", before)
                    .whereEqualTo("isActive", true);
            return executeQuery(query);
        } catch (DataRepositoryException e) {
            throw e;
        } catch (Exception e) {
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "PasskeyCredentialEntity");
        }
    }

    @Override
    public List<PasskeyCredentialEntity> findByLastUsedAtBefore(Instant before) {
        try {
            Query query = getCollection()
                    .whereLessThan("lastUsedAt", before)
                    .whereEqualTo("isActive", true);
            return executeQuery(query);
        } catch (DataRepositoryException e) {
            throw e;
        } catch (Exception e) {
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "PasskeyCredentialEntity");
        }
    }

    @Override
    public long countByUserId(String userId) {
        return findByUserId(userId).size();
    }

    @Override
    public boolean existsByCredentialId(String credentialId) {
        return findByCredentialId(credentialId).isPresent();
    }

    @Override
    public void deleteByUserId(String userId) {
        List<PasskeyCredentialEntity> entities = findByUserId(userId);
        entities.forEach(this::delete);
    }

    // ===============================
    // Helper Methods
    // ===============================

    protected CollectionReference getCollection() {
        return firestore.collection("passkey_credentials");
    }

    private List<PasskeyCredentialEntity> executeQuery(Query query) {
        try {
            ApiFuture<QuerySnapshot> future = query.get();
            QuerySnapshot querySnapshot = future.get();

            return querySnapshot.getDocuments().stream()
                    .map(doc -> {
                        PasskeyCredentialEntity entity = doc.toObject(PasskeyCredentialEntity.class);
                        entity.setId(doc.getId());
                        return entity;
                    })
                    .collect(Collectors.toList());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED, e, "PasskeyCredentialEntity");
        } catch (ExecutionException e) {
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "PasskeyCredentialEntity");
        }
    }
}