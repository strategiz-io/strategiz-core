package io.strategiz.data.user.repository;

import io.strategiz.data.user.entity.UserFollowEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of UserFollowRepository using Firestore.
 */
@Repository
public class UserFollowRepositoryImpl implements UserFollowRepository {

    private final UserFollowBaseRepository baseRepository;

    @Autowired
    public UserFollowRepositoryImpl(UserFollowBaseRepository baseRepository) {
        this.baseRepository = baseRepository;
    }

    @Override
    public UserFollowEntity follow(String followerId, String followingId, String userId) {
        // Check if relationship already exists
        Optional<UserFollowEntity> existing = baseRepository.findByFollowerAndFollowing(followerId, followingId);
        if (existing.isPresent()) {
            // Already following, return existing
            return existing.get();
        }

        // Create new follow relationship
        UserFollowEntity follow = new UserFollowEntity(followerId, followingId);
        return baseRepository.forceCreate(follow, userId);
    }

    @Override
    public Optional<UserFollowEntity> findByFollowerAndFollowing(String followerId, String followingId) {
        return baseRepository.findByFollowerAndFollowing(followerId, followingId);
    }

    @Override
    public boolean isFollowing(String followerId, String followingId) {
        return baseRepository.isFollowing(followerId, followingId);
    }

    @Override
    public List<UserFollowEntity> getFollowing(String followerId, int limit) {
        return baseRepository.findFollowingOrderByDate(followerId, limit);
    }

    @Override
    public List<UserFollowEntity> getFollowers(String followingId, int limit) {
        return baseRepository.findFollowersOrderByDate(followingId, limit);
    }

    @Override
    public int countFollowing(String followerId) {
        return baseRepository.countFollowing(followerId);
    }

    @Override
    public int countFollowers(String followingId) {
        return baseRepository.countFollowers(followingId);
    }

    @Override
    public boolean unfollow(String followerId, String followingId, String userId) {
        String id = UserFollowEntity.generateId(followerId, followingId);
        return baseRepository.delete(id, userId);
    }
}
