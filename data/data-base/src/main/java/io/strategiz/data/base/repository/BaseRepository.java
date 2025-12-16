package io.strategiz.data.base.repository;

import io.strategiz.data.base.entity.BaseEntity;
import io.strategiz.data.base.annotation.Collection;
import io.strategiz.data.base.transaction.FirestoreTransactionHolder;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Rock solid base repository for all platform entities.
 * 
 * ULTRA SIMPLE USAGE:
 * 1. Extend this class
 * 2. Use save(entity, userId) for create/update
 * 3. Use findById(id) for queries
 * 4. Use delete(id, userId) for soft deletes
 * 
 * Simple synchronous operations - no futures, no complexity.
 * 
 * @param <T> Entity type extending BaseEntity
 */
public abstract class BaseRepository<T extends BaseEntity> {

    private static final Logger log = LoggerFactory.getLogger(BaseRepository.class);

    protected final Firestore firestore;
    protected final Class<T> entityClass;

    protected BaseRepository(Firestore firestore, Class<T> entityClass) {
        this.firestore = firestore;
        this.entityClass = entityClass;
    }

    // === ULTRA SIMPLE PUBLIC API ===

    /**
     * Save entity (create or update) - ONE METHOD FOR EVERYTHING
     * @param entity The entity to save
     * @param userId Who is saving it
     * @return The saved entity
     */
    public T save(T entity, String userId) {
        try {
            validateInputs(entity, userId);
            
            boolean isCreate = (entity.getId() == null || entity.getId().isEmpty());
            
            if (isCreate) {
                return performCreate(entity, userId);
            } else {
                return performUpdate(entity, userId);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to save " + entityClass.getSimpleName() + ": " + e.getMessage(), e);
        }
    }

    /**
     * Find entity by ID (only returns active entities)
     * @param id Entity ID
     * @return Optional entity
     */
    public Optional<T> findById(String id) {
        try {
            DocumentSnapshot doc = getCollection().document(id).get().get();
            if (doc.exists()) {
                T entity = doc.toObject(entityClass);
                if (entity != null && Boolean.TRUE.equals(entity.getIsActive())) {
                    entity.setId(doc.getId());
                    return Optional.of(entity);
                }
            }
            return Optional.empty();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to find entity: " + e.getMessage(), e);
        }
    }

    /**
     * Find all active entities
     * @return List of active entities
     */
    public List<T> findAll() {
        try {
            Query query = getCollection().whereEqualTo("auditFields.isActive", true);
            List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();
            
            return docs.stream()
                .map(doc -> {
                    T entity = doc.toObject(entityClass);
                    entity.setId(doc.getId());
                    return entity;
                })
                .collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to find entities: " + e.getMessage(), e);
        }
    }

    /**
     * Soft delete entity
     * @param id Entity ID to delete
     * @param userId Who is deleting it
     * @return True if entity was found and deleted
     */
    public boolean delete(String id, String userId) {
        Optional<T> optional = findById(id);
        if (optional.isPresent()) {
            T entity = optional.get();
            entity._softDelete(userId);
            save(entity, userId);
            return true;
        }
        return false;
    }

    /**
     * Restore soft-deleted entity  
     * @param id Entity ID to restore
     * @param userId Who is restoring it
     * @return Restored entity if found
     */
    public Optional<T> restore(String id, String userId) {
        Optional<T> optional = findDeletedById(id);
        if (optional.isPresent()) {
            T entity = optional.get();
            entity._restore(userId);
            return Optional.of(save(entity, userId));
        }
        return Optional.empty();
    }

    /**
     * Force create an entity, even if it already has an ID set.
     * Use this when the entity ID is pre-assigned (e.g., UserEntity where getId() returns userId).
     * This bypasses the auto-detection logic in save().
     *
     * @param entity The entity to create
     * @param userId Who is creating it
     * @return The created entity
     */
    public T forceCreate(T entity, String userId) {
        try {
            validateInputs(entity, userId);
            return performCreate(entity, userId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create " + entityClass.getSimpleName() + ": " + e.getMessage(), e);
        }
    }

    /**
     * Check if entity exists and is active
     * @param id Entity ID
     * @return True if exists and active
     */
    public boolean exists(String id) {
        return findById(id).isPresent();
    }

    /**
     * Count active entities
     * @return Count of active entities
     */
    public long count() {
        return findAll().size();
    }

    /**
     * Get entity by ID (throws exception if not found)
     * @param id Entity ID
     * @return The entity
     * @throws RuntimeException if entity not found or inactive
     */
    public T getById(String id) {
        return findById(id).orElseThrow(() -> 
            new RuntimeException(entityClass.getSimpleName() + " not found with ID: " + id));
    }

    // === BULK OPERATIONS ===

    /**
     * Bulk save (create/update multiple entities)
     * @param entities Entities to save
     * @param userId Who is saving them
     * @return Saved entities
     */
    public List<T> saveAll(List<T> entities, String userId) {
        try {
            WriteBatch batch = firestore.batch();
            
            entities.forEach(entity -> {
                validateInputs(entity, userId);
                
                boolean isCreate = (entity.getId() == null || entity.getId().isEmpty());
                
                if (isCreate) {
                    prepareForCreate(entity, userId);
                } else {
                    prepareForUpdate(entity, userId);
                }
                
                DocumentReference docRef = getCollection().document(entity.getId());
                batch.set(docRef, entity);
            });

            batch.commit().get(); // Wait for completion
            
            log.debug("Bulk saved {} entities by user {}", entities.size(), userId);
            return entities;
            
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to bulk save entities: " + e.getMessage(), e);
        }
    }

    /**
     * Delete multiple entities (soft delete)
     * @param ids Entity IDs to delete
     * @param userId Who is deleting them
     * @return Number of entities actually deleted
     */
    public int deleteAll(List<String> ids, String userId) {
        int deleted = 0;
        for (String id : ids) {
            if (delete(id, userId)) {
                deleted++;
            }
        }
        return deleted;
    }

    // === QUERY HELPERS ===

    /**
     * Find entities by a field value
     * @param fieldName Field name to query
     * @param value Field value to match
     * @return List of matching entities
     */
    protected List<T> findByField(String fieldName, Object value) {
        try {
            Query query = getCollection()
                .whereEqualTo(fieldName, value)
                .whereEqualTo("auditFields.isActive", true);
                
            List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();
            
            return docs.stream()
                .map(doc -> {
                    T entity = doc.toObject(entityClass);
                    entity.setId(doc.getId());
                    return entity;
                })
                .collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to query entities: " + e.getMessage(), e);
        }
    }

    /**
     * Find entities created by a specific user
     * @param userId User ID
     * @return List of entities created by the user
     */
    public List<T> findByCreatedBy(String userId) {
        return findByField("auditFields.createdBy", userId);
    }

    /**
     * Check if any entity exists with a field value
     * @param fieldName Field name to check
     * @param value Field value to match
     * @return True if any entity exists with that field value
     */
    protected boolean existsByField(String fieldName, Object value) {
        return !findByField(fieldName, value).isEmpty();
    }

    // === INTERNAL IMPLEMENTATION ===

    private T performCreate(T entity, String userId) throws InterruptedException, ExecutionException {
        prepareForCreate(entity, userId);

        DocumentReference docRef = getCollection().document(entity.getId());

        // Check if we're inside a Firestore transaction
        Transaction transaction = FirestoreTransactionHolder.getTransaction();
        if (transaction != null) {
            // Use transaction for atomic operation
            transaction.set(docRef, entity);
            log.debug("Created {} with ID {} in transaction by user {}", entityClass.getSimpleName(), entity.getId(), userId);
        } else {
            // Normal non-transactional create
            docRef.set(entity).get(); // Wait for completion
            log.debug("Created {} with ID {} by user {}", entityClass.getSimpleName(), entity.getId(), userId);
        }

        return entity;
    }

    private T performUpdate(T entity, String userId) throws InterruptedException, ExecutionException {
        prepareForUpdate(entity, userId);

        DocumentReference docRef = getCollection().document(entity.getId());

        // Check if we're inside a Firestore transaction
        Transaction transaction = FirestoreTransactionHolder.getTransaction();
        if (transaction != null) {
            // Use transaction for atomic operation
            transaction.set(docRef, entity);
            log.debug("Updated {} with ID {} in transaction by user {}", entityClass.getSimpleName(), entity.getId(), userId);
        } else {
            // Normal non-transactional update
            docRef.set(entity).get(); // Wait for completion
            log.debug("Updated {} with ID {} by user {}", entityClass.getSimpleName(), entity.getId(), userId);
        }

        return entity;
    }

    private void prepareForCreate(T entity, String userId) {
        if (entity.getId() == null || entity.getId().isEmpty()) {
            // Generate UUID v4 format ID instead of Firestore's default format
            entity.setId(java.util.UUID.randomUUID().toString());
        }
        
        if (!entity._hasAudit()) {
            entity._initAudit(userId);
        }
        
        entity._validate();
    }

    private void prepareForUpdate(T entity, String userId) {
        if (!entity._hasAudit()) {
            throw new IllegalStateException("Cannot update entity without audit fields");
        }
        
        entity._updateAudit(userId);
        entity._validate();
    }

    private void validateInputs(T entity, String userId) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
    }

    private Optional<T> findDeletedById(String id) {
        try {
            DocumentSnapshot doc = getCollection().document(id).get().get();
            if (doc.exists()) {
                T entity = doc.toObject(entityClass);
                if (entity != null && !Boolean.TRUE.equals(entity.getIsActive())) {
                    entity.setId(doc.getId());
                    return Optional.of(entity);
                }
            }
            return Optional.empty();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to find deleted entity: " + e.getMessage(), e);
        }
    }

    protected CollectionReference getCollection() {
        Collection annotation = entityClass.getAnnotation(Collection.class);
        if (annotation == null) {
            throw new RuntimeException("Entity " + entityClass.getSimpleName() + 
                " must have @Collection annotation");
        }
        return firestore.collection(annotation.value());
    }
    
    /**
     * Get collection reference for user-scoped entities (subcollections under users)
     * @param userId The user ID for the subcollection
     * @return CollectionReference for the user's subcollection
     */
    protected CollectionReference getUserScopedCollection(String userId) {
        Collection annotation = entityClass.getAnnotation(Collection.class);
        if (annotation == null) {
            throw new RuntimeException("Entity " + entityClass.getSimpleName() + 
                " must have @Collection annotation");
        }
        // For user-scoped collections, create under users/{userId}/collection_name
        return firestore.collection("users").document(userId).collection(annotation.value());
    }
}