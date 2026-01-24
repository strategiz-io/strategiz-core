package io.strategiz.service.console.controller;

import io.strategiz.business.tokenauth.ServiceAccountService;
import io.strategiz.business.tokenauth.ServiceAccountService.ServiceAccountCreateResult;
import io.strategiz.data.preferences.entity.ServiceAccountEntity;
import io.strategiz.service.base.controller.BaseController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin controller for Service Account management.
 *
 * Service accounts enable machine-to-machine authentication for:
 * - CI/CD pipelines (integration testing)
 * - External service integrations
 * - Automated scripts and batch processes
 *
 * Endpoints:
 * - POST   /v1/console/service-accounts         - Create service account
 * - GET    /v1/console/service-accounts         - List all service accounts
 * - GET    /v1/console/service-accounts/{id}    - Get service account by ID
 * - PUT    /v1/console/service-accounts/{id}    - Update service account
 * - DELETE /v1/console/service-accounts/{id}    - Delete service account
 * - POST   /v1/console/service-accounts/{id}/regenerate-secret - Regenerate secret
 */
@RestController
@RequestMapping("/v1/console/service-accounts")
@Tag(name = "Admin - Service Accounts", description = "Service account management for M2M authentication")
public class AdminServiceAccountController extends BaseController {

	private static final String MODULE_NAME = "CONSOLE";

	private final ServiceAccountService serviceAccountService;

	public AdminServiceAccountController(ServiceAccountService serviceAccountService) {
		this.serviceAccountService = serviceAccountService;
	}

	@Override
	protected String getModuleName() {
		return MODULE_NAME;
	}

	/**
	 * Create a new service account.
	 */
	@PostMapping
	@Operation(summary = "Create service account", description = "Create a new service account for M2M authentication. Returns client ID and secret (secret only shown once).")
	public ResponseEntity<Map<String, Object>> createServiceAccount(@RequestBody CreateServiceAccountRequest request,
			HttpServletRequest httpRequest) {

		String adminUserId = (String) httpRequest.getAttribute("adminUserId");
		logRequest("createServiceAccount", adminUserId, "name=" + request.name);

		ServiceAccountCreateResult result = serviceAccountService.createServiceAccount(request.name, request.description,
				request.scopes, adminUserId);

		log.info("Service account created: id={}, clientId={}, by admin={}", result.account().getId(),
				result.account().getClientId(), adminUserId);

		// Return with plaintext secret (only shown once!)
		return ResponseEntity.ok(Map.of("success", true, "id", result.account().getId(), "name",
				result.account().getName(), "clientId", result.account().getClientId(), "clientSecret",
				result.clientSecret(), // IMPORTANT: Only shown once!
				"scopes", result.account().getScopes(), "message",
				"IMPORTANT: Save the clientSecret now. It will not be shown again."));
	}

	/**
	 * List all service accounts.
	 */
	@GetMapping
	@Operation(summary = "List service accounts", description = "Returns all service accounts (without secrets)")
	public ResponseEntity<List<ServiceAccountResponse>> listServiceAccounts(HttpServletRequest httpRequest) {

		String adminUserId = (String) httpRequest.getAttribute("adminUserId");
		logRequest("listServiceAccounts", adminUserId, "");

		List<ServiceAccountEntity> accounts = serviceAccountService.listServiceAccounts();

		List<ServiceAccountResponse> response = accounts.stream().map(this::toResponse).toList();

		return ResponseEntity.ok(response);
	}

	/**
	 * Get service account by ID.
	 */
	@GetMapping("/{id}")
	@Operation(summary = "Get service account", description = "Returns details for a specific service account")
	public ResponseEntity<ServiceAccountResponse> getServiceAccount(
			@Parameter(description = "Service Account ID") @PathVariable String id, HttpServletRequest httpRequest) {

		String adminUserId = (String) httpRequest.getAttribute("adminUserId");
		logRequest("getServiceAccount", adminUserId, "id=" + id);

		ServiceAccountEntity account = serviceAccountService.getServiceAccount(id)
			.orElseThrow(() -> new IllegalArgumentException("Service account not found: " + id));

		return ResponseEntity.ok(toResponse(account));
	}

