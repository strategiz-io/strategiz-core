package io.strategiz.service.console.controller;

import io.strategiz.data.session.entity.SessionEntity;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.console.model.UpdateUserRequest;
import io.strategiz.service.console.model.response.AdminUserResponse;
import io.strategiz.service.console.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin controller for user management.
 */
@RestController
@RequestMapping("/v1/console/users")
@Tag(name = "Admin - Users", description = "User management endpoints for administrators")
public class AdminUserController extends BaseController {

	private static final String MODULE_NAME = "CONSOLE";

	private final AdminUserService adminUserService;

	@Autowired
	public AdminUserController(AdminUserService adminUserService) {
		this.adminUserService = adminUserService;
	}

	@Override
	protected String getModuleName() {
		return MODULE_NAME;
	}

	@GetMapping
	@Operation(summary = "List all users", description = "Returns a paginated list of all users")
	public ResponseEntity<List<AdminUserResponse>> listUsers(
			@Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
			@Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
			HttpServletRequest request) {
		String adminUserId = (String) request.getAttribute("adminUserId");
		logRequest("listUsers", adminUserId, "page=" + page + ", size=" + size);

		List<AdminUserResponse> users = adminUserService.listUsers(page, size);
		return ResponseEntity.ok(users);
	}

	@GetMapping("/{userId}")
	@Operation(summary = "Get user details", description = "Returns details for a specific user")
	public ResponseEntity<AdminUserResponse> getUser(@Parameter(description = "User ID") @PathVariable String userId,
			HttpServletRequest request) {
		String adminUserId = (String) request.getAttribute("adminUserId");
		logRequest("getUser", adminUserId, "targetUserId=" + userId);

		AdminUserResponse user = adminUserService.getUser(userId);
		return ResponseEntity.ok(user);
	}

	@PostMapping("/{userId}/disable")
	@Operation(summary = "Disable user account", description = "Disables a user account and terminates all sessions")
	public ResponseEntity<AdminUserResponse> disableUser(
			@Parameter(description = "User ID") @PathVariable String userId, HttpServletRequest request) {
		String adminUserId = (String) request.getAttribute("adminUserId");
		logRequest("disableUser", adminUserId, "targetUserId=" + userId);

		AdminUserResponse user = adminUserService.disableUser(userId, adminUserId);
		log.info("User {} disabled by admin {}", userId, adminUserId);
		return ResponseEntity.ok(user);
	}

	@PostMapping("/{userId}/enable")
	@Operation(summary = "Enable user account", description = "Re-enables a previously disabled user account")
	public ResponseEntity<AdminUserResponse> enableUser(@Parameter(description = "User ID") @PathVariable String userId,
			HttpServletRequest request) {
		String adminUserId = (String) request.getAttribute("adminUserId");
		logRequest("enableUser", adminUserId, "targetUserId=" + userId);

		AdminUserResponse user = adminUserService.enableUser(userId);
		log.info("User {} enabled by admin {}", userId, adminUserId);
		return ResponseEntity.ok(user);
	}

	@GetMapping("/{userId}/sessions")
	@Operation(summary = "Get user sessions", description = "Returns all active sessions for a specific user")
	public ResponseEntity<List<SessionEntity>> getUserSessions(
			@Parameter(description = "User ID") @PathVariable String userId, HttpServletRequest request) {
		String adminUserId = (String) request.getAttribute("adminUserId");
		logRequest("getUserSessions", adminUserId, "targetUserId=" + userId);

		List<SessionEntity> sessions = adminUserService.getUserSessions(userId);
		return ResponseEntity.ok(sessions);
	}

	@DeleteMapping("/{userId}/sessions/{sessionId}")
	@Operation(summary = "Terminate user session", description = "Terminates a specific session for a user")
	public ResponseEntity<Map<String, String>> terminateSession(
			@Parameter(description = "User ID") @PathVariable String userId,
			@Parameter(description = "Session ID") @PathVariable String sessionId, HttpServletRequest request) {
		String adminUserId = (String) request.getAttribute("adminUserId");
		logRequest("terminateSession", adminUserId, "targetUserId=" + userId + ", sessionId=" + sessionId);

		adminUserService.terminateSession(userId, sessionId);
		log.info("Session {} for user {} terminated by admin {}", sessionId, userId, adminUserId);
		return ResponseEntity.ok(Map.of("message", "Session terminated"));
	}

	@DeleteMapping("/{userId}")
	@Operation(summary = "Delete user account",
			description = "Permanently deletes a user account and all associated data. Returns deleted passkey credential IDs for WebAuthn Signal API.")
	public ResponseEntity<Map<String, Object>> deleteUser(
			@Parameter(description = "User ID") @PathVariable String userId, HttpServletRequest request) {
		String adminUserId = (String) request.getAttribute("adminUserId");
		logRequest("deleteUser", adminUserId, "targetUserId=" + userId);

		List<String> deletedCredentialIds = adminUserService.deleteUser(userId, adminUserId);
		log.warn("User {} permanently deleted by admin {}. {} passkey credentials removed.", userId, adminUserId,
				deletedCredentialIds.size());

		return ResponseEntity.ok(Map.of("message", "User permanently deleted", "deletedPasskeyCredentialIds",
				deletedCredentialIds));
	}

	@PostMapping("/{userId}/role")
	@Operation(summary = "Update user role", description = "Updates the role for a specific user (USER or ADMIN)")
	public ResponseEntity<AdminUserResponse> updateUserRole(
			@Parameter(description = "User ID") @PathVariable String userId, @RequestBody Map<String, String> body,
			HttpServletRequest request) {
		String adminUserId = (String) request.getAttribute("adminUserId");
		String newRole = body.get("role");
		logRequest("updateUserRole", adminUserId, "targetUserId=" + userId + ", newRole=" + newRole);

		AdminUserResponse user = adminUserService.updateUserRole(userId, newRole, adminUserId);
		log.info("User {} role updated to {} by admin {}", userId, newRole, adminUserId);
		return ResponseEntity.ok(user);
	}

	@PutMapping("/{userId}")
	@Operation(summary = "Update user profile", description = "Updates profile fields for a specific user")
	public ResponseEntity<AdminUserResponse> updateUser(@Parameter(description = "User ID") @PathVariable String userId,
			@RequestBody UpdateUserRequest updateRequest, HttpServletRequest request) {
		String adminUserId = (String) request.getAttribute("adminUserId");
		logRequest("updateUser", adminUserId, "targetUserId=" + userId);

		AdminUserResponse user = adminUserService.updateUser(userId, updateRequest, adminUserId);
		log.info("User {} profile updated by admin {}", userId, adminUserId);
		return ResponseEntity.ok(user);
	}

}

