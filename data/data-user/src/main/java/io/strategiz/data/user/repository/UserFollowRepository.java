package io.strategiz.data.user.repository;

import io.strategiz.data.user.entity.UserFollowEntity;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for user follow operations.
 *
 * Supports queries:
 * - Follow/unfollow a user
 * - Get followers of a user
 * - Get users a user is following
 * - Check if following
 * - Count followers/following
 */
public interface UserFollowRepository {

    // === Create Operations ===

    /**
     * Create a new follow relationship.
     *
     * @param followerId  The user who is following
     * @param followingId The user being followed
     * @param userId      Who is performing this action
     * @return The created follow entity
     */
    UserFollowEntity follow(String followerId, String followingId, String userId);

    // === Read Operations ===

    /**
     * Find a follow relationship by follower and following IDs.
     */
    Optional<UserFollowEntity> findByFollowerAndFollowing(String followerId, String followingId);

    /**
     * Check if a user is following another user.
     */
    boolean isFollowing(String followerId, String followingId);

    /**
     * Get all users that a specific user is following (with pagination).
     *
     * @param followerId The user who is following others
     * @param limit      Maximum number of results
     * @return List of follow entities
     */
    List<UserFollowEntity> getFollowing(String followerId, int limit);

    /**
     * Get all followers of a specific user (with pagination).
     *
     * @param followingId The user being followed
     * @param limit       Maximum number of results
     * @return List of follow entities
     */
    List<UserFollowEntity> getFollowers(String followingId, int limit);

    /**
     * Count how many users a specific user is following.
     */
    int countFollowing(String followerId);

    /**
     * Count how many followers a specific user has.
     */
    int countFollowers(String followingId);

    // === Delete Operations ===

    /**
     * Unfollow a user (soft delete the follow relationship).
     *
     * @param followerId  The user who is unfollowing
     * @param followingId The user being unfollowed
     * @param userId      Who is performing this action
     * @return True if the relationship was found and deleted
     */
    boolean unfollow(String followerId, String followingId, String userId);
}
