package io.strategiz.client.firebase.repository.auth;

import io.strategiz.data.auth.entity.AuthenticationMethodEntity;
import io.strategiz.data.auth.entity.AuthenticationMethodType;
import io.strategiz.data.auth.repository.AuthenticationMethodRepository;
import io.strategiz.data.base.repository.SubcollectionRepository;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.api.core.ApiFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Firebase implementation of AuthenticationMethodRepository
 * Manages authentication methods in users/{userId}/authentication_methods subcollection
 */
@Repository
public class AuthenticationMethodRepositoryImpl extends SubcollectionRepository<AuthenticationMethodEntity> 
    implements AuthenticationMethodRepository {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationMethodRepositoryImpl.class);
    
    @Autowired
    public AuthenticationMethodRepositoryImpl(Firestore firestore) {
        super(firestore, AuthenticationMethodEntity.class);
    }

    @Override
    protected String getParentCollectionName() {
        return "users";
    }

    @Override
    protected String getSubcollectionName() {
        return "authentication_methods";
    }

    // ===============================
    // Subcollection-specific Methods
    // ===============================

    @Override
    public List<AuthenticationMethodEntity> findByUserId(String userId) {
        try {
            return findAllInSubcollection(userId).stream()
                    .filter(AuthenticationMethodEntity::isEnabled)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to find authentication methods for user: {}", userId, e);
            throw new RuntimeException("Failed to find authentication methods by user ID", e);
        }
    }

    @Override
    public List<AuthenticationMethodEntity> findByUserIdAndType(String userId, AuthenticationMethodType type) {
        try {
            return findAllInSubcollection(userId).stream()
                    .filter(entity -> entity.getType() == type && entity.isEnabled())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to find authentication methods for user: {} and type: {}", userId, type, e);
            throw new RuntimeException("Failed to find authentication methods by user ID and type", e);
        }
    }

    @Override
    public Optional<AuthenticationMethodEntity> findByUserIdAndId(String userId, String methodId) {
        return findByIdInSubcollection(userId, methodId);
    }

    @Override
    public List<AuthenticationMethodEntity> findByUserIdAndIsEnabled(String userId, boolean isEnabled) {
        try {
            return findAllInSubcollection(userId).stream()
                    .filter(entity -> entity.isEnabled() == isEnabled)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to find authentication methods for user: {} with enabled: {}", userId, isEnabled, e);
            throw new RuntimeException("Failed to find authentication methods by user ID and enabled status", e);
        }
    }

    @Override
    public List<AuthenticationMethodEntity> findByUserIdAndTypeAndIsEnabled(String userId, AuthenticationMethodType type, boolean isEnabled) {
        try {
            return findAllInSubcollection(userId).stream()
                    .filter(entity -> entity.getType() == type && entity.isEnabled() == isEnabled)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to find authentication methods for user: {}, type: {}, enabled: {}", userId, type, isEnabled, e);
            throw new RuntimeException("Failed to find authentication methods by user ID, type and enabled status", e);
        }
    }

    @Override
    public boolean existsByUserIdAndType(String userId, AuthenticationMethodType type) {
        return !findByUserIdAndType(userId, type).isEmpty();
    }

    @Override
    public boolean existsByUserIdAndTypeAndIsEnabled(String userId, AuthenticationMethodType type, boolean isEnabled) {
        return !findByUserIdAndTypeAndIsEnabled(userId, type, isEnabled).isEmpty();
    }

    @Override
    public long countByUserIdAndType(String userId, AuthenticationMethodType type) {
        return findByUserIdAndType(userId, type).size();
    }

    @Override
    public void deleteByUserIdAndType(String userId, AuthenticationMethodType type) {
        List<AuthenticationMethodEntity> entities = findByUserIdAndType(userId, type);
        entities.forEach(entity -> deleteForUser(userId, entity.getId()));
    }

    @Override
    public AuthenticationMethodEntity saveForUser(String userId, AuthenticationMethodEntity entity) {
        // Use the standardized subcollection save method from base class
        // The userId parameter acts as both the parent document ID and the audit user
        return saveInSubcollection(userId, entity, userId);
    }

    @Override
    public void deleteForUser(String userId, String methodId) {
        // Use soft delete from base class
        if (!deleteInSubcollection(userId, methodId, userId)) {
            log.warn("Authentication method {} not found for user {}", methodId, userId);
        }
    }
}