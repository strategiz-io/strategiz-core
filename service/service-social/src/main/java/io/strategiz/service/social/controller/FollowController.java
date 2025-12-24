package io.strategiz.service.social.controller;

import io.strategiz.data.user.entity.UserFollowEntity;
import io.strategiz.service.social.service.FollowService;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.framework.authorization.annotation.RequireAuth;
import io.strategiz.framework.authorization.annotation.AuthUser;
import io.strategiz.framework.authorization.context.AuthenticatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller for user follow operations.
 *
 * Endpoints:
 * - POST   /v1/social/users/{userId}/follow        - Follow a user
 * - DELETE /v1/social/users/{userId}/follow        - Unfollow a user
 * - GET    /v1/social/users/{userId}/followers     - Get user's followers
 * - GET    /v1/social/users/{userId}/following     - Get who user follows
 * - GET    /v1/social/users/{userId}/follow-status - Check if following
 * - GET    /v1/social/users/{userId}/stats         - Get follower/following counts
 */
@RestController
@RequestMapping("/v1/social/users")
@RequireAuth(minAcr = "1")
public class FollowController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(FollowController.class);

    @Override
    protected String getModuleName() {
        return "service-social";
    }

    @Autowired
    private FollowService followService;

    /**
     * Follow a user.
     */
    @PostMapping("/{userId}/follow")
    @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "https://strategiz.io"}, allowedHeaders = "*")
    public ResponseEntity<Object> followUser(
            @PathVariable String userId,
            @AuthUser AuthenticatedUser user) {
        try {
            String currentUserId = user.getUserId();

            UserFollowEntity follow = followService.follow(currentUserId, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "Successfully followed user",
                    "follow", follow
            ));
        } catch (Exception e) {
            log.error("Error following user {}", userId, e);
            return handleException(e);
        }
    }

    /**
     * Unfollow a user.
     */
    @DeleteMapping("/{userId}/follow")
    @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "https://strategiz.io"}, allowedHeaders = "*")
    public ResponseEntity<Object> unfollowUser(
            @PathVariable String userId,
            @AuthUser AuthenticatedUser user) {
        try {
            String currentUserId = user.getUserId();

            followService.unfollow(currentUserId, userId);
            return ResponseEntity.ok(Map.of("message", "Successfully unfollowed user"));
        } catch (Exception e) {
            log.error("Error unfollowing user {}", userId, e);
            return handleException(e);
        }
    }

    /**
     * Get a user's followers.
     */
    @GetMapping("/{userId}/followers")
    @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "https://strategiz.io"}, allowedHeaders = "*")
    public ResponseEntity<Object> getFollowers(
            @PathVariable String userId,
            @RequestParam(defaultValue = "50") int limit) {
        try {
            List<UserFollowEntity> followers = followService.getFollowers(userId, limit);
            return ResponseEntity.ok(Map.of(
                    "followers", followers,
                    "count", followers.size()
            ));
        } catch (Exception e) {
            log.error("Error getting followers for user {}", userId, e);
            return handleException(e);
        }
    }

    /**
     * Get users that a user is following.
     */
    @GetMapping("/{userId}/following")
    @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "https://strategiz.io"}, allowedHeaders = "*")
    public ResponseEntity<Object> getFollowing(
            @PathVariable String userId,
            @RequestParam(defaultValue = "50") int limit) {
        try {
            List<UserFollowEntity> following = followService.getFollowing(userId, limit);
            return ResponseEntity.ok(Map.of(
                    "following", following,
                    "count", following.size()
            ));
        } catch (Exception e) {
            log.error("Error getting following for user {}", userId, e);
            return handleException(e);
        }
    }

    /**
     * Check if the current user is following a specific user.
     */
    @GetMapping("/{userId}/follow-status")
    @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "https://strategiz.io"}, allowedHeaders = "*")
    public ResponseEntity<Object> getFollowStatus(
            @PathVariable String userId,
            @AuthUser AuthenticatedUser user) {
        try {
            String currentUserId = user.getUserId();

            boolean isFollowing = followService.isFollowing(currentUserId, userId);
            return ResponseEntity.ok(Map.of(
                    "isFollowing", isFollowing,
                    "userId", userId
            ));
        } catch (Exception e) {
            log.error("Error checking follow status for user {}", userId, e);
            return handleException(e);
        }
    }

    /**
     * Get follower and following counts for a user.
     */
    @GetMapping("/{userId}/stats")
    @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "https://strategiz.io"}, allowedHeaders = "*")
    public ResponseEntity<Object> getFollowStats(@PathVariable String userId) {
        try {
            Map<String, Integer> stats = followService.getFollowStats(userId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error getting follow stats for user {}", userId, e);
            return handleException(e);
        }
    }

    // Helper methods

    private ResponseEntity<Object> handleException(Exception e) {
        String message = e.getMessage();
        if (message != null) {
            if (message.contains("not found") || message.contains("NOT_FOLLOWING")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", message));
            } else if (message.contains("cannot follow") || message.contains("CANNOT_FOLLOW_SELF")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", message));
            } else if (message.contains("already following") || message.contains("ALREADY_FOLLOWING")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", message));
            }
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "An error occurred: " + message));
    }
}
