package io.strategiz.service.marketplace.service;

import io.strategiz.data.strategy.entity.Strategy;
import io.strategiz.data.strategy.entity.StrategyCommentEntity;
import io.strategiz.data.strategy.repository.CreateStrategyCommentRepository;
import io.strategiz.data.strategy.repository.DeleteStrategyCommentRepository;
import io.strategiz.data.strategy.repository.ReadStrategyCommentRepository;
import io.strategiz.data.strategy.repository.ReadStrategyRepository;
import io.strategiz.data.strategy.repository.UpdateStrategyCommentRepository;
import io.strategiz.data.strategy.repository.UpdateStrategyRepository;
import io.strategiz.data.user.repository.UserRepository;
import io.strategiz.service.marketplace.exception.MarketplaceErrorDetails;
import io.strategiz.framework.exception.StrategizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing strategy comments.
 *
 * Key rules:
 * - Only published (public) strategies can have comments
 * - Any authenticated user can comment
 * - Users can only edit/delete their own comments
 */
@Service
public class StrategyCommentService {

    private static final Logger log = LoggerFactory.getLogger(StrategyCommentService.class);
    private static final String MODULE_NAME = "service-marketplace";

    private final CreateStrategyCommentRepository createCommentRepo;
    private final ReadStrategyCommentRepository readCommentRepo;
    private final UpdateStrategyCommentRepository updateCommentRepo;
    private final DeleteStrategyCommentRepository deleteCommentRepo;
    private final ReadStrategyRepository readStrategyRepo;
    private final UpdateStrategyRepository updateStrategyRepo;
    private final UserRepository userRepository;

    @Autowired
    public StrategyCommentService(
            CreateStrategyCommentRepository createCommentRepo,
            ReadStrategyCommentRepository readCommentRepo,
            UpdateStrategyCommentRepository updateCommentRepo,
            DeleteStrategyCommentRepository deleteCommentRepo,
            ReadStrategyRepository readStrategyRepo,
            UpdateStrategyRepository updateStrategyRepo,
            UserRepository userRepository) {
        this.createCommentRepo = createCommentRepo;
        this.readCommentRepo = readCommentRepo;
        this.updateCommentRepo = updateCommentRepo;
        this.deleteCommentRepo = deleteCommentRepo;
        this.readStrategyRepo = readStrategyRepo;
        this.updateStrategyRepo = updateStrategyRepo;
        this.userRepository = userRepository;
    }

    /**
     * Add a comment to a strategy.
     * Only works for published (public) strategies.
     */
    public StrategyCommentEntity addComment(String strategyId, String userId, String content) {
        // Validate strategy exists and is published
        Strategy strategy = validateStrategyIsPublished(strategyId);

        // Get user info for denormalization
        String userName = null;
        String userPhotoURL = null;
        try {
            userRepository.findById(userId).ifPresent(user -> {
                // Can't assign to local variables from lambda, so we'll set after
            });
            var userOpt = userRepository.findById(userId);
            if (userOpt.isPresent()) {
                userName = userOpt.get().getProfile().getName();
                userPhotoURL = userOpt.get().getProfile().getPhotoURL();
            }
        } catch (Exception e) {
            log.warn("Could not fetch user info for comment denormalization", e);
        }

        // Create comment
        StrategyCommentEntity comment = new StrategyCommentEntity(strategyId, userId, content);
        comment.setUserName(userName);
        comment.setUserPhotoURL(userPhotoURL);

        StrategyCommentEntity created = createCommentRepo.create(comment, userId);

        // Increment strategy comment count
        strategy.incrementComments();
        updateStrategyRepo.update(strategy.getId(), userId, strategy);

        log.info("User {} added comment {} to strategy {}", userId, created.getId(), strategyId);
        return created;
    }

    /**
     * Add a reply to an existing comment.
     */
    public StrategyCommentEntity addReply(String strategyId, String parentCommentId, String userId, String content) {
        // Validate strategy exists and is published
        Strategy strategy = validateStrategyIsPublished(strategyId);

        // Validate parent comment exists
        StrategyCommentEntity parentComment = readCommentRepo.findById(parentCommentId)
                .orElseThrow(() -> new StrategizException(MarketplaceErrorDetails.COMMENT_NOT_FOUND, MODULE_NAME));

        // Ensure parent belongs to same strategy
        if (!parentComment.getStrategyId().equals(strategyId)) {
            throw new StrategizException(MarketplaceErrorDetails.INVALID_OPERATION, MODULE_NAME);
        }

        // Get user info
        String userName = null;
        String userPhotoURL = null;
        var userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            userName = userOpt.get().getProfile().getName();
            userPhotoURL = userOpt.get().getProfile().getPhotoURL();
        }

        // Create reply
        StrategyCommentEntity reply = new StrategyCommentEntity(strategyId, userId, content, parentCommentId);
        reply.setUserName(userName);
        reply.setUserPhotoURL(userPhotoURL);

