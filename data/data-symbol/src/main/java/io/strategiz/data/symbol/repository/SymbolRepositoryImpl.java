package io.strategiz.data.symbol.repository;

import com.google.cloud.firestore.*;
import io.strategiz.data.base.repository.BaseRepository;
import io.strategiz.data.symbol.entity.SymbolEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Firestore implementation of SymbolRepository extending BaseRepository.
 * Symbols are system-wide (not user-scoped), so we use "SYSTEM" as the userId.
 */
@Repository
public class SymbolRepositoryImpl extends BaseRepository<SymbolEntity> implements SymbolRepository {

    private static final Logger log = LoggerFactory.getLogger(SymbolRepositoryImpl.class);
    private static final String SYSTEM_USER = "SYSTEM";
    private static final int BATCH_SIZE = 500;

    @Autowired
    public SymbolRepositoryImpl(Firestore firestore) {
        super(firestore, SymbolEntity.class);
    }

    @Override
    public SymbolEntity save(SymbolEntity entity) {
        // Ensure ID is uppercase canonical symbol
        if (entity.getId() != null) {
            entity.setId(entity.getId().toUpperCase());
        }
        return super.save(entity, SYSTEM_USER);
    }

    @Override
    public List<SymbolEntity> saveAll(List<SymbolEntity> entities) {
        if (entities.isEmpty()) {
            return entities;
        }

        try {
            List<SymbolEntity> savedEntities = new ArrayList<>();

            // Process in batches (Firestore limit is 500 per batch)
            for (int i = 0; i < entities.size(); i += BATCH_SIZE) {
                int endIndex = Math.min(i + BATCH_SIZE, entities.size());
                List<SymbolEntity> batch = entities.subList(i, endIndex);

                WriteBatch writeBatch = firestore.batch();

                for (SymbolEntity entity : batch) {
                    // Ensure ID is uppercase
                    if (entity.getId() != null) {
                        entity.setId(entity.getId().toUpperCase());
                    }

                    // Initialize audit fields if needed
                    if (!entity._hasAudit()) {
                        entity._initAudit(SYSTEM_USER);
                    } else {
                        entity._updateAudit(SYSTEM_USER);
                    }

                    DocumentReference docRef = getCollection().document(entity.getId());
                    writeBatch.set(docRef, entity);
                }

                // Commit the batch
                writeBatch.commit().get();

                savedEntities.addAll(batch);
                log.info("Saved batch of {} symbol entities", batch.size());
            }

            return savedEntities;

        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            log.error("Error saving batch of symbols", e);
            throw new RuntimeException("Failed to save symbol batch", e);
        }
    }

    @Override
    public Optional<SymbolEntity> findByProviderSymbol(String provider, String providerSymbol) {
        try {
            // Query for symbols where providerSymbols map contains the provider key with matching value
            // Firestore supports querying map fields using dot notation
            String fieldPath = "providerSymbols." + provider.toUpperCase();

            Query query = getCollection()
                .whereEqualTo(fieldPath, providerSymbol)
                .whereEqualTo("isActive", true)
                .limit(1);

            List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();

            if (!docs.isEmpty()) {
                SymbolEntity entity = docs.get(0).toObject(SymbolEntity.class);
                entity.setId(docs.get(0).getId());
                return Optional.of(entity);
            }
            return Optional.empty();

        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            log.error("Error finding symbol by provider symbol: {} {}", provider, providerSymbol, e);
            return Optional.empty();
        }
    }

    @Override
    public List<SymbolEntity> findByAssetType(String assetType) {
        try {
            Query query = getCollection()
                .whereEqualTo("assetType", assetType.toUpperCase())
                .whereEqualTo("isActive", true)
                .orderBy("id");

            List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();

            return docs.stream()
                .map(doc -> {
                    SymbolEntity entity = doc.toObject(SymbolEntity.class);
                    entity.setId(doc.getId());
                    return entity;
                })
                .collect(Collectors.toList());

        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            log.error("Error finding symbols by asset type: {}", assetType, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<SymbolEntity> findActiveForCollection() {
        try {
            Query query = getCollection()
                .whereEqualTo("collectionActive", true)
                .whereEqualTo("status", "ACTIVE")
                .whereEqualTo("isActive", true);

            List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();

            return docs.stream()
                .map(doc -> {
                    SymbolEntity entity = doc.toObject(SymbolEntity.class);
                    entity.setId(doc.getId());
                    return entity;
                })
                .collect(Collectors.toList());

        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            log.error("Error finding active collection symbols", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<SymbolEntity> findActiveForCollectionByDataSource(String dataSource) {
        try {
            Query query = getCollection()
                .whereEqualTo("collectionActive", true)
                .whereEqualTo("status", "ACTIVE")
                .whereEqualTo("primaryDataSource", dataSource.toUpperCase())
                .whereEqualTo("isActive", true);

            List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();

            return docs.stream()
                .map(doc -> {
                    SymbolEntity entity = doc.toObject(SymbolEntity.class);
                    entity.setId(doc.getId());
                    return entity;
                })
                .collect(Collectors.toList());

        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            log.error("Error finding symbols for data source: {}", dataSource, e);
            return Collections.emptyList();
        }
    }

    @Override
    public boolean delete(String canonicalSymbol) {
        return super.delete(canonicalSymbol.toUpperCase(), SYSTEM_USER);
    }

    @Override
    public boolean exists(String canonicalSymbol) {
        return super.exists(canonicalSymbol.toUpperCase());
    }

    @Override
    public List<SymbolEntity> findByCategory(String category) {
        try {
            Query query = getCollection()
                .whereEqualTo("category", category.toLowerCase())
                .whereEqualTo("isActive", true)
                .orderBy("id");

            List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();

            return docs.stream()
                .map(doc -> {
                    SymbolEntity entity = doc.toObject(SymbolEntity.class);
                    entity.setId(doc.getId());
                    return entity;
                })
                .collect(Collectors.toList());

        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            log.error("Error finding symbols by category: {}", category, e);
            return Collections.emptyList();
        }
    }
}
