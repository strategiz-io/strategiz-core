package io.strategiz.service.console.config;

import io.strategiz.business.tokenauth.SessionAuthBusiness;
import io.strategiz.data.user.entity.UserEntity;
import io.strategiz.data.user.repository.UserRepository;
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
 * Interceptor that validates admin role for /v1/console/* endpoints.
 * Requires the user to have ADMIN role in their profile.
 * Can be disabled for local development via console.auth.enabled=false.
 */
@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    name = "console.auth.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class AdminAuthInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(AdminAuthInterceptor.class);
    private static final String ADMIN_ROLE = "ADMIN";
    private static final String ACCESS_TOKEN_COOKIE = "strategiz-access-token";

    private final SessionAuthBusiness sessionAuthBusiness;
    private final UserRepository userRepository;

    @Value("${console.auth.enabled:true}")
    private boolean authEnabled;

    @Autowired
    public AdminAuthInterceptor(SessionAuthBusiness sessionAuthBusiness, UserRepository userRepository) {
        this.sessionAuthBusiness = sessionAuthBusiness;
        this.userRepository = userRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
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

        // Extract session token from cookie
        String sessionToken = extractSessionToken(request);
        if (sessionToken == null) {
            logger.warn("No session token found for admin request: {}", requestPath);
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Authentication required");
            return false;
        }

        // Validate session and get user ID
        Optional<String> userIdOpt;
        try {
            userIdOpt = sessionAuthBusiness.validateSession(sessionToken);
        } catch (Exception e) {
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

        // Check if user has admin role
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            logger.warn("User not found for admin request: userId={}", userId);
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "User not found");
            return false;
        }

        UserEntity user = userOpt.get();
        String role = user.getProfile() != null ? user.getProfile().getRole() : null;

        if (!ADMIN_ROLE.equals(role)) {
            logger.warn("Non-admin user attempted admin access: userId={}, role={}, path={}",
                userId, role, requestPath);
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
