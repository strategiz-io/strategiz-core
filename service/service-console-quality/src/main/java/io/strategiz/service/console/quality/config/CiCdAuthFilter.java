package io.strategiz.service.console.quality.config;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Authentication filter for CI/CD quality analysis endpoints.
 *
 * Validates Bearer token against Vault-stored CI/CD token for POST /cache
 * endpoint.
 */
@Component
public class CiCdAuthFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(CiCdAuthFilter.class);

	private static final String CACHE_ENDPOINT = "/v1/console/quality/cache";

	@Autowired
	private CiCdAuthConfig ciCdAuthConfig;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String requestUri = request.getRequestURI();
		String method = request.getMethod();

		// Only protect POST to /cache endpoint
		if ("POST".equals(method) && requestUri.equals(CACHE_ENDPOINT)) {
			log.debug("Authenticating CI/CD request to {}", requestUri);

			// Extract Authorization header
			String authHeader = request.getHeader("Authorization");

			if (authHeader == null || !authHeader.startsWith("Bearer ")) {
				log.warn("Missing or invalid Authorization header for CI/CD endpoint");
				response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing Authorization header");
				return;
			}

			String token = authHeader.substring(7); // Remove "Bearer " prefix

			// Validate token
			if (!ciCdAuthConfig.isValidToken(token)) {
				log.warn("Invalid CI/CD token provided");
				response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid API token");
				return;
			}

			log.info("CI/CD authentication successful");
		}

		// Continue with request
		filterChain.doFilter(request, response);
	}

}
