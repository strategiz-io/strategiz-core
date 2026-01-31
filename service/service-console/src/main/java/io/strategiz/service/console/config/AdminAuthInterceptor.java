package io.strategiz.service.console.config;

import io.strategiz.business.tokenauth.SessionAuthBusiness;
import io.strategiz.data.user.entity.UserEntity;
import io.strategiz.data.user.repository.UserRepository;
import io.strategiz.framework.authorization.context.AuthenticatedUser;
import io.strategiz.framework.authorization.validator.PasetoTokenValidator;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

/**
 * Interceptor that validates admin role for /v1/console/* endpoints. Requires the user to
 * have ADMIN role in their profile. Can be disabled for local development via
 * console.auth.enabled=false.
 */
@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "console.auth.enabled",
		havingValue = "true", matchIfMissing = true)
public class AdminAuthInterceptor implements HandlerInterceptor {

	private static final Logger logger = LoggerFactory.getLogger(AdminAuthInterceptor.class);

	private static final String ADMIN_ROLE = "ADMIN";

	private static final String ACCESS_TOKEN_COOKIE = "strategiz-access-token";

	private static final String SERVICE_ACCOUNT_PREFIX = "sa:";

	private final SessionAuthBusiness sessionAuthBusiness;

	private final UserRepository userRepository;

	private final PasetoTokenValidator pasetoTokenValidator;

	@Value("${console.auth.enabled:true}")
	private boolean authEnabled;

	@Autowired
	public AdminAuthInterceptor(SessionAuthBusiness sessionAuthBusiness, UserRepository userRepository,
			PasetoTokenValidator pasetoTokenValidator) {
		this.sessionAuthBusiness = sessionAuthBusiness;
		this.userRepository = userRepository;
		this.pasetoTokenValidator = pasetoTokenValidator;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		String requestPath = request.getRequestURI();

		// Only intercept /v1/console/* paths
		if (!requestPath.startsWith("/v1/console")) {
			return true;
		}

		// Skip authentication if disabled (for local development)
		if (!authEnabled) {
			logger.warn("Console authentication DISABLED - allowing unauthenticated access to: {}", requestPath);
			request.setAttribute("adminUserId", "dev-admin");
			return true;
		}

		logger.debug("Admin auth check for path: {}", requestPath);

		// Extract token from cookie or header
		String token = extractSessionToken(request);
		if (token == null) {
			logger.warn("No session token found for admin request: {}", requestPath);
			response.sendError(HttpStatus.UNAUTHORIZED.value(), "Authentication required");
			return false;
		}

		// If this is a PASETO token (v4.local.), try to validate it directly
		// This handles both service accounts and regular user access tokens
		if (isPasetoToken(token)) {
			return handlePasetoAuth(request, response, token, requestPath);
		}

		// Fall back to session-based validation for other token formats
		return handleUserSessionAuth(request, response, token, requestPath);
	}

	/**
	 * Check if the token is a PASETO token.
	 */
	private boolean isPasetoToken(String token) {
		// PASETO v4.local tokens start with "v4.local."
		return token != null && token.startsWith("v4.local.");
	}

