package io.strategiz.service.auth.service;

import io.strategiz.business.tokenauth.SessionAuthBusiness;
import io.strategiz.data.user.entity.SsoRelayToken;
import io.strategiz.data.user.entity.UserEntity;
import io.strategiz.data.user.repository.AuthTokenRepository;
import io.strategiz.data.user.repository.UserRepository;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.auth.exception.AuthErrors;
import io.strategiz.service.base.BaseService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Service for managing one-time SSO relay tokens. Used for cross-app SSO (token relay
 * pattern).
 *
 * Flow: 1. User logs in at strategiz.io (or auth.strategiz.io) 2. If redirect param
 * exists, generate one-time relay token 3. Redirect to target app with token 4. Target
 * app exchanges token for session
 */
@Service
public class AuthTokenService extends BaseService {

	@Override
	protected String getModuleName() {
		return "service-auth";
	}

	private static final int TOKEN_LENGTH = 32; // 256 bits

	private static final int DEFAULT_TTL_SECONDS = 60; // 1 minute

	private final AuthTokenRepository authTokenRepository;

	private final UserRepository userRepository;

	private final SessionAuthBusiness sessionAuthBusiness;

	private final SecureRandom secureRandom;

	// Allowed redirect domains for security
	private final Set<String> allowedDomains = Set.of("console.strategiz.io", "strategiz-console.web.app", "localhost");

	@Value("${app.auth-token.ttl-seconds:60}")
	private int tokenTtlSeconds = DEFAULT_TTL_SECONDS;

	public AuthTokenService(AuthTokenRepository authTokenRepository, UserRepository userRepository,
			SessionAuthBusiness sessionAuthBusiness) {
		this.authTokenRepository = authTokenRepository;
		this.userRepository = userRepository;
		this.sessionAuthBusiness = sessionAuthBusiness;
		this.secureRandom = new SecureRandom();
	}

	/**
	 * Generate a one-time SSO relay token for cross-app authentication
	 * @param userId The authenticated user's ID
	 * @param redirectUrl The URL to redirect to after token exchange
	 * @param clientIp The client's IP address
	 * @return The generated token
	 */
	public String generateToken(String userId, String redirectUrl, String clientIp) {
		// Validate redirect URL
		if (!isAllowedRedirect(redirectUrl)) {
			log.warn("Blocked token generation for unauthorized redirect: {}", redirectUrl);
			throw new StrategizException(AuthErrors.INVALID_TOKEN, "Invalid redirect URL");
		}

		// Generate secure random token
		byte[] tokenBytes = new byte[TOKEN_LENGTH];
		secureRandom.nextBytes(tokenBytes);
		String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

		// Determine target app from redirect URL
		String targetApp = extractDomain(redirectUrl);

		// Create and save SSO relay token
		SsoRelayToken tokenEntity = new SsoRelayToken(token, userId, targetApp, redirectUrl, tokenTtlSeconds);
		tokenEntity.setCreatedFromIp(clientIp);
		authTokenRepository.save(tokenEntity);

		log.info("Generated SSO relay token for user {} targeting {}", userId, targetApp);
		return token;
	}

	/**
	 * Exchange a one-time SSO relay token for a session
	 * @param token The SSO relay token
	 * @param clientIp The client's IP address
	 * @return Session tokens (access token, refresh token)
	 */
	public ExchangeResult exchangeToken(String token, String clientIp) {
		// Find token
		Optional<SsoRelayToken> tokenOpt = authTokenRepository.findByToken(token);
		if (tokenOpt.isEmpty()) {
			log.warn("Token exchange failed: SSO relay token not found");
			throw new StrategizException(AuthErrors.INVALID_TOKEN, "Invalid or expired token");
		}

		SsoRelayToken tokenEntity = tokenOpt.get();

		// Validate token
		if (!tokenEntity.isValid()) {
			log.warn("Token exchange failed: SSO relay token expired or already used for user {}",
					tokenEntity.getUserId());
			// Delete invalid token
			authTokenRepository.delete(token);
			throw new StrategizException(AuthErrors.INVALID_TOKEN, "Token expired or already used");
		}

		// Mark token as used immediately to prevent replay attacks
		authTokenRepository.markAsUsed(token);

		// Get user info
		Optional<UserEntity> userOpt = userRepository.findById(tokenEntity.getUserId());
		if (userOpt.isEmpty()) {
			log.error("Token exchange failed: user not found {}", tokenEntity.getUserId());
			throw new StrategizException(AuthErrors.INVALID_TOKEN, "User not found");
		}

		UserEntity user = userOpt.get();
		String email = user.getProfile() != null ? user.getProfile().getEmail() : "";
		String name = user.getProfile() != null ? user.getProfile().getName() : "";
		String role = user.getProfile() != null ? user.getProfile().getRole() : "";

		// Create session for target app
		SessionAuthBusiness.AuthRequest authRequest = new SessionAuthBusiness.AuthRequest(tokenEntity.getUserId(),
				email, List.of("sso_relay"), false, // Not partial auth
				null, // Device ID
				null, // Device fingerprint
				clientIp, "SSO Relay from " + tokenEntity.getTargetApp(), false // demoMode
		);

		SessionAuthBusiness.AuthResult authResult = sessionAuthBusiness.createAuthentication(authRequest);

		log.info("SSO relay token exchange successful for user {} at {}", tokenEntity.getUserId(),
				tokenEntity.getTargetApp());

		// Delete token after successful exchange
		authTokenRepository.delete(token);

		return new ExchangeResult(authResult.accessToken(), authResult.refreshToken(), tokenEntity.getUserId(), email,
				name, role);
	}

	/**
	 * Check if redirect URL is allowed
	 */
	private boolean isAllowedRedirect(String redirectUrl) {
		if (redirectUrl == null || redirectUrl.isEmpty()) {
			return false;
		}

		try {
			java.net.URL url = new java.net.URL(redirectUrl);
			String host = url.getHost();

			// Check against allowed domains
			for (String domain : allowedDomains) {
				if (host.equals(domain) || host.endsWith("." + domain)) {
					return true;
				}
			}

			// Allow localhost for development
			if (host.equals("localhost") || host.equals("127.0.0.1")) {
				return true;
			}

			return false;
		}
		catch (Exception e) {
			log.warn("Invalid redirect URL format: {}", redirectUrl);
			return false;
		}
	}

	/**
	 * Extract domain from URL
	 */
	private String extractDomain(String url) {
		try {
			java.net.URL parsedUrl = new java.net.URL(url);
			return parsedUrl.getHost();
		}
		catch (Exception e) {
			return "unknown";
		}
	}

	/**
	 * Result of token exchange
	 */
	public record ExchangeResult(String accessToken, String refreshToken, String userId, String email, String name,
			String role) {
	}

}
