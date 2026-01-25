package io.strategiz.business.tokenauth;

import io.strategiz.data.preferences.entity.ServiceAccountEntity;
import io.strategiz.data.preferences.repository.ServiceAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing Service Accounts and generating M2M authentication tokens.
 *
 * Service accounts enable programmatic API access for:
 * - CI/CD pipelines (integration testing)
 * - External service integrations
 * - Automated scripts and batch processes
 *
 * Authentication flow:
 * 1. Admin creates service account via API → gets clientId + clientSecret
 * 2. Client calls /v1/auth/service-account/token with credentials
 * 3. Service validates credentials and returns access token
 * 4. Client uses access token for API calls
 */
@Service
public class ServiceAccountService {

	private static final Logger log = LoggerFactory.getLogger(ServiceAccountService.class);

	private static final int CLIENT_SECRET_LENGTH = 32; // 256 bits

	private static final Duration DEFAULT_TOKEN_VALIDITY = Duration.ofHours(1);

	private static final Duration MAX_TOKEN_VALIDITY = Duration.ofHours(24);

	private final ServiceAccountRepository serviceAccountRepository;

	private final PasetoTokenProvider tokenProvider;

	private final PasswordEncoder passwordEncoder;

	private final SecureRandom secureRandom;

	public ServiceAccountService(ServiceAccountRepository serviceAccountRepository, PasetoTokenProvider tokenProvider) {
		this.serviceAccountRepository = serviceAccountRepository;
		this.tokenProvider = tokenProvider;
		this.passwordEncoder = new BCryptPasswordEncoder();
		this.secureRandom = new SecureRandom();
	}

	// =====================================================
	// Service Account Management (Admin Operations)
	// =====================================================

	/**
	 * Create a new service account.
	 * @param name Human-readable name
	 * @param description Purpose/usage description
	 * @param scopes Authorized scopes (e.g., ["read:strategies", "write:test-results"])
	 * @param createdBy Admin user who created this account
	 * @return Created service account with plaintext client secret (only shown once)
	 */
	public ServiceAccountCreateResult createServiceAccount(String name, String description, List<String> scopes,
			String createdBy) {
		log.info("Creating service account: name={}, scopes={}, createdBy={}", name, scopes, createdBy);

		// Generate unique client ID
		String clientId = generateClientId();
		while (serviceAccountRepository.existsByClientId(clientId)) {
			clientId = generateClientId();
		}

		// Generate random client secret
		String clientSecret = generateClientSecret();
		String hashedSecret = passwordEncoder.encode(clientSecret);

		// Create entity
		ServiceAccountEntity entity = new ServiceAccountEntity(name, clientId, hashedSecret, scopes);
		entity.setDescription(description);
		entity.setCreatedBy(createdBy);

		// Save to database
		ServiceAccountEntity created = serviceAccountRepository.create(entity);

		log.info("Service account created: id={}, clientId={}", created.getId(), created.getClientId());

		// Return with plaintext secret (only shown once)
		return new ServiceAccountCreateResult(created, clientSecret);
	}

	/**
	 * Get a service account by ID.
	 */
	public Optional<ServiceAccountEntity> getServiceAccount(String id) {
		return serviceAccountRepository.findById(id);
	}

	/**
	 * List all service accounts.
	 */
	public List<ServiceAccountEntity> listServiceAccounts() {
		return serviceAccountRepository.findAll();
	}

	/**
	 * Update a service account.
	 */
	public ServiceAccountEntity updateServiceAccount(String id, String name, String description, List<String> scopes,
			Boolean enabled, List<String> ipWhitelist, Integer rateLimit) {

		ServiceAccountEntity entity = serviceAccountRepository.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("Service account not found: " + id));

		if (name != null) {
			entity.setName(name);
		}
		if (description != null) {
			entity.setDescription(description);
		}
		if (scopes != null) {
			entity.setScopes(scopes);
		}
		if (enabled != null) {
			entity.setEnabled(enabled);
		}
		if (ipWhitelist != null) {
			entity.setIpWhitelist(ipWhitelist);
		}
		if (rateLimit != null) {
			entity.setRateLimit(rateLimit);
		}

