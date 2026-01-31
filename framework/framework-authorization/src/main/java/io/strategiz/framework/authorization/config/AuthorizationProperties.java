package io.strategiz.framework.authorization.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the authorization framework.
 *
 * <p>
 * Example configuration:
 *
 * <pre>
 * strategiz.authorization.enabled=true
 * strategiz.authorization.skip-paths=/actuator/**,/v3/api-docs/**,/v1/auth/**
 * </pre>
 */
@ConfigurationProperties(prefix = "strategiz.authorization")
public class AuthorizationProperties {

	/** Whether the authorization framework is enabled. */
	private boolean enabled = true;

	/**
	 * Paths to skip authentication (Ant-style patterns). Default: actuator, OpenAPI docs,
	 * and auth endpoints.
	 */
	private List<String> skipPaths = new ArrayList<>(List.of("/actuator/**", "/v3/api-docs/**", "/swagger-ui/**",
			"/v1/auth/**", "/v1/auth/oauth/**", "/health", "/info"));

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public List<String> getSkipPaths() {
		return skipPaths;
	}

	public void setSkipPaths(List<String> skipPaths) {
		this.skipPaths = skipPaths;
	}

}
