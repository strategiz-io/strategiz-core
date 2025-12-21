package io.strategiz.service.social.service;

import io.strategiz.data.user.entity.UserFollowEntity;
import io.strategiz.data.user.repository.UserFollowRepository;
import io.strategiz.data.user.repository.UserRepository;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.social.exception.SocialErrorDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Service for managing user follow relationships.
 *
 * Key rules:
 * - Users cannot follow themselves
 * - Follow is one-way (like Twitter)
 * - Duplicate follows are idempotent (no error, returns existing)
 */
@Service
public class FollowService {

    private static final Logger log = LoggerFactory.getLogger(FollowService.class);
    private static final String MODULE_NAME = "service-social";

    private final UserFollowRepository followRepository;
    private final UserRepository userRepository;

    @Autowired
    public FollowService(UserFollowRepository followRepository, UserRepository userRepository) {
        this.followRepository = followRepository;
        this.userRepository = userRepository;
    }

    /**
     * Follow a user.
     *
     * @param followerId  The user who wants to follow
     * @param followingId The user to follow
     * @return The follow entity
     */
    public UserFollowEntity follow(String followerId, String followingId) {
        // Validate: cannot follow yourself
        if (followerId.equals(followingId)) {
            throw new StrategizException(SocialErrorDetails.CANNOT_FOLLOW_SELF, MODULE_NAME);
        }

        // Validate: user being followed exists
        if (!userRepository.existsById(followingId)) {
            throw new StrategizException(SocialErrorDetails.USER_NOT_FOUND, MODULE_NAME);
        }

        // Get user info for denormalization
        String followerName = null;
        String followerPhotoURL = null;
        String followingName = null;
        String followingPhotoURL = null;

        try {
            var followerOpt = userRepository.findById(followerId);
            if (followerOpt.isPresent()) {
                followerName = followerOpt.get().getProfile().getName();
                followerPhotoURL = followerOpt.get().getProfile().getPhotoURL();
            }

            var followingOpt = userRepository.findById(followingId);
            if (followingOpt.isPresent()) {
                followingName = followingOpt.get().getProfile().getName();
                followingPhotoURL = followingOpt.get().getProfile().getPhotoURL();
            }
        } catch (Exception e) {
            log.warn("Could not fetch user info for follow denormalization", e);
        }

        // Create follow relationship
        UserFollowEntity follow = followRepository.follow(followerId, followingId, followerId);

        // Update denormalized fields
        follow.setFollowerName(followerName);
        follow.setFollowerPhotoURL(followerPhotoURL);
        follow.setFollowingName(followingName);
        follow.setFollowingPhotoURL(followingPhotoURL);

        log.info("User {} followed user {}", followerId, followingId);
        return follow;
    }

    /**
     * Unfollow a user.
     *
     * @param followerId  The user who wants to unfollow
     * @param followingId The user to unfollow
     */
    public void unfollow(String followerId, String followingId) {
        boolean deleted = followRepository.unfollow(followerId, followingId, followerId);
        if (!deleted) {
            throw new StrategizException(SocialErrorDetails.NOT_FOLLOWING, MODULE_NAME);
        }
        log.info("User {} unfollowed user {}", followerId, followingId);
    }

    /**
     * Check if a user is following another user.
     */
    public boolean isFollowing(String followerId, String followingId) {
        return followRepository.isFollowing(followerId, followingId);
    }

    /**
     * Get a user's followers (with pagination).
     */
    public List<UserFollowEntity> getFollowers(String userId, int limit) {
        return followRepository.getFollowers(userId, limit);
    }

    /**
     * Get users that a user is following (with pagination).
     */
    public List<UserFollowEntity> getFollowing(String userId, int limit) {
        return followRepository.getFollowing(userId, limit);
    }

    /**
     * Get follower and following counts for a user.
     */
    public Map<String, Integer> getFollowStats(String userId) {
        int followers = followRepository.countFollowers(userId);
        int following = followRepository.countFollowing(userId);
        return Map.of(
                "followers", followers,
                "following", following
        );
    }
}
