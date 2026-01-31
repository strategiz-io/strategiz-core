package io.strategiz.service.marketdata.config;

import io.strategiz.framework.secrets.controller.SecretManager;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * Configuration for admin API key authentication. Protects /api/admin/* endpoints with an
 * API key stored in Vault.
 *
 * Usage: Include header X-Admin-Key: <admin-api-key>
 *
 * Vault path: admin.api-key
 *
 * Can be disabled via ADMIN_API_KEY_ENABLED=false environment variable
 */
@Configuration
public class AdminApiKeyConfig {

	private static final Logger log = LoggerFactory.getLogger(AdminApiKeyConfig.class);

	private static final String ADMIN_KEY_HEADER = "X-Admin-Key";

	private final SecretManager secretManager;

	private String adminApiKey;

	@Value("${admin.api-key.enabled:true}")
	private boolean adminApiKeyEnabled;

	@Autowired
	public AdminApiKeyConfig(@Qualifier("vaultSecretService") SecretManager secretManager) {
		this.secretManager = secretManager;
		loadAdminApiKey();
	}

	private void loadAdminApiKey() {
		try {
			// Read from secret/strategiz/admin path
			this.adminApiKey = secretManager.readSecret("admin.api-key");
			if (adminApiKey != null && !adminApiKey.isEmpty()) {
				log.info("Loaded admin API key from Vault");
			}
			else {
				log.warn("Admin API key not found in Vault at 'admin.api-key' - admin endpoints will be unprotected!");
			}
		}
		catch (Exception e) {
			log.error("Failed to load admin API key from Vault: {}", e.getMessage());
		}
	}

	@Bean
	public FilterRegistrationBean<AdminApiKeyFilter> adminApiKeyFilter() {
		FilterRegistrationBean<AdminApiKeyFilter> registrationBean = new FilterRegistrationBean<>();
		registrationBean.setFilter(new AdminApiKeyFilter());
		registrationBean.addUrlPatterns("/api/admin/*");
		registrationBean.setOrder(1);
		return registrationBean;
	}

	/**
	 * Filter that validates admin API key on /api/admin/* endpoints.
	 */
	private class AdminApiKeyFilter implements Filter {

		@Override
		public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
				throws IOException, ServletException {

			HttpServletRequest httpRequest = (HttpServletRequest) request;
			HttpServletResponse httpResponse = (HttpServletResponse) response;

			// Skip filter if admin API key is disabled
			if (!adminApiKeyEnabled) {
				log.warn("Admin API key check DISABLED - allowing request to {}", httpRequest.getRequestURI());
				chain.doFilter(request, response);
				return;
			}

			// Skip filter if no admin key configured (for backward compatibility)
			if (adminApiKey == null || adminApiKey.isEmpty()) {
				log.warn("Admin API key not configured - allowing request to {}", httpRequest.getRequestURI());
				chain.doFilter(request, response);
				return;
			}

			// Check for admin key header
			String providedKey = httpRequest.getHeader(ADMIN_KEY_HEADER);

			if (providedKey == null || providedKey.isEmpty()) {
				log.warn("Admin API request without key from {}: {}", httpRequest.getRemoteAddr(),
						httpRequest.getRequestURI());
				httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				httpResponse.setContentType("application/json");
				httpResponse.getWriter().write("{\"error\":\"Missing X-Admin-Key header\"}");
				return;
			}

			if (!adminApiKey.equals(providedKey)) {
				log.warn("Invalid admin API key from {}: {}", httpRequest.getRemoteAddr(), httpRequest.getRequestURI());
				httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
				httpResponse.setContentType("application/json");
				httpResponse.getWriter().write("{\"error\":\"Invalid admin API key\"}");
				return;
			}

			// Valid key - proceed
			log.debug("Valid admin API key for request: {}", httpRequest.getRequestURI());
			chain.doFilter(request, response);
		}

	}

}
