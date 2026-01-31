package io.strategiz.service.console.controller;

import io.strategiz.business.tokenauth.ServiceAccountService;
import io.strategiz.business.tokenauth.ServiceAccountService.ServiceAccountCreateResult;
import io.strategiz.data.preferences.entity.ServiceAccountEntity;
import io.strategiz.service.base.controller.BaseController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin controller for managing Service Accounts.
 *
 * Service accounts enable machine-to-machine (M2M) authentication for: - CI/CD pipelines
 * - Integration testing - External service integrations - Automated scripts
 *
 * All endpoints require admin authentication.
 */
@RestController
@RequestMapping("/v1/console/service-accounts")
@Tag(name = "Admin - Service Accounts", description = "Manage service accounts for M2M authentication")
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
	 *
	 * IMPORTANT: The clientSecret is only returned once at creation time. Store it
	 * securely - it cannot be retrieved later.
	 */
	@PostMapping
	@Operation(summary = "Create service account",
			description = "Create a new service account for M2M authentication. The client secret is only shown once!")
	public ResponseEntity<Map<String, Object>> createServiceAccount(@RequestBody CreateRequest request) {
		log.info("Creating service account: name={}", request.name);

		ServiceAccountCreateResult result = serviceAccountService.createServiceAccount(request.name,
				request.description, request.scopes, "admin"); // TODO: get from
																// authenticated user

		Map<String, Object> response = new HashMap<>();
		response.put("id", result.account().getId());
		response.put("name", result.account().getName());
		response.put("clientId", result.account().getClientId());
		response.put("clientSecret", result.clientSecret()); // Only shown once!
		response.put("scopes", result.account().getScopes());
		response.put("message", "IMPORTANT: Save the clientSecret now - it cannot be retrieved later. "
				+ "Use it with clientId to authenticate.");

		log.info("Service account created: id={}, clientId={}", result.account().getId(),
				result.account().getClientId());

		return ResponseEntity.ok(response);
	}

	/**
	 * List all service accounts.
	 */
	@GetMapping
	@Operation(summary = "List service accounts", description = "Get all service accounts (secrets are not returned)")
	public ResponseEntity<List<ServiceAccountSummary>> listServiceAccounts() {
		log.debug("Listing all service accounts");

		List<ServiceAccountEntity> accounts = serviceAccountService.listServiceAccounts();

		List<ServiceAccountSummary> summaries = accounts.stream().map(this::toSummary).toList();

		return ResponseEntity.ok(summaries);
	}

	/**
	 * Get a specific service account by ID.
	 */
	@GetMapping("/{id}")
	@Operation(summary = "Get service account", description = "Get a specific service account by ID")
	public ResponseEntity<ServiceAccountSummary> getServiceAccount(@PathVariable String id) {
		log.debug("Getting service account: id={}", id);

		return serviceAccountService.getServiceAccount(id)
			.map(this::toSummary)
			.map(ResponseEntity::ok)
			.orElse(ResponseEntity.notFound().build());
	}

	/**
	 * Update a service account.
	 */
	@PutMapping("/{id}")
	@Operation(summary = "Update service account", description = "Update service account properties")
	public ResponseEntity<ServiceAccountSummary> updateServiceAccount(@PathVariable String id,
			@RequestBody UpdateRequest request) {
		log.info("Updating service account: id={}", id);

		try {
			ServiceAccountEntity updated = serviceAccountService.updateServiceAccount(id, request.name,
					request.description, request.scopes, request.enabled, request.ipWhitelist, request.rateLimit);

			return ResponseEntity.ok(toSummary(updated));
		}
		catch (IllegalArgumentException e) {
			log.warn("Service account not found: id={}", id);
			return ResponseEntity.notFound().build();
		}
	}

	/**
	 * Delete a service account.
	 */
	@DeleteMapping("/{id}")
	@Operation(summary = "Delete service account", description = "Permanently delete a service account")
	public ResponseEntity<Map<String, String>> deleteServiceAccount(@PathVariable String id) {
		log.info("Deleting service account: id={}", id);

		boolean deleted = serviceAccountService.deleteServiceAccount(id);

		if (deleted) {
			return ResponseEntity.ok(Map.of("message", "Service account deleted successfully", "id", id));
		}
		else {
			return ResponseEntity.notFound().build();
		}
	}

	/**
	 * Regenerate the client secret for a service account.
	 *
	 * IMPORTANT: The new clientSecret is only returned once. Store it securely - it
	 * cannot be retrieved later.
	 */
	@PostMapping("/{id}/regenerate-secret")
	@Operation(summary = "Regenerate secret",
			description = "Generate a new client secret. The old secret will stop working immediately.")
	public ResponseEntity<Map<String, Object>> regenerateSecret(@PathVariable String id) {
		log.info("Regenerating secret for service account: id={}", id);

		try {
			String newSecret = serviceAccountService.regenerateClientSecret(id);

			Map<String, Object> response = new HashMap<>();
			response.put("id", id);
			response.put("clientSecret", newSecret); // Only shown once!
			response.put("message", "IMPORTANT: Save the new clientSecret now - it cannot be retrieved later. "
					+ "The old secret is now invalid.");

			return ResponseEntity.ok(response);
		}
		catch (IllegalArgumentException e) {
			log.warn("Service account not found: id={}", id);
			return ResponseEntity.notFound().build();
		}
	}

	// =====================================================
	// Helper Methods
	// =====================================================

	private ServiceAccountSummary toSummary(ServiceAccountEntity entity) {
		return new ServiceAccountSummary(entity.getId(), entity.getName(), entity.getDescription(),
				entity.getClientId(), entity.getScopes(), entity.isEnabled(), entity.getCreatedAt(),
				entity.getUpdatedAt(), entity.getLastUsedAt(), entity.getUsageCount(), entity.getRateLimit(),
				entity.getIpWhitelist());
	}

	// =====================================================
	// Request/Response DTOs
	// =====================================================

	public static class CreateRequest {

		public String name;

		public String description;

		public List<String> scopes;

	}

	public static class UpdateRequest {

		public String name;

		public String description;

		public List<String> scopes;

		public Boolean enabled;

		public List<String> ipWhitelist;

		public Integer rateLimit;

	}

	public record ServiceAccountSummary(String id, String name, String description, String clientId,
			List<String> scopes, boolean enabled, com.google.cloud.Timestamp createdAt,
			com.google.cloud.Timestamp updatedAt, com.google.cloud.Timestamp lastUsedAt, long usageCount, int rateLimit,
			List<String> ipWhitelist) {
	}

}
