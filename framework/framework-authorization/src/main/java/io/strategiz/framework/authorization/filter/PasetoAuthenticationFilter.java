package io.strategiz.framework.authorization.filter;

import io.strategiz.framework.authorization.config.AuthorizationProperties;
import io.strategiz.framework.authorization.context.AuthenticatedUser;
import io.strategiz.framework.authorization.context.SecurityContext;
import io.strategiz.framework.authorization.context.SecurityContextHolder;
import io.strategiz.framework.authorization.validator.PasetoTokenValidator;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.util.AntPathMatcher;

/**
 * Servlet filter that extracts and validates PASETO tokens from incoming requests.
 *
 * <p>
 * Token extraction order:
 *
 * <ol>
 * <li>HTTP-only cookie named {@code strategiz-access-token}
 * <li>Authorization header with Bearer scheme
 * </ol>
 *
 * <p>
 * If a valid token is found, the authenticated user is set in
 * {@link SecurityContextHolder}. If no token is found or the token is invalid, the
 * request proceeds without authentication (the authorization aspects will enforce
 * authentication requirements).
 *
 * <p>
 * This filter delegates ALL token validation to {@link PasetoTokenValidator}, which is
 * the single source of truth for token validation in the system.
 */
public class PasetoAuthenticationFilter implements Filter {

	private static final Logger log = LoggerFactory.getLogger(PasetoAuthenticationFilter.class);

	private static final String ACCESS_TOKEN_COOKIE = "strategiz-access-token";

	private static final String AUTHORIZATION_HEADER = "Authorization";

	private static final String BEARER_PREFIX = "Bearer ";

	private final PasetoTokenValidator tokenValidator;

	private final AuthorizationProperties properties;

	private final AntPathMatcher pathMatcher = new AntPathMatcher();

	/**
	 * Creates a new PasetoAuthenticationFilter.
	 * @param tokenValidator the token validator
	 * @param properties the authorization properties
	 */
	public PasetoAuthenticationFilter(PasetoTokenValidator tokenValidator, AuthorizationProperties properties) {
		this.tokenValidator = tokenValidator;
		this.properties = properties;
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		log.info("PasetoAuthenticationFilter initialized with PasetoTokenValidator");
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest httpRequest = (HttpServletRequest) request;

		try {
			// Initialize security context
			SecurityContextHolder.setContext(new SecurityContext());

			// Skip authentication for configured paths
			if (!shouldAuthenticate(httpRequest)) {
				log.debug("Skipping authentication for path: {}", httpRequest.getRequestURI());
				chain.doFilter(request, response);
				return;
			}

			// Extract and validate token using the single source of truth
			String token = extractToken(httpRequest);
			if (token != null) {
				try {
					Optional<AuthenticatedUser> userOpt = tokenValidator.validateAndExtract(token);
					if (userOpt.isPresent()) {
						AuthenticatedUser user = userOpt.get();
						SecurityContextHolder.getContext().setAuthenticatedUser(user);
						MDC.put("userId", user.getUserId());
						log.debug("Authenticated user: {} (acr={})", user.getUserId(), user.getAcr());
					}
					else {
						log.debug("Token validation failed - continuing without authentication");
					}
				}
				catch (Exception e) {
					// Catch any exception from token parsing (e.g., malformed tokens)
					log.debug("Token parsing error - continuing without authentication: {}", e.getMessage());
				}
			}

			chain.doFilter(request, response);
		}
		finally {
			SecurityContextHolder.clearContext();
			MDC.remove("userId");
		}
	}

	@Override
	public void destroy() {
		log.info("PasetoAuthenticationFilter destroyed");
	}

	/** Check if the request path should be authenticated. */
	private boolean shouldAuthenticate(HttpServletRequest request) {
		String path = request.getRequestURI();
		for (String skipPath : properties.getSkipPaths()) {
			if (pathMatcher.match(skipPath, path)) {
				return false;
			}
		}
		return true;
	}

	/** Extract token from request (cookie first, then header). */
	private String extractToken(HttpServletRequest request) {
		// 1. Try cookie first
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (ACCESS_TOKEN_COOKIE.equals(cookie.getName())) {
					return cookie.getValue();
				}
			}
		}

		// 2. Fall back to Authorization header
		String header = request.getHeader(AUTHORIZATION_HEADER);
		if (header != null && header.startsWith(BEARER_PREFIX)) {
			return header.substring(BEARER_PREFIX.length());
		}

		return null;
	}

}