		return serviceAccountRepository.update(entity);
	}

	/**
	 * Regenerate client secret for a service account.
	 * @return New plaintext secret (only shown once)
	 */
	public String regenerateClientSecret(String id) {
		log.info("Regenerating client secret for service account: id={}", id);

		ServiceAccountEntity entity = serviceAccountRepository.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("Service account not found: " + id));

		String newSecret = generateClientSecret();
		entity.setHashedClientSecret(passwordEncoder.encode(newSecret));

		serviceAccountRepository.update(entity);

		log.info("Client secret regenerated for service account: id={}", id);
		return newSecret;
	}

	/**
	 * Delete a service account.
	 */
	public boolean deleteServiceAccount(String id) {
		log.info("Deleting service account: id={}", id);
		return serviceAccountRepository.delete(id);
	}

	// =====================================================
	// Token Generation (Client Operations)
	// =====================================================

	/**
	 * Authenticate service account and generate access token.
	 * @param clientId The client ID
	 * @param clientSecret The client secret (plaintext)
	 * @param requestedScopes Optional: requested scopes (must be subset of granted)
	 * @param requestedValidity Optional: requested validity (max 24 hours)
	 * @param clientIp The client IP address for validation and logging
	 * @return Token response with access token
	 */
	public ServiceAccountTokenResult generateToken(String clientId, String clientSecret, List<String> requestedScopes,
			Duration requestedValidity, String clientIp) throws ServiceAccountAuthenticationException {

		log.debug("Token generation requested for clientId={} from IP={}", clientId, clientIp);

		// Find service account
		ServiceAccountEntity account = serviceAccountRepository.findByClientId(clientId)
			.orElseThrow(() -> new ServiceAccountAuthenticationException("Invalid client credentials"));

		// Validate account is active
		if (!account.isValid()) {
			log.warn("Token denied - account disabled or expired: clientId={}", clientId);
			throw new ServiceAccountAuthenticationException("Service account is disabled or expired");
		}

		// Validate IP whitelist
		if (!account.isIpAllowed(clientIp)) {
			log.warn("Token denied - IP not whitelisted: clientId={}, ip={}", clientId, clientIp);
			throw new ServiceAccountAuthenticationException("IP address not allowed");
		}

		// Validate client secret
		if (!passwordEncoder.matches(clientSecret, account.getHashedClientSecret())) {
			log.warn("Token denied - invalid secret: clientId={}", clientId);
			throw new ServiceAccountAuthenticationException("Invalid client credentials");
		}

		// Determine scopes
		List<String> effectiveScopes = account.getScopes();
		if (requestedScopes != null && !requestedScopes.isEmpty()) {
			// Validate requested scopes are subset of granted scopes
			for (String scope : requestedScopes) {
				if (!account.getScopes().contains(scope)) {
					throw new ServiceAccountAuthenticationException("Requested scope not granted: " + scope);
				}
			}
			effectiveScopes = requestedScopes;
		}

		// Determine validity
		Duration validity = DEFAULT_TOKEN_VALIDITY;
		if (requestedValidity != null) {
			if (requestedValidity.compareTo(MAX_TOKEN_VALIDITY) > 0) {
				throw new ServiceAccountAuthenticationException("Requested validity exceeds maximum of 24 hours");
			}
			validity = requestedValidity;
		}

		// Generate token using service account ID as the "user ID"
		// Using a special prefix to distinguish service account tokens
		String tokenSubject = "sa:" + account.getId();

		// Use createAuthenticationToken with ACR "2" for service accounts
		// This ensures the token is accepted by endpoints with @RequireAuth(minAcr = "1")
		// Service account credentials are considered strong authentication (ACR 2)
		List<String> authMethods = List.of("client_credentials");
		String accessToken = tokenProvider.createAuthenticationToken(tokenSubject, authMethods, "2", validity, false);

		// Record usage
		serviceAccountRepository.recordUsage(account.getId(), clientIp);

		log.info("Token issued for service account: clientId={}, scopes={}, validity={}", clientId, effectiveScopes,
				validity);

		return new ServiceAccountTokenResult(accessToken, "Bearer", validity.toSeconds(), effectiveScopes);
	}

	// =====================================================
	// Helper Methods
	// =====================================================

	private String generateClientId() {
		// Format: sa_<uuid> (sa = service account)
		return "sa_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
	}

	private String generateClientSecret() {
		byte[] secretBytes = new byte[CLIENT_SECRET_LENGTH];
		secureRandom.nextBytes(secretBytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(secretBytes);
	}

	// =====================================================
	// Result Classes
	// =====================================================

	/**
	 * Result of creating a service account. Contains the entity and the plaintext
	 * secret (only available at creation time).
	 */
	public record ServiceAccountCreateResult(ServiceAccountEntity account, String clientSecret) {
	}

	/**
	 * Result of token generation.
	 */
	public record ServiceAccountTokenResult(String accessToken, String tokenType, long expiresIn,
			List<String> scopes) {
	}

	/**
	 * Exception thrown when service account authentication fails.
	 */
	public static class ServiceAccountAuthenticationException extends Exception {

		public ServiceAccountAuthenticationException(String message) {
			super(message);
		}

	}

}
