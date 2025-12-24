package io.strategiz.data.user.repository;

import io.strategiz.data.user.entity.UserEntity;
import io.strategiz.data.user.entity.UserProfileEntity;
import io.strategiz.data.user.model.UserAggregateData;
import io.strategiz.data.user.model.UserWithAuthMethods;
import io.strategiz.data.user.model.UserWithWatchlist;
import io.strategiz.data.user.model.UserWithProviders;
import io.strategiz.data.user.model.UserWithDevices;
import io.strategiz.data.user.model.UserWithPreferences;

import java.util.List;
import java.util.Optional;

/**
 * Repository for users collection - main user document operations and aggregation
 * 
 * This repository handles:
 * 1. CRUD operations for the main users/{userId} document (containing profile)
 * 2. Aggregation methods to fetch user data with subcollections
 * 
 * Subcollection operations are delegated to specific data modules:
 * - data-auth: Authentication methods subcollection
 * - data-watchlist: Watchlist subcollection
 * - data-provider: Connected providers subcollection
 * - data-device: User devices subcollection
 * - data-preferences: User preferences subcollections
 */
public interface UserRepository {
    
    // ===============================
    // Main User Document Operations
    // ===============================
    
    /**
     * Create new user
     */
    UserEntity createUser(UserEntity user);
    
    /**
     * Get user by ID (main document only)
     */
    Optional<UserEntity> findById(String userId);
    
    /**
     * Update user (main document only)
     */
    UserEntity updateUser(UserEntity user);
    
    /**
     * Soft delete user
     */
    void deleteUser(String userId);
    
    /**
     * Get user by email
     */
    Optional<UserEntity> findByEmail(String email);
    
    /**
     * Check if user exists by ID
     */
    boolean existsById(String userId);
    
    /**
     * Get all users (admin function)
     */
    List<UserEntity> findAll();
    
    /**
     * Save user (create or update)
     */
    UserEntity save(UserEntity user);
    
    // ===============================
    // Aggregation Operations
    // ===============================
    
    /**
     * Get user with all data from subcollections
     * Uses other data repositories to aggregate complete user data
     */
    UserAggregateData getUserWithAllData(String userId);
    
    /**
     * Get user with authentication methods
     * Delegates to data-auth repository
     */
    UserWithAuthMethods getUserWithAuthMethods(String userId);
    
    /**
     * Get user with watchlist
     * Delegates to data-watchlist repository  
     */
    UserWithWatchlist getUserWithWatchlist(String userId);
    
    /**
     * Get user with connected providers
     * Delegates to data-provider repository
     */
    UserWithProviders getUserWithProviders(String userId);
    
    /**
     * Get user with devices
     * Delegates to data-device repository
     */
    UserWithDevices getUserWithDevices(String userId);
    
    /**
     * Get user with preferences
     * Delegates to data-preferences repository
     */
    UserWithPreferences getUserWithPreferences(String userId);

    // ===============================
    // Watchlist Convenience Methods
    // ===============================

    /**
     * Get user watchlist data
     * Delegates to data-watchlist repository
     */
    Object readUserWatchlist(String userId);

    /**
     * Check if user has asset in watchlist
     * Delegates to data-watchlist repository
     */
    boolean isAssetInWatchlist(String userId, String symbol);

    /**
     * Create watchlist item for user
     * Delegates to data-watchlist repository
     */
    Object createWatchlistItem(String userId, Object request);

    /**
     * Delete watchlist item for user
     * Delegates to data-watchlist repository
     */
    Object deleteWatchlistItem(String userId, Object request);

    /**
     * Get user by ID (alias for findById for backward compatibility)
     */
    default Object getUserById(String userId) {
        return findById(userId).orElse(null);
    }

    /**
     * Get user by email (alias for findByEmail for backward compatibility)
     */
    default Optional<UserEntity> getUserByEmail(String email) {
        return findByEmail(email);
    }

    /**
     * Atomically create a user only if no user with the same email exists.
     * Uses Firestore transactions to prevent race conditions and duplicate users.
     *
     * @param user The user entity to create
     * @param createdBy Who is creating the user (typically the email)
     * @return The created user
     * @throws RuntimeException if a user with the same email already exists
     */
    UserEntity createUserIfEmailNotExists(UserEntity user, String createdBy);
}