	/**
	 * Update service account.
	 */
	@PutMapping("/{id}")
	@Operation(summary = "Update service account", description = "Update service account properties (not the secret)")
	public ResponseEntity<ServiceAccountResponse> updateServiceAccount(
			@Parameter(description = "Service Account ID") @PathVariable String id,
			@RequestBody UpdateServiceAccountRequest request, HttpServletRequest httpRequest) {

		String adminUserId = (String) httpRequest.getAttribute("adminUserId");
		logRequest("updateServiceAccount", adminUserId, "id=" + id);

		ServiceAccountEntity updated = serviceAccountService.updateServiceAccount(id, request.name, request.description,
				request.scopes, request.enabled, request.ipWhitelist, request.rateLimit);

		log.info("Service account updated: id={}, by admin={}", id, adminUserId);

		return ResponseEntity.ok(toResponse(updated));
	}

	/**
	 * Delete service account.
	 */
	@DeleteMapping("/{id}")
	@Operation(summary = "Delete service account", description = "Permanently delete a service account")
	public ResponseEntity<Map<String, Object>> deleteServiceAccount(
			@Parameter(description = "Service Account ID") @PathVariable String id, HttpServletRequest httpRequest) {

		String adminUserId = (String) httpRequest.getAttribute("adminUserId");
		logRequest("deleteServiceAccount", adminUserId, "id=" + id);

		boolean deleted = serviceAccountService.deleteServiceAccount(id);

		if (deleted) {
			log.info("Service account deleted: id={}, by admin={}", id, adminUserId);
			return ResponseEntity.ok(Map.of("success", true, "message", "Service account deleted"));
		}
		else {
			return ResponseEntity.notFound().build();
		}
	}

	/**
	 * Regenerate client secret.
	 */
	@PostMapping("/{id}/regenerate-secret")
	@Operation(summary = "Regenerate client secret", description = "Generate a new client secret (invalidates old one). Returns new secret (only shown once).")
	public ResponseEntity<Map<String, Object>> regenerateSecret(
			@Parameter(description = "Service Account ID") @PathVariable String id, HttpServletRequest httpRequest) {

		String adminUserId = (String) httpRequest.getAttribute("adminUserId");
		logRequest("regenerateSecret", adminUserId, "id=" + id);

		String newSecret = serviceAccountService.regenerateClientSecret(id);

		log.info("Service account secret regenerated: id={}, by admin={}", id, adminUserId);

		return ResponseEntity.ok(Map.of("success", true, "clientSecret", newSecret, // IMPORTANT: Only shown once!
				"message", "IMPORTANT: Save the new clientSecret now. It will not be shown again."));
	}

	// =====================================================
	// Request/Response DTOs
	// =====================================================

	/**
	 * Request to create a service account.
	 */
	public record CreateServiceAccountRequest(String name, String description, List<String> scopes) {
	}

	/**
	 * Request to update a service account.
	 */
	public record UpdateServiceAccountRequest(String name, String description, List<String> scopes, Boolean enabled,
			List<String> ipWhitelist, Integer rateLimit) {
	}

	/**
	 * Service account response (without secret).
	 */
	public record ServiceAccountResponse(String id, String name, String description, String clientId,
			List<String> scopes, boolean enabled, String createdBy, String createdAt, String lastUsedAt,
			String lastUsedIp, long usageCount, List<String> ipWhitelist, int rateLimit) {
	}

	private ServiceAccountResponse toResponse(ServiceAccountEntity entity) {
		return new ServiceAccountResponse(entity.getId(), entity.getName(), entity.getDescription(),
				entity.getClientId(), entity.getScopes(), entity.isEnabled(), entity.getCreatedBy(),
				entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null,
				entity.getLastUsedAt() != null ? entity.getLastUsedAt().toString() : null, entity.getLastUsedIp(),
				entity.getUsageCount(), entity.getIpWhitelist(), entity.getRateLimit());
	}

}
