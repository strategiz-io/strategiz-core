package io.strategiz.framework.authorization.filter;

import dev.paseto.jpaseto.Paseto;
import dev.paseto.jpaseto.PasetoException;
import dev.paseto.jpaseto.Pasetos;
import io.strategiz.framework.authorization.config.AuthorizationProperties;
import io.strategiz.framework.authorization.context.AuthenticatedUser;
import io.strategiz.framework.authorization.context.SecurityContext;
import io.strategiz.framework.authorization.context.SecurityContextHolder;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.util.AntPathMatcher;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Servlet filter that extracts and validates PASETO tokens from incoming requests.
 *
 * <p>Token extraction order:</p>
 * <ol>
 *   <li>HTTP-only cookie named {@code strategiz-access-token}</li>
 *   <li>Authorization header with Bearer scheme</li>
 * </ol>
 *
 * <p>If a valid token is found, the authenticated user is set in {@link SecurityContextHolder}.
 * If no token is found or the token is invalid, the request proceeds without authentication
 * (the authorization aspects will enforce authentication requirements).</p>
 */
public class PasetoAuthenticationFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(PasetoAuthenticationFilter.class);
    private static final String ACCESS_TOKEN_COOKIE = "strategiz-access-token";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final SecretKey sessionKey;
    private final SecretKey identityKey;
    private final AuthorizationProperties properties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public PasetoAuthenticationFilter(SecretKey sessionKey, SecretKey identityKey,
                                      AuthorizationProperties properties) {
        this.sessionKey = sessionKey;
        this.identityKey = identityKey;
        this.properties = properties;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("PasetoAuthenticationFilter initialized");
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

            // Extract and validate token
            String token = extractToken(httpRequest);
            if (token != null) {
                try {
                    AuthenticatedUser user = validateAndExtract(token);
                    SecurityContextHolder.getContext().setAuthenticatedUser(user);
                    MDC.put("userId", user.getUserId());
                    log.debug("Authenticated user: {} (acr={})", user.getUserId(), user.getAcr());
                } catch (PasetoException e) {
                    log.debug("Token validation failed: {}", e.getMessage());
                    // Continue without authentication - aspects will enforce requirements
                }
            }

            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
            MDC.remove("userId");
        }
    }

    @Override
    public void destroy() {
        log.info("PasetoAuthenticationFilter destroyed");
    }

    /**
     * Check if the request path should be authenticated.
     */
    private boolean shouldAuthenticate(HttpServletRequest request) {
        String path = request.getRequestURI();
        for (String skipPath : properties.getSkipPaths()) {
            if (pathMatcher.match(skipPath, path)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Extract token from request (cookie first, then header).
     */
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

    /**
     * Validate token and extract claims into AuthenticatedUser.
     */
    private AuthenticatedUser validateAndExtract(String token) throws PasetoException {
        // Try session key first (most common)
        Map<String, Object> claims;
        try {
            claims = parseWithKey(token, sessionKey);
        } catch (PasetoException e) {
            // Try identity key
            claims = parseWithKey(token, identityKey);
        }

        // Extract claims
        String userId = (String) claims.get("sub");
        String acr = (String) claims.getOrDefault("acr", "0");
        Boolean demoMode = (Boolean) claims.getOrDefault("demoMode", true);

        // Parse scopes
        Set<String> scopes = parseScopes((String) claims.get("scope"));

        // Parse AMR
        List<Integer> amr = parseAmr(claims.get("amr"));

        // Parse timestamps
        Instant authTime = parseInstant(claims.get("iat"));
        Instant tokenExpiry = parseInstant(claims.get("exp"));

        return AuthenticatedUser.builder()
                .userId(userId)
                .scopes(scopes)
                .acr(acr)
                .amr(amr)
                .demoMode(demoMode)
                .authTime(authTime)
                .tokenExpiry(tokenExpiry)
                .build();
    }

    private Map<String, Object> parseWithKey(String token, SecretKey key) throws PasetoException {
        Paseto paseto = Pasetos.parserBuilder()
                .setSharedSecret(key)
                .build()
                .parse(token);
        return paseto.getClaims();
    }

    private Set<String> parseScopes(String scopeString) {
        if (scopeString == null || scopeString.isBlank()) {
            return Set.of();
        }
        return new HashSet<>(Arrays.asList(scopeString.split("\\s+")));
    }

    @SuppressWarnings("unchecked")
    private List<Integer> parseAmr(Object amrObj) {
        if (amrObj == null) {
            return List.of();
        }
        if (amrObj instanceof List<?>) {
            List<Integer> result = new ArrayList<>();
            for (Object item : (List<?>) amrObj) {
                if (item instanceof Integer) {
                    result.add((Integer) item);
                } else if (item instanceof Number) {
                    result.add(((Number) item).intValue());
                }
            }
            return result;
        }
        return List.of();
    }

    private Instant parseInstant(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Instant) {
            return (Instant) value;
        }
        if (value instanceof Long) {
            return Instant.ofEpochSecond((Long) value);
        }
        if (value instanceof Number) {
            return Instant.ofEpochSecond(((Number) value).longValue());
        }
        return null;
    }
}
