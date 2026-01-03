package io.strategiz.data.user.repository;

import io.strategiz.data.user.entity.UserEntity;
import io.strategiz.data.user.model.UserAggregateData;
import io.strategiz.data.user.model.UserWithAuthMethods;
import io.strategiz.data.user.model.UserWithWatchlist;
import io.strategiz.data.user.model.UserWithProviders;
import io.strategiz.data.user.model.UserWithDevices;
import io.strategiz.data.user.model.UserWithPreferences;
import io.strategiz.data.base.exception.DataRepositoryErrorDetails;
import io.strategiz.data.base.exception.DataRepositoryException;
import io.strategiz.data.base.repository.BaseRepository;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Implementation of UserRepository using BaseRepository
 * Handles CRUD operations for the main user document and aggregation methods
 */
@Repository
public class UserRepositoryImpl extends BaseRepository<UserEntity> implements UserRepository {

    private static final Logger log = LoggerFactory.getLogger(UserRepositoryImpl.class);

    @Autowired
    public UserRepositoryImpl(Firestore firestore) {
        super(firestore, UserEntity.class);
    }

    @Override
    protected String getModuleName() {
        return "data-user";
    }

    // ===============================
    // Main User Document Operations
    // ===============================

    @Override
    public UserEntity createUser(UserEntity user) {
        log.info("=== USER REPOSITORY: createUser START ===");
        log.info("UserRepositoryImpl.createUser - userId before save: [{}]", user.getUserId());
        log.info("UserRepositoryImpl.createUser - user.getId() before save: [{}]", user.getId());
        log.info("UserRepositoryImpl.createUser - email: {}", user.getProfile() != null ? user.getProfile().getEmail() : "null");

        // Use email as createdBy for user entities (since user is creating their own account)
        String createdBy = (user.getProfile() != null && user.getProfile().getEmail() != null)
            ? user.getProfile().getEmail()
            : user.getUserId();
        log.info("UserRepositoryImpl.createUser - createdBy: {}", createdBy);

        // Use forceCreate because UserEntity.getId() returns userId which is typically pre-set.
        // The standard save() would treat this as an update instead of a create.
        log.info("UserRepositoryImpl.createUser - Calling super.forceCreate()");
        UserEntity savedUser = super.forceCreate(user, createdBy);

        log.info("UserRepositoryImpl.createUser - savedUser.getId(): [{}]", savedUser.getId());
        log.info("UserRepositoryImpl.createUser - savedUser.getUserId(): [{}]", savedUser.getUserId());
        log.info("=== USER REPOSITORY: createUser END ===");
        return savedUser;
    }

    @Override
    public Optional<UserEntity> findById(String userId) {
        return super.findById(userId);
    }

    @Override
    public UserEntity updateUser(UserEntity user) {
        // Use email as modifiedBy for user entities
        String modifiedBy = (user.getProfile() != null && user.getProfile().getEmail() != null) 
            ? user.getProfile().getEmail() 
            : user.getUserId();
        return super.save(user, modifiedBy);
    }

    @Override
    public void deleteUser(String userId) {
        super.delete(userId, userId);
    }

