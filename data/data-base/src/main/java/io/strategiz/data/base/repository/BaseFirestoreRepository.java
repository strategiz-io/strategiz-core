package io.strategiz.data.base.repository;

import io.strategiz.data.base.audit.AuditableEntity;
import io.strategiz.data.base.audit.AuditEnforcementService;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Base repository class for all Firestore entities that enforces audit field consistency.
 * This class provides common CRUD operations while automatically managing audit fields.
 * 
 * ALL Firestore repositories should extend this class to ensure audit enforcement.
 * 
 * Features:
 * - Automatic audit field management
 * - Soft delete support with filtering
 * - Optimistic locking with version control
 * - Async operations with CompletableFuture
 * - Built-in validation and error handling
 * 
 * @param <T> The entity type extending AuditableEntity
 * @author Strategiz Platform
 * @since 1.0
 */
public abstract class BaseFirestoreRepository<T extends AuditableEntity> {

    private static final Logger log = LoggerFactory.getLogger(BaseFirestoreRepository.class);

    protected final Firestore firestore;
    protected final AuditEnforcementService auditService;
    protected final Class<T> entityClass;

    @Autowired
    public BaseFirestoreRepository(Firestore firestore, AuditEnforcementService auditService, Class<T> entityClass) {
        this.firestore = firestore;
        this.auditService = auditService;
        this.entityClass = entityClass;
    }

    /**
     * Gets the Firestore collection reference for this entity type
     * @return CollectionReference for the entity
     */
    protected CollectionReference getCollection() {
        String collectionName = getCollectionName();
        return firestore.collection(collectionName);
    }

    /**
     * Gets the collection name for this entity type
     * Subclasses can override this if they need custom logic
     * @return The collection name
     */
    protected String getCollectionName() {
        try {
            T instance = entityClass.getDeclaredConstructor().newInstance();
            return instance.getCollectionName();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get collection name for " + entityClass.getSimpleName(), e);
        }
    }

