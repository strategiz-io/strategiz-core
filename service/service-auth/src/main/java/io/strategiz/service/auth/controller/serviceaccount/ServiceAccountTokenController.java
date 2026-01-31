package io.strategiz.service.auth.controller.serviceaccount;

import io.strategiz.business.tokenauth.ServiceAccountService;
import io.strategiz.business.tokenauth.ServiceAccountService.ServiceAccountAuthenticationException;
import io.strategiz.business.tokenauth.ServiceAccountService.ServiceAccountTokenResult;
import io.strategiz.service.base.controller.BaseController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Controller for Service Account token generation (OAuth 2.0 Client Credentials flow).
 *
 * This endpoint allows service accounts to obtain access tokens using their client
 * credentials (client_id + client_secret).
 *
 * Usage: POST /v1/auth/service-account/token Content-Type: application/json
 *
 * { "client_id": "sa_abc123", "client_secret": "your-secret-here", "grant_type":
 * "client_credentials", "scope": "read:strategies write:test-results" (optional) }
 *
 * Response: { "access_token": "v4.local.xxx...", "token_type": "Bearer", "expires_in":
 * 3600, "scope": "read:strategies write:test-results" }
 */
@RestController
@RequestMapping("/v1/auth/service-account")
@Tag(name = "Service Account Auth", description = "Service account token generation (Client Credentials flow)")
public class ServiceAccountTokenController extends BaseController {

	private static final String MODULE_NAME = "AUTH";

	private final ServiceAccountService serviceAccountService;

	public ServiceAccountTokenController(ServiceAccountService serviceAccountService) {
		this.serviceAccountService = serviceAccountService;
	}

	@Override
	protected String getModuleName() {
		return MODULE_NAME;
	}

	/**
	 * Generate access token for service account (OAuth 2.0 Client Credentials flow).
	 *
	 * This is a PUBLIC endpoint - no authentication required. Authentication is done via
	 * client_id + client_secret in the request body.
	 */
	@PostMapping("/token")
	@Operation(summary = "Get access token",
			description = "Exchange client credentials for an access token (OAuth 2.0 Client Credentials flow)")
	public ResponseEntity<Map<String, Object>> getToken(@RequestBody TokenRequest request,
			HttpServletRequest httpRequest) {

		String clientIp = getClientIp(httpRequest);
		log.debug("Service account token request from IP: {}", clientIp);

		// Validate grant type
		if (request.grantType == null || !request.grantType.equals("client_credentials")) {
			return ResponseEntity.badRequest()
				.body(Map.of("error", "unsupported_grant_type", "error_description",
						"Only 'client_credentials' grant type is supported"));
		}

		// Validate required fields
		if (request.clientId == null || request.clientId.isEmpty()) {
			return ResponseEntity.badRequest()
				.body(Map.of("error", "invalid_request", "error_description", "client_id is required"));
		}

		if (request.clientSecret == null || request.clientSecret.isEmpty()) {
			return ResponseEntity.badRequest()
				.body(Map.of("error", "invalid_request", "error_description", "client_secret is required"));
		}

		try {
			// Parse optional parameters
			List<String> requestedScopes = null;
			if (request.scope != null && !request.scope.isEmpty()) {
				requestedScopes = List.of(request.scope.split(" "));
			}

			Duration requestedValidity = null;
			if (request.expiresIn != null && request.expiresIn > 0) {
				requestedValidity = Duration.ofSeconds(request.expiresIn);
			}

			// Generate token
			ServiceAccountTokenResult result = serviceAccountService.generateToken(request.clientId,
					request.clientSecret, requestedScopes, requestedValidity, clientIp);

			log.info("Service account token issued for clientId={} from IP={}", request.clientId, clientIp);

			// Return OAuth 2.0 compliant response
			return ResponseEntity.ok(Map.of("access_token", result.accessToken(), "token_type", result.tokenType(),
					"expires_in", result.expiresIn(), "scope", String.join(" ", result.scopes())));

		}
		catch (ServiceAccountAuthenticationException e) {
			log.warn("Service account authentication failed: clientId={}, reason={}, IP={}", request.clientId,
					e.getMessage(), clientIp);

			// Return OAuth 2.0 compliant error
			return ResponseEntity.status(401)
				.body(Map.of("error", "invalid_client", "error_description", e.getMessage()));
		}
		catch (Exception e) {
			log.error("Service account token generation error: clientId={}, error={}", request.clientId, e.getMessage(),
					e);

			return ResponseEntity.status(500)
				.body(Map.of("error", "server_error", "error_description", "An internal error occurred"));
		}
	}

	/**
	 * Token introspection endpoint (optional, for debugging).
	 */
	@PostMapping("/introspect")
	@Operation(summary = "Introspect token", description = "Check if a token is valid and get its metadata")
	public ResponseEntity<Map<String, Object>> introspectToken(@RequestBody Map<String, String> request) {
		// TODO: Implement token introspection if needed
		return ResponseEntity.ok(Map.of("active", false, "message", "Token introspection not yet implemented"));
	}

	/**
	 * Get client IP address.
	 */
	private String getClientIp(HttpServletRequest request) {
		String xForwardedFor = request.getHeader("X-Forwarded-For");
		if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
			return xForwardedFor.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}

	// =====================================================
	// Request DTO
	// =====================================================

	/**
	 * OAuth 2.0 Client Credentials token request. Uses snake_case for OAuth 2.0
	 * compliance.
	 */
	public static class TokenRequest {

		@com.fasterxml.jackson.annotation.JsonProperty("client_id")
		private String clientId;

		@com.fasterxml.jackson.annotation.JsonProperty("client_secret")
		private String clientSecret;

		@com.fasterxml.jackson.annotation.JsonProperty("grant_type")
		private String grantType;

		@com.fasterxml.jackson.annotation.JsonProperty("scope")
		private String scope; // Space-separated scopes

		@com.fasterxml.jackson.annotation.JsonProperty("expires_in")
		private Long expiresIn; // Optional: requested validity in seconds

		// Getters and setters for Jackson

		public String getClientId() {
			return clientId;
		}

		public void setClientId(String clientId) {
			this.clientId = clientId;
		}

		public String getClientSecret() {
			return clientSecret;
		}

		public void setClientSecret(String clientSecret) {
			this.clientSecret = clientSecret;
		}

		public String getGrantType() {
			return grantType;
		}

		public void setGrantType(String grantType) {
			this.grantType = grantType;
		}

		public String getScope() {
			return scope;
		}

		public void setScope(String scope) {
			this.scope = scope;
		}

		public Long getExpiresIn() {
			return expiresIn;
		}

		public void setExpiresIn(Long expiresIn) {
			this.expiresIn = expiresIn;
		}

	}

}
