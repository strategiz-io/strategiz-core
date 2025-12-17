package io.strategiz.data.base.repository;

import io.strategiz.data.base.entity.BaseEntity;
import io.strategiz.data.base.exception.DataRepositoryErrorDetails;
import io.strategiz.data.base.exception.DataRepositoryException;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.Firestore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Base repository for entities stored in subcollections.
 * Extends BaseRepository to support documents nested under parent documents.
 * 
 * Example: users/{userId}/authentication_methods/{methodId}
 * 
 * @param <T> Entity type extending BaseEntity
 */
public abstract class SubcollectionRepository<T extends BaseEntity> extends BaseRepository<T> {
    
    private static final Logger log = LoggerFactory.getLogger(SubcollectionRepository.class);
    
    protected SubcollectionRepository(Firestore firestore, Class<T> entityClass) {
        super(firestore, entityClass);
    }
    
    /**
     * Get the parent collection name (e.g., "users")
     */
    protected abstract String getParentCollectionName();
    
    /**
     * Get the subcollection name (e.g., "authentication_methods")
     */
    protected abstract String getSubcollectionName();
    
    /**
     * Get subcollection reference for a specific parent
     * @param parentId The parent document ID
     * @return CollectionReference for the subcollection
     */
    protected CollectionReference getSubcollection(String parentId) {
        log.info("=== SUBCOLLECTION: getSubcollection ===");
        log.info("SubcollectionRepository.getSubcollection - parentId: [{}]", parentId);
        log.info("SubcollectionRepository.getSubcollection - parentCollection: {}", getParentCollectionName());
        log.info("SubcollectionRepository.getSubcollection - subcollection: {}", getSubcollectionName());
        
        // ENHANCED VALIDATION - Add explicit format check
        if (parentId == null || parentId.trim().isEmpty()) {
            throw new DataRepositoryException(DataRepositoryErrorDetails.ENTITY_ID_REQUIRED, getSubcollectionName(), "parentId");
        }
        
        // UUID format validation (most user IDs should be UUIDs)
        boolean isUUID = parentId.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        if (!isUUID) {
            log.warn("CRITICAL: parentId [{}] is NOT in UUID format! This may cause incorrect Firestore paths.", parentId);
            log.warn("Expected format: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx (UUID v4)");
            log.warn("This will create path: {}/{}/{} which may be incorrect!",
                    getParentCollectionName(), parentId, getSubcollectionName());
        }
        
        log.info("SubcollectionRepository.getSubcollection - FULL PATH: {}/{}/{}",
                getParentCollectionName(), parentId, getSubcollectionName());
                
        return firestore.collection(getParentCollectionName())
                .document(parentId)
                .collection(getSubcollectionName());
    }
    
    /**
     * Save entity in a subcollection
     * @param parentId The parent document ID
     * @param entity The entity to save
     * @param userId Who is saving it
     * @return The saved entity
     */
    public T saveInSubcollection(String parentId, T entity, String userId) {
        try {
            validateInputs(entity, userId);
            
            boolean isCreate = (entity.getId() == null || entity.getId().isEmpty());
            
            if (isCreate) {
                entity.setId(getSubcollection(parentId).document().getId());
                entity._initAudit(userId);
            } else {
                entity._updateAudit(userId);
            }
            
            entity._validate();

            getSubcollection(parentId).document(entity.getId()).set(entity).get();

            log.debug("Saved {} in subcollection under parent {} by user {}",
                entityClass.getSimpleName(), parentId, userId);

            return entity;
        } catch (DataRepositoryException e) {
            throw e;
        } catch (Exception e) {
            throw new DataRepositoryException(DataRepositoryErrorDetails.SUBCOLLECTION_ACCESS_FAILED, e, entityClass.getSimpleName(), parentId);
        }
    }

    /**
     * Find entity by ID in a subcollection
     * @param parentId The parent document ID
     * @param entityId The entity ID
     * @return Optional entity
     */
    public Optional<T> findByIdInSubcollection(String parentId, String entityId) {
        try {
            var doc = getSubcollection(parentId).document(entityId).get().get();
            if (doc.exists()) {
                T entity = doc.toObject(entityClass);
                if (entity != null && Boolean.TRUE.equals(entity.getIsActive())) {
                    entity.setId(doc.getId());
                    return Optional.of(entity);
                }
            }
            return Optional.empty();
        } catch (DataRepositoryException e) {
            throw e;
        } catch (Exception e) {
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, entityClass.getSimpleName(), parentId);
        }
    }

    /**
     * Find all entities in a subcollection
     * @param parentId The parent document ID
     * @return List of entities
     */
    public List<T> findAllInSubcollection(String parentId) {
        try {
            var query = getSubcollection(parentId)
                    .whereEqualTo("isActive", true);
                    
            return query.get().get().getDocuments().stream()
                    .map(doc -> {
                        T entity = doc.toObject(entityClass);
                        entity.setId(doc.getId());
                        return entity;
                    })
                    .collect(java.util.stream.Collectors.toList());
        } catch (DataRepositoryException e) {
            throw e;
        } catch (Exception e) {
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, entityClass.getSimpleName(), parentId);
        }
    }

    /**
     * Delete entity in a subcollection (soft delete)
     * @param parentId The parent document ID
     * @param entityId The entity ID
     * @param userId Who is deleting it
     * @return True if deleted
     */
    public boolean deleteInSubcollection(String parentId, String entityId, String userId) {
        Optional<T> optional = findByIdInSubcollection(parentId, entityId);
        if (optional.isPresent()) {
            T entity = optional.get();
            entity._softDelete(userId);
            saveInSubcollection(parentId, entity, userId);
            return true;
        }
        return false;
    }
    
    // Override getCollection to prevent direct usage (must use subcollection methods)
    @Override
    protected CollectionReference getCollection() {
        throw new DataRepositoryException(DataRepositoryErrorDetails.SUBCOLLECTION_ACCESS_FAILED, entityClass.getSimpleName(),
                "Use subcollection-specific methods with parentId");
    }

    // Helper method to validate parent ID
    protected void validateParentId(String parentId) {
        if (parentId == null || parentId.trim().isEmpty()) {
            throw new DataRepositoryException(DataRepositoryErrorDetails.ENTITY_ID_REQUIRED, "parentId");
        }
    }

    private void validateInputs(T entity, String userId) {
        if (entity == null) {
            throw new DataRepositoryException(DataRepositoryErrorDetails.ENTITY_NULL, entityClass.getSimpleName());
        }
        if (userId == null || userId.trim().isEmpty()) {
            throw new DataRepositoryException(DataRepositoryErrorDetails.USER_ID_REQUIRED);
        }
    }

}