    @Override
    public Optional<UserEntity> findByEmail(String email) {
        try {
            Query query = getCollection()
                .whereEqualTo("profile.email", email)
                .whereEqualTo("isActive", true)
                .limit(1);
            
            var documents = query.get().get().getDocuments();
            if (documents.isEmpty()) {
                return Optional.empty();
            }
            
            return Optional.of(documents.get(0).toObject(UserEntity.class));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED, e, "UserEntity", email);
        } catch (ExecutionException e) {
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "UserEntity", email);
        }
    }

    @Override
    public boolean existsById(String userId) {
        return findById(userId).isPresent();
    }

    @Override
    public List<UserEntity> findAll() {
        try {
            Query query = getCollection()
                .whereEqualTo("isActive", true);
            
            return query.get().get().getDocuments().stream()
                .map(doc -> doc.toObject(UserEntity.class))
                .collect(Collectors.toList());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED, e, "UserEntity");
        } catch (ExecutionException e) {
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "UserEntity");
        }
    }

    @Override
    public UserEntity save(UserEntity user) {
        // Use email as audit user for user entities
        String auditUser = (user.getProfile() != null && user.getProfile().getEmail() != null)
            ? user.getProfile().getEmail()
            : user.getUserId();
        return super.save(user, auditUser);
    }

    @Override
    public UserEntity createUserIfEmailNotExists(UserEntity user, String createdBy) {
        log.info("=== USER REPOSITORY: createUserIfEmailNotExists START ===");
        String email = user.getProfile() != null ? user.getProfile().getEmail() : null;

        if (email == null || email.isEmpty()) {
            throw new DataRepositoryException(DataRepositoryErrorDetails.INVALID_ARGUMENT,
                "UserEntity", "User email cannot be null or empty");
        }

        log.info("Attempting atomic user creation for email: {}", email);

        try {
            // Use Firestore transaction for atomic check-and-create
            UserEntity createdUser = firestore.runTransaction(transaction -> {
                // Step 1: Check if user with this email already exists
                Query emailQuery = getCollection()
                    .whereEqualTo("profile.email", email)
                    .whereEqualTo("isActive", true)
                    .limit(1);

                // Note: We can't use transaction.get() with a Query directly,
                // so we execute the query and check results
                var existingDocs = emailQuery.get().get().getDocuments();

                if (!existingDocs.isEmpty()) {
                    log.warn("User with email {} already exists, aborting creation", email);
                    throw new DataRepositoryException(DataRepositoryErrorDetails.DUPLICATE_ENTITY, "UserEntity", email);
                }

                // Step 2: Generate ID if not set
                if (user.getUserId() == null || user.getUserId().isEmpty()) {
                    String newUserId = UUID.randomUUID().toString();
                    user.setUserId(newUserId);
                }

                // Step 3: Initialize audit fields
                if (!user._hasAudit()) {
                    user._initAudit(createdBy);
                }
                user._validate();

                // Step 4: Create the user document within the transaction
                DocumentReference userDocRef = getCollection().document(user.getUserId());
                transaction.set(userDocRef, user);

                log.info("User document created in transaction with ID: {}", user.getUserId());
                return user;
            }).get();

            log.info("=== USER REPOSITORY: createUserIfEmailNotExists SUCCESS - userId: {} ===", createdUser.getUserId());
            return createdUser;

        } catch (ExecutionException e) {
            // Check if this is our custom "duplicate entity" error
            Throwable cause = e.getCause();
            if (cause instanceof DataRepositoryException) {
                throw (DataRepositoryException) cause;
            }
            log.error("Failed to create user atomically: {}", e.getMessage(), e);
            throw new DataRepositoryException(DataRepositoryErrorDetails.ENTITY_SAVE_FAILED, e, "UserEntity", email);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED, e, "UserEntity", email);
        }
    }

    // ===============================
    // Aggregation Operations
    // ===============================

    @Override
    public UserAggregateData getUserWithAllData(String userId) {
        Optional<UserEntity> userOpt = findById(userId);
        if (userOpt.isEmpty()) {
            throw new DataRepositoryException(DataRepositoryErrorDetails.ENTITY_NOT_FOUND, "UserEntity", userId);
        }

        UserAggregateData aggregate = new UserAggregateData();
        aggregate.setUser(userOpt.get());
        // TODO: Implement subcollection fetching
        return aggregate;
    }

    @Override
    public UserWithAuthMethods getUserWithAuthMethods(String userId) {
        Optional<UserEntity> userOpt = findById(userId);
        if (userOpt.isEmpty()) {
            throw new DataRepositoryException(DataRepositoryErrorDetails.ENTITY_NOT_FOUND, "UserEntity", userId);
        }

        UserWithAuthMethods result = new UserWithAuthMethods();
        result.setUser(userOpt.get());
        // TODO: Fetch auth methods from subcollection
        return result;
    }

    @Override
    public UserWithWatchlist getUserWithWatchlist(String userId) {
        Optional<UserEntity> userOpt = findById(userId);
        if (userOpt.isEmpty()) {
            throw new DataRepositoryException(DataRepositoryErrorDetails.ENTITY_NOT_FOUND, "UserEntity", userId);
        }

        UserWithWatchlist result = new UserWithWatchlist();
        result.setUser(userOpt.get());
        // TODO: Fetch watchlist from subcollection
        return result;
    }

    @Override
    public UserWithProviders getUserWithProviders(String userId) {
        Optional<UserEntity> userOpt = findById(userId);
        if (userOpt.isEmpty()) {
            throw new DataRepositoryException(DataRepositoryErrorDetails.ENTITY_NOT_FOUND, "UserEntity", userId);
        }

        UserWithProviders result = new UserWithProviders();
        result.setUser(userOpt.get());
        // TODO: Fetch providers from subcollection
        return result;
    }

    @Override
    public UserWithDevices getUserWithDevices(String userId) {
        Optional<UserEntity> userOpt = findById(userId);
        if (userOpt.isEmpty()) {
            throw new DataRepositoryException(DataRepositoryErrorDetails.ENTITY_NOT_FOUND, "UserEntity", userId);
        }

        UserWithDevices result = new UserWithDevices();
        result.setUser(userOpt.get());
        // TODO: Fetch devices from subcollection
        return result;
    }

    @Override
    public UserWithPreferences getUserWithPreferences(String userId) {
        Optional<UserEntity> userOpt = findById(userId);
        if (userOpt.isEmpty()) {
            throw new DataRepositoryException(DataRepositoryErrorDetails.ENTITY_NOT_FOUND, "UserEntity", userId);
        }

        UserWithPreferences result = new UserWithPreferences();
        result.setUser(userOpt.get());
        // TODO: Fetch preferences from subcollection
        return result;
    }

    // ===============================
    // Watchlist Convenience Methods
    // ===============================

    @Override
    public Object readUserWatchlist(String userId) {
        // TODO: Delegate to watchlist repository
        throw new DataRepositoryException(DataRepositoryErrorDetails.OPERATION_NOT_SUPPORTED,
            "UserEntity", "Watchlist operations should be delegated to data-watchlist module");
    }

    @Override
    public boolean isAssetInWatchlist(String userId, String symbol) {
        // TODO: Delegate to watchlist repository
        return false;
    }

    @Override
    public Object createWatchlistItem(String userId, Object request) {
        // TODO: Delegate to watchlist repository
        throw new DataRepositoryException(DataRepositoryErrorDetails.OPERATION_NOT_SUPPORTED,
            "UserEntity", "Watchlist operations should be delegated to data-watchlist module");
    }

    @Override
    public Object deleteWatchlistItem(String userId, Object request) {
        // TODO: Delegate to watchlist repository
        throw new DataRepositoryException(DataRepositoryErrorDetails.OPERATION_NOT_SUPPORTED,
            "UserEntity", "Watchlist operations should be delegated to data-watchlist module");
    }
}