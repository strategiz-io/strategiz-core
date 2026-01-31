package io.strategiz.service.marketplace.controller;

import io.strategiz.data.strategy.entity.StrategyCommentEntity;
import io.strategiz.service.marketplace.service.StrategyCommentService;
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
 * Controller for strategy comments.
 *
 * Endpoints: - POST /v1/strategies/{strategyId}/comments - Add comment - GET
 * /v1/strategies/{strategyId}/comments - Get comments - PUT
 * /v1/strategies/{strategyId}/comments/{id} - Edit comment - DELETE
 * /v1/strategies/{strategyId}/comments/{id} - Delete comment - POST
 * /v1/strategies/{strategyId}/comments/{id}/like - Like comment - DELETE
 * /v1/strategies/{strategyId}/comments/{id}/like - Unlike comment - GET
 * /v1/strategies/{strategyId}/comments/{id}/replies - Get replies - POST
 * /v1/strategies/{strategyId}/comments/{id}/replies - Add reply
 *
 * Note: Comments can only be added to published (public) strategies.
 */
@RestController
@RequestMapping("/v1/strategies/{strategyId}/comments")
@RequireAuth(minAcr = "1")
public class StrategyCommentController extends BaseController {

	private static final Logger log = LoggerFactory.getLogger(StrategyCommentController.class);

	@Override
	protected String getModuleName() {
		return "service-marketplace";
	}

	@Autowired
	private StrategyCommentService commentService;

