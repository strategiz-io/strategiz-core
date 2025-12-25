package io.strategiz.data.featureflags.repository;

import io.strategiz.data.featureflags.entity.FeatureFlagEntity;
import io.strategiz.data.base.exception.DataRepositoryException;
import io.strategiz.data.base.exception.DataRepositoryErrorDetails;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Firestore implementation of FeatureFlagRepository.
 * Stores flags in system/config/feature_flags collection.
 */
@Repository
public class FeatureFlagRepositoryImpl implements FeatureFlagRepository {

    private static final Logger log = LoggerFactory.getLogger(FeatureFlagRepositoryImpl.class);
    private static final String COLLECTION_PATH = "system/config/feature_flags";

    private final Firestore firestore;

    @Autowired
    public FeatureFlagRepositoryImpl(Firestore firestore) {
        this.firestore = firestore;
        log.info("FeatureFlagRepositoryImpl initialized with collection path: {}", COLLECTION_PATH);
    }

    private CollectionReference getCollection() {
        return firestore.collection(COLLECTION_PATH);
    }

    @Override
    public Optional<FeatureFlagEntity> findById(String flagId) {
        try {
            DocumentSnapshot doc = getCollection().document(flagId).get().get();
            if (doc.exists()) {
                FeatureFlagEntity flag = doc.toObject(FeatureFlagEntity.class);
                if (flag != null) {
                    flag.setFlagId(doc.getId());
                }
                return Optional.ofNullable(flag);
            }
            return Optional.empty();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error finding feature flag by ID: {}", flagId, e);
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    @Override
    public List<FeatureFlagEntity> findAll() {
        try {
            List<FeatureFlagEntity> flags = new ArrayList<>();
            var documents = getCollection().get().get().getDocuments();
            for (DocumentSnapshot doc : documents) {
                FeatureFlagEntity flag = doc.toObject(FeatureFlagEntity.class);
                if (flag != null) {
                    flag.setFlagId(doc.getId());
                    flags.add(flag);
                }
            }
            return flags;
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error finding all feature flags", e);
            Thread.currentThread().interrupt();
            return new ArrayList<>();
        }
    }

    @Override
    public List<FeatureFlagEntity> findByCategory(String category) {
        try {
            List<FeatureFlagEntity> flags = new ArrayList<>();
            Query query = getCollection().whereEqualTo("category", category);
            var documents = query.get().get().getDocuments();
            for (DocumentSnapshot doc : documents) {
                FeatureFlagEntity flag = doc.toObject(FeatureFlagEntity.class);
                if (flag != null) {
                    flag.setFlagId(doc.getId());
                    flags.add(flag);
                }
            }
            return flags;
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error finding feature flags by category: {}", category, e);
            Thread.currentThread().interrupt();
            return new ArrayList<>();
        }
    }

    @Override
    public FeatureFlagEntity save(FeatureFlagEntity flag) {
        try {
            String flagId = flag.getFlagId();
            if (flagId == null || flagId.isEmpty()) {
                throw new DataRepositoryException(DataRepositoryErrorDetails.INVALID_ARGUMENT,
                    "FeatureFlagEntity", "Flag ID is required");
            }

            // Set audit fields using BaseEntity methods
            if (!flag._hasAudit()) {
                flag._initAudit("SYSTEM");
            } else {
                flag._updateAudit("SYSTEM");
            }

            getCollection().document(flagId).set(flag).get();
            log.info("Saved feature flag: {}", flagId);
            return flag;
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error saving feature flag: {}", flag.getFlagId(), e);
            Thread.currentThread().interrupt();
            throw new DataRepositoryException(DataRepositoryErrorDetails.ENTITY_SAVE_FAILED, e, "FeatureFlagEntity");
        }
    }

    @Override
    public void delete(String flagId) {
        try {
            getCollection().document(flagId).delete().get();
            log.info("Deleted feature flag: {}", flagId);
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error deleting feature flag: {}", flagId, e);
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public boolean isEnabled(String flagId) {
        return findById(flagId)
            .map(FeatureFlagEntity::isEnabled)
            .orElse(false); // Default to disabled if flag doesn't exist
    }
}
