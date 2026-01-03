package io.strategiz.data.auth.repository;

import io.strategiz.data.auth.entity.AuthenticationMethodEntity;
import io.strategiz.data.auth.entity.AuthenticationMethodType;
import io.strategiz.data.base.exception.DataRepositoryErrorDetails;
import io.strategiz.data.base.exception.DataRepositoryException;
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
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Firebase implementation of AuthenticationMethodRepository
 * Manages authentication methods in users/{userId}/security subcollection
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
        return "security";
    }

    @Override
    protected String getModuleName() {
        return "data-auth";
    }

    // ===============================
    // Subcollection-specific Methods
    // ===============================

    @Override
    public List<AuthenticationMethodEntity> findByUserId(String userId) {
        try {
            return findAllInSubcollection(userId).stream()
                    .filter(entity -> Boolean.TRUE.equals(entity.getIsActive()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to find authentication methods for user: {}", userId, e);
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "AuthenticationMethodEntity", userId);
        }
    }

    @Override
    public List<AuthenticationMethodEntity> findByUserIdAndType(String userId, AuthenticationMethodType type) {
        try {
            return findAllInSubcollection(userId).stream()
                    .filter(entity -> entity.getAuthenticationMethod() == type)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to find authentication methods for user: {} and type: {}", userId, type, e);
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "AuthenticationMethodEntity", userId);
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
                    .filter(entity -> Boolean.TRUE.equals(entity.getIsActive()) == isEnabled)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to find authentication methods for user: {} with enabled: {}", userId, isEnabled, e);
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "AuthenticationMethodEntity", userId);
        }
    }

    @Override
    public List<AuthenticationMethodEntity> findByUserIdAndTypeAndIsEnabled(String userId, AuthenticationMethodType type, boolean isEnabled) {
        try {
            return findAllInSubcollection(userId).stream()
                    .filter(entity -> entity.getAuthenticationMethod() == type && Boolean.TRUE.equals(entity.getIsActive()) == isEnabled)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to find authentication methods for user: {}, type: {}, enabled: {}", userId, type, isEnabled, e);
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "AuthenticationMethodEntity", userId);
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
        log.info("=== AUTH METHOD REPOSITORY: saveForUser START ===");
        log.info("AuthenticationMethodRepositoryImpl.saveForUser - userId (parent doc): [{}]", userId);
        log.info("AuthenticationMethodRepositoryImpl.saveForUser - entity type: {}", entity.getAuthenticationType());
        log.info("AuthenticationMethodRepositoryImpl.saveForUser - This will create document at: users/{}/security/...", userId);
        // Use the standardized subcollection save method from base class
        // The userId parameter acts as both the parent document ID and the audit user
        AuthenticationMethodEntity saved = saveInSubcollection(userId, entity, userId);
        log.info("AuthenticationMethodRepositoryImpl.saveForUser - Saved auth method with ID: {} under userId: [{}]", saved.getId(), userId);
        log.info("=== AUTH METHOD REPOSITORY: saveForUser END ===");
        return saved;
    }

    @Override
    public void deleteForUser(String userId, String methodId) {
        // Use soft delete from base class
        if (!deleteInSubcollection(userId, methodId, userId)) {
            log.warn("Authentication method {} not found for user {}", methodId, userId);
        }
    }
    
    @Override
    public Optional<AuthenticationMethodEntity> findByPasskeyCredentialId(String credentialId) {
        try {
            log.debug("Searching for passkey with credential ID: {}", credentialId);

            // For passkey authentication, we need to search across all users
            // This requires a collection group query on the security subcollection
            // Note: This requires a composite index in Firestore
            // IMPORTANT: Firestore uses camelCase field names from Java getters, not @PropertyName
            Query query = firestore.collectionGroup("security")
                    .whereEqualTo("authenticationMethod", "PASSKEY")  // Use camelCase, not snake_case!
                    .whereEqualTo("isActive", true)
                    .limit(100);

            ApiFuture<QuerySnapshot> querySnapshot = query.get();
            QuerySnapshot snapshot = querySnapshot.get();

            if (snapshot.isEmpty()) {
                log.debug("No active passkey documents found in collection group query");
                return Optional.empty();
            }

            // Filter documents by credentialId in metadata
            for (com.google.cloud.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                AuthenticationMethodEntity entity = doc.toObject(AuthenticationMethodEntity.class);
                entity.setId(doc.getId());

                // Check if this passkey has the matching credential ID
                String storedCredentialId = entity.getMetadataAsString("credentialId");
                if (credentialId.equals(storedCredentialId)) {
                    // Extract userId from the document path and store it in metadata
                    // Path format: users/{userId}/security/{methodId}
                    String path = doc.getReference().getPath();
                    String[] pathParts = path.split("/");
                    if (pathParts.length >= 2) {
                        String userId = pathParts[1]; // users/{userId}/...
                        entity.putMetadata("userId", userId);
                        log.debug("Found matching passkey for user {}", userId);
                    }
                    return Optional.of(entity);
                }
            }

            log.debug("No passkey found with credential ID: {}", credentialId);
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Failed to find authentication method by credential ID: {}", credentialId, e);
            throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED, e, "AuthenticationMethodEntity", credentialId);
        } catch (ExecutionException e) {
            log.error("Failed to find authentication method by credential ID: {}", credentialId, e);
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "AuthenticationMethodEntity", credentialId);
        }
    }
}