	/**
	 * Add a comment to a strategy. Only works for published (public) strategies.
	 */
	@PostMapping
	@CrossOrigin(origins = { "http://localhost:3000", "http://localhost:3001", "https://strategiz.io" },
			allowedHeaders = "*")
	public ResponseEntity<Object> addComment(@PathVariable String strategyId,
			@RequestBody Map<String, String> requestBody, @AuthUser AuthenticatedUser user) {
		try {
			String userId = user.getUserId();

			String content = requestBody.get("content");
			if (content == null || content.trim().isEmpty()) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(Map.of("error", "Comment content is required"));
			}

			StrategyCommentEntity comment = commentService.addComment(strategyId, userId, content.trim());
			return ResponseEntity.status(HttpStatus.CREATED).body(comment);
		}
		catch (Exception e) {
			log.error("Error adding comment to strategy {}", strategyId, e);
			return handleException(e);
		}
	}

	/**
	 * Get comments for a strategy.
	 */
	@GetMapping
	@CrossOrigin(origins = { "http://localhost:3000", "http://localhost:3001", "https://strategiz.io" },
			allowedHeaders = "*")
	public ResponseEntity<Object> getComments(@PathVariable String strategyId,
			@RequestParam(defaultValue = "50") int limit) {
		try {
			List<StrategyCommentEntity> comments = commentService.getComments(strategyId, limit);
			return ResponseEntity.ok(Map.of("comments", comments, "count", comments.size()));
		}
		catch (Exception e) {
			log.error("Error getting comments for strategy {}", strategyId, e);
			return handleException(e);
		}
	}

	/**
	 * Edit a comment (owner only).
	 */
	@PutMapping("/{commentId}")
	@CrossOrigin(origins = { "http://localhost:3000", "http://localhost:3001", "https://strategiz.io" },
			allowedHeaders = "*")
	public ResponseEntity<Object> editComment(@PathVariable String strategyId, @PathVariable String commentId,
			@RequestBody Map<String, String> requestBody, @AuthUser AuthenticatedUser user) {
		try {
			String userId = user.getUserId();

			String content = requestBody.get("content");
			if (content == null || content.trim().isEmpty()) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(Map.of("error", "Comment content is required"));
			}

			StrategyCommentEntity comment = commentService.editComment(commentId, userId, content.trim());
			return ResponseEntity.ok(comment);
		}
		catch (Exception e) {
			log.error("Error editing comment {} on strategy {}", commentId, strategyId, e);
			return handleException(e);
		}
	}

	/**
	 * Delete a comment (owner or strategy owner only).
	 */
	@DeleteMapping("/{commentId}")
	@CrossOrigin(origins = { "http://localhost:3000", "http://localhost:3001", "https://strategiz.io" },
			allowedHeaders = "*")
	public ResponseEntity<Object> deleteComment(@PathVariable String strategyId, @PathVariable String commentId,
			@AuthUser AuthenticatedUser user) {
		try {
			String userId = user.getUserId();

			commentService.deleteComment(commentId, userId);
			return ResponseEntity.ok(Map.of("message", "Comment deleted successfully"));
		}
		catch (Exception e) {
			log.error("Error deleting comment {} from strategy {}", commentId, strategyId, e);
			return handleException(e);
		}
	}

	/**
	 * Like a comment.
	 */
	@PostMapping("/{commentId}/like")
	@CrossOrigin(origins = { "http://localhost:3000", "http://localhost:3001", "https://strategiz.io" },
			allowedHeaders = "*")
	public ResponseEntity<Object> likeComment(@PathVariable String strategyId, @PathVariable String commentId,
			@AuthUser AuthenticatedUser user) {
		try {
			String userId = user.getUserId();

			commentService.likeComment(commentId, userId);
			return ResponseEntity.ok(Map.of("message", "Comment liked"));
		}
		catch (Exception e) {
			log.error("Error liking comment {}", commentId, e);
			return handleException(e);
		}
	}

	/**
	 * Unlike a comment.
	 */
	@DeleteMapping("/{commentId}/like")
	@CrossOrigin(origins = { "http://localhost:3000", "http://localhost:3001", "https://strategiz.io" },
			allowedHeaders = "*")
	public ResponseEntity<Object> unlikeComment(@PathVariable String strategyId, @PathVariable String commentId,
			@AuthUser AuthenticatedUser user) {
		try {
			String userId = user.getUserId();

			commentService.unlikeComment(commentId, userId);
			return ResponseEntity.ok(Map.of("message", "Comment unliked"));
		}
		catch (Exception e) {
			log.error("Error unliking comment {}", commentId, e);
			return handleException(e);
		}
	}

	/**
	 * Get replies to a comment.
	 */
	@GetMapping("/{commentId}/replies")
	@CrossOrigin(origins = { "http://localhost:3000", "http://localhost:3001", "https://strategiz.io" },
			allowedHeaders = "*")
	public ResponseEntity<Object> getReplies(@PathVariable String strategyId, @PathVariable String commentId) {
		try {
			List<StrategyCommentEntity> replies = commentService.getReplies(commentId);
			return ResponseEntity.ok(Map.of("replies", replies, "count", replies.size()));
		}
		catch (Exception e) {
			log.error("Error getting replies for comment {}", commentId, e);
			return handleException(e);
		}
	}

	/**
	 * Add a reply to a comment.
	 */
	@PostMapping("/{commentId}/replies")
	@CrossOrigin(origins = { "http://localhost:3000", "http://localhost:3001", "https://strategiz.io" },
			allowedHeaders = "*")
	public ResponseEntity<Object> addReply(@PathVariable String strategyId, @PathVariable String commentId,
			@RequestBody Map<String, String> requestBody, @AuthUser AuthenticatedUser user) {
		try {
			String userId = user.getUserId();

			String content = requestBody.get("content");
			if (content == null || content.trim().isEmpty()) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Reply content is required"));
			}

			StrategyCommentEntity reply = commentService.addReply(strategyId, commentId, userId, content.trim());
			return ResponseEntity.status(HttpStatus.CREATED).body(reply);
		}
		catch (Exception e) {
			log.error("Error adding reply to comment {} on strategy {}", commentId, strategyId, e);
			return handleException(e);
		}
	}

	// Helper methods

	private ResponseEntity<Object> handleException(Exception e) {
		String message = e.getMessage();
		if (message != null) {
			if (message.contains("not found")) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", message));
			}
			else if (message.contains("unauthorized") || message.contains("permission")
					|| message.contains("not published")) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", message));
			}
		}
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(Map.of("error", "An error occurred: " + message));
	}

}
