package io.strategiz.data.base;

import io.strategiz.data.base.entity.BaseEntity;
import io.strategiz.data.base.repository.BaseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Ultra simple entity manager for common operations.
 * 
 * This makes working with entities brain-dead simple:
 * 
 * entityManager.save(repository, myEntity, userId);
 * entityManager.findById(repository, id);
 * entityManager.delete(repository, id, userId);
 * 
 * No futures, no complexity - just simple synchronous operations.
 */
@Component
public class EntityManager {

    private static final Logger log = LoggerFactory.getLogger(EntityManager.class);

    /**
     * Save any entity (create or update)
     * @param repository The repository for this entity type
     * @param entity The entity to save
     * @param userId Who is saving it
     * @return The saved entity
     */
    public <T extends BaseEntity> T save(BaseRepository<T> repository, T entity, String userId) {
        log.debug("Saving {} by user {}", entity.getClass().getSimpleName(), userId);
        return repository.save(entity, userId);
    }

    /**
     * Find entity by ID
     * @param repository The repository for this entity type
     * @param id Entity ID
     * @return Optional entity
     */
    public <T extends BaseEntity> Optional<T> findById(BaseRepository<T> repository, String id) {
        log.debug("Finding entity with ID {}", id);
        return repository.findById(id);
    }

    /**
     * Get entity by ID (throws exception if not found)
     * @param repository The repository for this entity type
     * @param id Entity ID
     * @return The entity
     * @throws RuntimeException if entity not found
     */
    public <T extends BaseEntity> T getById(BaseRepository<T> repository, String id) {
        log.debug("Getting entity with ID {}", id);
        return repository.getById(id);
    }

    /**
     * Find all entities of a type
     * @param repository The repository for this entity type
     * @return List of entities
     */
    public <T extends BaseEntity> List<T> findAll(BaseRepository<T> repository) {
        log.debug("Finding all entities");
        return repository.findAll();
    }

    /**
     * Delete entity (soft delete)
     * @param repository The repository for this entity type
     * @param id Entity ID to delete
     * @param userId Who is deleting it
     * @return True if entity was found and deleted
     */
    public <T extends BaseEntity> boolean delete(BaseRepository<T> repository, String id, String userId) {
        log.debug("Deleting entity with ID {} by user {}", id, userId);
        return repository.delete(id, userId);
    }

    /**
     * Check if entity exists
     * @param repository The repository for this entity type
     * @param id Entity ID
     * @return True if exists and active
     */
    public <T extends BaseEntity> boolean exists(BaseRepository<T> repository, String id) {
        return repository.exists(id);
    }

    /**
     * Count entities
     * @param repository The repository for this entity type
     * @return Count of active entities
     */
    public <T extends BaseEntity> long count(BaseRepository<T> repository) {
        return repository.count();
    }

    /**
     * Bulk save entities
     * @param repository The repository for this entity type
     * @param entities Entities to save
     * @param userId Who is saving them
     * @return Saved entities
     */
    public <T extends BaseEntity> List<T> saveAll(BaseRepository<T> repository, List<T> entities, String userId) {
        log.debug("Bulk saving {} entities by user {}", entities.size(), userId);
        return repository.saveAll(entities, userId);
    }

    /**
     * Restore deleted entity
     * @param repository The repository for this entity type
     * @param id Entity ID to restore
     * @param userId Who is restoring it
     * @return Restored entity if found
     */
    public <T extends BaseEntity> Optional<T> restore(BaseRepository<T> repository, String id, String userId) {
        log.debug("Restoring entity with ID {} by user {}", id, userId);
        return repository.restore(id, userId);
    }

    /**
     * Delete multiple entities
     * @param repository The repository for this entity type
     * @param ids Entity IDs to delete
     * @param userId Who is deleting them
     * @return Number of entities actually deleted
     */
    public <T extends BaseEntity> int deleteAll(BaseRepository<T> repository, List<String> ids, String userId) {
        log.debug("Bulk deleting {} entities by user {}", ids.size(), userId);
        return repository.deleteAll(ids, userId);
    }
}