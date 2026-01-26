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

		// Check if this is a service account token (PASETO token)
		if (isServiceAccountToken(token)) {
			return handleServiceAccountAuth(request, response, token, requestPath);
		}

		// Regular user session validation
		return handleUserSessionAuth(request, response, token, requestPath);
	}

	/**
	 * Check if the token is a service account PASETO token.
	 */
	private boolean isServiceAccountToken(String token) {
		// PASETO v4.local tokens start with "v4.local."
		return token != null && token.startsWith("v4.local.");
	}

	/**
	 * Handle authentication for service account tokens.
	 */
	private boolean handleServiceAccountAuth(HttpServletRequest request, HttpServletResponse response, String token,
			String requestPath) throws Exception {
		try {
			Optional<AuthenticatedUser> userOpt = pasetoTokenValidator.validateAndExtract(token);
			if (userOpt.isEmpty()) {
				logger.warn("Invalid service account token for admin request: {}", requestPath);
				response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid service account token");
				return false;
			}

			AuthenticatedUser user = userOpt.get();
			String userId = user.getUserId();

			// Check if this is a service account (userId starts with "sa:")
			if (!userId.startsWith(SERVICE_ACCOUNT_PREFIX)) {
				logger.warn("Token is not a service account token: userId={}", userId);
				response.sendError(HttpStatus.FORBIDDEN.value(), "Service account required");
				return false;
			}

			// Service accounts are granted admin access for console operations
			request.setAttribute("adminUserId", userId);
			logger.info("Service account access granted: serviceAccountId={}, path={}", userId, requestPath);
			return true;
		}
		catch (Exception e) {
			logger.warn("Service account token validation error: {} - {}", requestPath, e.getMessage());
			response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid service account token");
			return false;
		}
	}

	/**
	 * Handle authentication for regular user sessions.
	 */
	private boolean handleUserSessionAuth(HttpServletRequest request, HttpServletResponse response, String token,
			String requestPath) throws Exception {
		logger.info("Admin auth: validating user session for path: {}", requestPath);
		logger.info("Admin auth: token prefix: {}...", token != null ? token.substring(0, Math.min(20, token.length())) : "null");

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