	/**
	 * Handle authentication for PASETO tokens (both service accounts and user access
	 * tokens).
	 */
	private boolean handlePasetoAuth(HttpServletRequest request, HttpServletResponse response, String token,
			String requestPath) throws Exception {
		try {
			Optional<AuthenticatedUser> userOpt = pasetoTokenValidator.validateAndExtract(token);
			if (userOpt.isEmpty()) {
				logger.warn("Invalid PASETO token for admin request: {}", requestPath);
				response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid token");
				return false;
			}

			AuthenticatedUser user = userOpt.get();
			String userId = user.getUserId();

			// Service accounts (userId starts with "sa:") get automatic admin access
			if (userId.startsWith(SERVICE_ACCOUNT_PREFIX)) {
				request.setAttribute("adminUserId", userId);
				logger.info("Service account access granted: serviceAccountId={}, path={}", userId, requestPath);
				return true;
			}

			// Regular user - check admin role in database
			logger.info("Admin auth: PASETO token validated for userId={}, checking admin role", userId);
			Optional<UserEntity> userEntityOpt = userRepository.findById(userId);
			if (userEntityOpt.isEmpty()) {
				logger.warn("User not found for admin request: userId={}", userId);
				response.sendError(HttpStatus.UNAUTHORIZED.value(), "User not found");
				return false;
			}

			UserEntity userEntity = userEntityOpt.get();
			boolean hasProfile = userEntity.getProfile() != null;
			String role = hasProfile ? userEntity.getProfile().getRole() : null;
			logger.info("Admin auth: userId={}, hasProfile={}, role={}", userId, hasProfile, role);

			if (!ADMIN_ROLE.equals(role)) {
				logger.warn("Non-admin user attempted admin access: userId={}, role={}, path={}", userId, role,
						requestPath);
				response.sendError(HttpStatus.FORBIDDEN.value(), "Admin access required");
				return false;
			}

			request.setAttribute("adminUserId", userId);
			logger.info("Admin access granted via PASETO: userId={}, path={}", userId, requestPath);
			return true;
		}
		catch (Exception e) {
			logger.warn("PASETO token validation error: {} - {}", requestPath, e.getMessage());
			response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid token");
			return false;
		}
	}

	/**
	 * Handle authentication for regular user sessions.
	 */
	private boolean handleUserSessionAuth(HttpServletRequest request, HttpServletResponse response, String token,
			String requestPath) throws Exception {
		logger.info("Admin auth: validating user session for path: {}", requestPath);
		logger.info("Admin auth: token prefix: {}...",
				token != null ? token.substring(0, Math.min(20, token.length())) : "null");

		// Validate session and get user ID
		Optional<String> userIdOpt;
		try {
			userIdOpt = sessionAuthBusiness.validateSession(token);
		}
		catch (Exception e) {
			logger.warn("Session validation error for admin request: {} - {}", requestPath, e.getMessage());
			response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid session token");
			return false;
		}
		if (userIdOpt.isEmpty()) {
			logger.warn("Invalid session for admin request: {}", requestPath);
			response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid or expired session");
			return false;
		}

		String userId = userIdOpt.get();
		logger.info("Admin auth: session validated for userId={}", userId);

		// Check if user has admin role
		Optional<UserEntity> userOpt = userRepository.findById(userId);
		if (userOpt.isEmpty()) {
			logger.warn("User not found for admin request: userId={}", userId);
			response.sendError(HttpStatus.UNAUTHORIZED.value(), "User not found");
			return false;
		}

		UserEntity user = userOpt.get();
		boolean hasProfile = user.getProfile() != null;
		String role = hasProfile ? user.getProfile().getRole() : null;
		logger.info("Admin auth: userId={}, hasProfile={}, role={}", userId, hasProfile, role);

		if (!ADMIN_ROLE.equals(role)) {
			logger.warn("Non-admin user attempted admin access: userId={}, role={}, path={}", userId, role,
					requestPath);
			response.sendError(HttpStatus.FORBIDDEN.value(), "Admin access required");
			return false;
		}

		// Set user ID in request attribute for controllers to use
		request.setAttribute("adminUserId", userId);
		logger.info("Admin access granted: userId={}, path={}", userId, requestPath);

		return true;
	}

	private String extractSessionToken(HttpServletRequest request) {
		// Try cookie first
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (ACCESS_TOKEN_COOKIE.equals(cookie.getName())) {
					return cookie.getValue();
				}
			}
		}

		// Try Authorization header as fallback
		String authHeader = request.getHeader("Authorization");
		if (authHeader != null && authHeader.startsWith("Bearer ")) {
			return authHeader.substring(7);
		}

		return null;
	}

}
