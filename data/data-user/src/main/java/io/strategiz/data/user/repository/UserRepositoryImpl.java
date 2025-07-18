package io.strategiz.data.user.repository;

import io.strategiz.data.user.entity.UserEntity;
import io.strategiz.data.user.model.UserAggregateData;
import io.strategiz.data.user.model.UserWithAuthMethods;
import io.strategiz.data.user.model.UserWithWatchlist;
import io.strategiz.data.user.model.UserWithProviders;
import io.strategiz.data.user.model.UserWithDevices;
import io.strategiz.data.user.model.UserWithPreferences;
import io.strategiz.data.base.repository.BaseRepository;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Implementation of UserRepository using BaseRepository
 * Handles CRUD operations for the main user document and aggregation methods
 */
@Repository
public class UserRepositoryImpl extends BaseRepository<UserEntity> implements UserRepository {

    @Autowired
    public UserRepositoryImpl(Firestore firestore) {
        super(firestore, UserEntity.class);
    }

    // ===============================
    // Main User Document Operations
    // ===============================

    @Override
    public UserEntity createUser(UserEntity user) {
        return super.save(user, user.getUserId());
    }

    @Override
    public Optional<UserEntity> findById(String userId) {
        return super.findById(userId);
    }

    @Override
    public UserEntity updateUser(UserEntity user) {
        return super.save(user, user.getUserId());
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
                .whereEqualTo("auditFields.isActive", true)
                .limit(1);
            
            var documents = query.get().get().getDocuments();
            if (documents.isEmpty()) {
                return Optional.empty();
            }
            
            return Optional.of(documents.get(0).toObject(UserEntity.class));
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error finding user by email: " + email, e);
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
                .whereEqualTo("auditFields.isActive", true);
            
            return query.get().get().getDocuments().stream()
                .map(doc -> doc.toObject(UserEntity.class))
                .collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error finding all users", e);
        }
    }

    @Override
    public UserEntity save(UserEntity user) {
        return super.save(user, user.getUserId());
    }

    // ===============================
    // Aggregation Operations
    // ===============================

    @Override
    public UserAggregateData getUserWithAllData(String userId) {
        Optional<UserEntity> userOpt = findById(userId);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found: " + userId);
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
            throw new RuntimeException("User not found: " + userId);
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
            throw new RuntimeException("User not found: " + userId);
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
            throw new RuntimeException("User not found: " + userId);
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
            throw new RuntimeException("User not found: " + userId);
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
            throw new RuntimeException("User not found: " + userId);
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
        throw new UnsupportedOperationException("Watchlist operations should be delegated to data-watchlist module");
    }

    @Override
    public boolean isAssetInWatchlist(String userId, String symbol) {
        // TODO: Delegate to watchlist repository
        return false;
    }

    @Override
    public Object createWatchlistItem(String userId, Object request) {
        // TODO: Delegate to watchlist repository
        throw new UnsupportedOperationException("Watchlist operations should be delegated to data-watchlist module");
    }

    @Override
    public Object deleteWatchlistItem(String userId, Object request) {
        // TODO: Delegate to watchlist repository
        throw new UnsupportedOperationException("Watchlist operations should be delegated to data-watchlist module");
    }
}