    /**
     * Saves a new entity to Firestore with automatic audit field initialization
     * @param entity The entity to save
     * @param userId The user ID creating the entity
     * @return CompletableFuture with the saved entity
     */
    public CompletableFuture<T> create(T entity, String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Prepare entity for creation (initializes audit fields)
                auditService.prepareForCreate(entity, userId);
                
                // Generate ID if not set
                if (entity.getId() == null || entity.getId().isEmpty()) {
                    entity.setId(getCollection().document().getId());
                }

                // Save to Firestore
                DocumentReference docRef = getCollection().document(entity.getId());
                ApiFuture<WriteResult> future = docRef.set(entity);
                future.get(); // Wait for completion

                log.debug("Created entity {} with ID {} by user {}", 
                         entityClass.getSimpleName(), entity.getId(), userId);
                
                return entity;
            } catch (InterruptedException | ExecutionException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Failed to create " + entityClass.getSimpleName(), e);
            }
        });
    }

    /**
     * Updates an existing entity with automatic audit field management
     * @param entity The entity to update
     * @param userId The user ID updating the entity
     * @return CompletableFuture with the updated entity
     */
    public CompletableFuture<T> update(T entity, String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Prepare entity for update (updates audit fields)
                auditService.prepareForUpdate(entity, userId);

                // Save to Firestore
                DocumentReference docRef = getCollection().document(entity.getId());
                ApiFuture<WriteResult> future = docRef.set(entity);
                future.get(); // Wait for completion

                log.debug("Updated entity {} with ID {} by user {}", 
                         entityClass.getSimpleName(), entity.getId(), userId);
                
                return entity;
            } catch (InterruptedException | ExecutionException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Failed to update " + entityClass.getSimpleName(), e);
            }
        });
    }

    /**
     * Finds an entity by ID, returning only active (non-soft-deleted) entities
     * @param id The entity ID
     * @return CompletableFuture with Optional entity
     */
    public CompletableFuture<Optional<T>> findActiveById(String id) {
        return findById(id).thenApply(optional -> 
            optional.filter(AuditableEntity::isActive)
        );
    }

    /**
     * Finds an entity by ID, including soft-deleted entities
     * @param id The entity ID
     * @return CompletableFuture with Optional entity
     */
    public CompletableFuture<Optional<T>> findById(String id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                DocumentReference docRef = getCollection().document(id);
                ApiFuture<DocumentSnapshot> future = docRef.get();
                DocumentSnapshot document = future.get();

                if (document.exists()) {
                    T entity = document.toObject(entityClass);
                    if (entity != null) {
                        entity.setId(document.getId());
                        return Optional.of(entity);
                    }
                }
                return Optional.empty();
            } catch (InterruptedException | ExecutionException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Failed to find " + entityClass.getSimpleName() + " by ID", e);
            }
        });
    }

    /**
     * Finds all active entities in the collection
     * @return CompletableFuture with list of active entities
     */
    public CompletableFuture<List<T>> findAllActive() {
        return findByActiveStatus(true);
    }

    /**
     * Finds all entities (including soft-deleted)
     * @return CompletableFuture with list of all entities
     */
    public CompletableFuture<List<T>> findAll() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ApiFuture<QuerySnapshot> future = getCollection().get();
                QuerySnapshot querySnapshot = future.get();

                return querySnapshot.getDocuments().stream()
                    .map(doc -> {
                        T entity = doc.toObject(entityClass);
                        entity.setId(doc.getId());
                        return entity;
                    })
                    .collect(Collectors.toList());
            } catch (InterruptedException | ExecutionException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Failed to find all " + entityClass.getSimpleName(), e);
            }
        });
    }

    /**
     * Finds entities by active status
     * @param isActive true for active entities, false for soft-deleted
     * @return CompletableFuture with filtered list
     */
    public CompletableFuture<List<T>> findByActiveStatus(boolean isActive) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Query query = getCollection().whereEqualTo("auditFields.isActive", isActive);
                ApiFuture<QuerySnapshot> future = query.get();
                QuerySnapshot querySnapshot = future.get();

                return querySnapshot.getDocuments().stream()
                    .map(doc -> {
                        T entity = doc.toObject(entityClass);
                        entity.setId(doc.getId());
                        return entity;
                    })
                    .collect(Collectors.toList());
            } catch (InterruptedException | ExecutionException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Failed to find " + entityClass.getSimpleName() + " by active status", e);
            }
        });
    }

    /**
     * Soft deletes an entity
     * @param id The entity ID to soft delete
     * @param userId The user ID performing the deletion
     * @return CompletableFuture with the soft-deleted entity
     */
    public CompletableFuture<Optional<T>> softDelete(String id, String userId) {
        return findById(id).thenCompose(optional -> {
            if (optional.isPresent()) {
                T entity = optional.get();
                auditService.prepareForSoftDelete(entity, userId);
                return update(entity, userId).thenApply(Optional::of);
            }
            return CompletableFuture.completedFuture(Optional.empty());
        });
    }

    /**
     * Restores a soft-deleted entity
     * @param id The entity ID to restore
     * @param userId The user ID performing the restoration
     * @return CompletableFuture with the restored entity
     */
    public CompletableFuture<Optional<T>> restore(String id, String userId) {
        return findById(id).thenCompose(optional -> {
            if (optional.isPresent()) {
                T entity = optional.get();
                if (entity.isSoftDeleted()) {
                    auditService.prepareForRestore(entity, userId);
                    return update(entity, userId).thenApply(Optional::of);
                }
            }
            return CompletableFuture.completedFuture(optional);
        });
    }

    /**
     * Performs hard delete of an entity (permanent removal)
     * Use with caution - this cannot be undone
     * @param id The entity ID to delete
     * @return CompletableFuture indicating completion
     */
    public CompletableFuture<Void> hardDelete(String id) {
        return CompletableFuture.runAsync(() -> {
            try {
                DocumentReference docRef = getCollection().document(id);
                ApiFuture<WriteResult> future = docRef.delete();
                future.get(); // Wait for completion

                log.warn("Hard deleted entity {} with ID {}", entityClass.getSimpleName(), id);
            } catch (InterruptedException | ExecutionException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Failed to hard delete " + entityClass.getSimpleName(), e);
            }
        });
    }

    /**
     * Counts active entities in the collection
     * @return CompletableFuture with count
     */
    public CompletableFuture<Long> countActive() {
        return findAllActive().thenApply(list -> (long) list.size());
    }

    /**
     * Checks if an entity exists and is active
     * @param id The entity ID
     * @return CompletableFuture with boolean result
     */
    public CompletableFuture<Boolean> existsAndActive(String id) {
        return findActiveById(id).thenApply(Optional::isPresent);
    }

    /**
     * Bulk create operation with audit enforcement
     * @param entities The entities to create
     * @param userId The user ID creating the entities
     * @return CompletableFuture with list of created entities
     */
    public CompletableFuture<List<T>> bulkCreate(List<T> entities, String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Prepare all entities for creation
                auditService.prepareForBulkCreate(entities, userId);

                // Set IDs for entities that don't have them
                entities.forEach(entity -> {
                    if (entity.getId() == null || entity.getId().isEmpty()) {
                        entity.setId(getCollection().document().getId());
                    }
                });

                // Use Firestore batch for atomic operation
                WriteBatch batch = firestore.batch();
                entities.forEach(entity -> {
                    DocumentReference docRef = getCollection().document(entity.getId());
                    batch.set(docRef, entity);
                });

                ApiFuture<List<WriteResult>> future = batch.commit();
                future.get(); // Wait for completion

                log.debug("Bulk created {} entities of type {} by user {}", 
                         entities.size(), entityClass.getSimpleName(), userId);

                return entities;
            } catch (InterruptedException | ExecutionException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Failed to bulk create " + entityClass.getSimpleName(), e);
            }
        });
    }
}