        StrategyCommentEntity created = createCommentRepo.createReply(reply, parentCommentId, userId);

        // Increment parent's reply count
        updateCommentRepo.incrementReplies(parentCommentId, userId);

        // Increment strategy comment count
        strategy.incrementComments();
        updateStrategyRepo.update(strategy.getId(), userId, strategy);

        log.info("User {} added reply {} to comment {} on strategy {}", userId, created.getId(), parentCommentId, strategyId);
        return created;
    }

    /**
     * Get comments for a strategy.
     */
    public List<StrategyCommentEntity> getComments(String strategyId, int limit) {
        // Validate strategy exists (doesn't need to be published to view existing comments)
        readStrategyRepo.findById(strategyId)
                .orElseThrow(() -> new StrategizException(MarketplaceErrorDetails.STRATEGY_NOT_FOUND, MODULE_NAME));

        return readCommentRepo.findTopLevelByStrategyId(strategyId, limit);
    }

    /**
     * Get replies to a comment.
     */
    public List<StrategyCommentEntity> getReplies(String commentId) {
        return readCommentRepo.findReplies(commentId);
    }

    /**
     * Edit a comment (owner only).
     */
    public StrategyCommentEntity editComment(String commentId, String userId, String newContent) {
        StrategyCommentEntity comment = readCommentRepo.findById(commentId)
                .orElseThrow(() -> new StrategizException(MarketplaceErrorDetails.COMMENT_NOT_FOUND, MODULE_NAME));

        // Only owner can edit
        if (!comment.getUserId().equals(userId)) {
            throw new StrategizException(MarketplaceErrorDetails.UNAUTHORIZED_UPDATE, MODULE_NAME);
        }

        comment.setContent(newContent);
        comment.markAsEdited();

        log.info("User {} edited comment {}", userId, commentId);
        return updateCommentRepo.update(comment, userId);
    }

    /**
     * Delete a comment (owner or strategy owner only).
     */
    public void deleteComment(String commentId, String userId) {
        StrategyCommentEntity comment = readCommentRepo.findById(commentId)
                .orElseThrow(() -> new StrategizException(MarketplaceErrorDetails.COMMENT_NOT_FOUND, MODULE_NAME));

        // Get strategy to check ownership
        Strategy strategy = readStrategyRepo.findById(comment.getStrategyId())
                .orElseThrow(() -> new StrategizException(MarketplaceErrorDetails.STRATEGY_NOT_FOUND, MODULE_NAME));

        // Allow delete if user is comment owner OR strategy owner
        if (!comment.getUserId().equals(userId) && !strategy.getUserId().equals(userId)) {
            throw new StrategizException(MarketplaceErrorDetails.UNAUTHORIZED_DELETE, MODULE_NAME);
        }

        // If this is a parent comment, we don't delete replies (they become orphaned but still visible)
        deleteCommentRepo.delete(commentId, userId);

        // Decrement strategy comment count
        strategy.decrementComments();
        updateStrategyRepo.update(strategy.getId(), userId, strategy);

        // If this was a reply, decrement parent's reply count
        if (comment.isReply()) {
            updateCommentRepo.decrementReplies(comment.getParentCommentId(), userId);
        }

        log.info("User {} deleted comment {} from strategy {}", userId, commentId, comment.getStrategyId());
    }

    /**
     * Like a comment.
     */
    public void likeComment(String commentId, String userId) {
        if (!readCommentRepo.existsById(commentId)) {
            throw new StrategizException(MarketplaceErrorDetails.COMMENT_NOT_FOUND, MODULE_NAME);
        }
        updateCommentRepo.incrementLikes(commentId, userId);
        log.debug("User {} liked comment {}", userId, commentId);
    }

    /**
     * Unlike a comment.
     */
    public void unlikeComment(String commentId, String userId) {
        if (!readCommentRepo.existsById(commentId)) {
            throw new StrategizException(MarketplaceErrorDetails.COMMENT_NOT_FOUND, MODULE_NAME);
        }
        updateCommentRepo.decrementLikes(commentId, userId);
        log.debug("User {} unliked comment {}", userId, commentId);
    }

    /**
     * Get comment count for a strategy.
     */
    public int getCommentCount(String strategyId) {
        return readCommentRepo.countByStrategyId(strategyId);
    }

    // Private helper methods

    private Strategy validateStrategyIsPublished(String strategyId) {
        Strategy strategy = readStrategyRepo.findById(strategyId)
                .orElseThrow(() -> new StrategizException(MarketplaceErrorDetails.STRATEGY_NOT_FOUND, MODULE_NAME));

        if (!strategy.isPublished()) {
            throw new StrategizException(MarketplaceErrorDetails.STRATEGY_NOT_PUBLISHED, MODULE_NAME);
        }

        return strategy;
    }
}
