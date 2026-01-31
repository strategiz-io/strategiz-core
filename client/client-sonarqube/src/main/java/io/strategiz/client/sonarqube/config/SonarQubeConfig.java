package io.strategiz.client.sonarqube.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for SonarQube API client. Loads configuration from application.properties
 * or Vault (via SonarQubeVaultConfig).
 */
@Configuration
@ConfigurationProperties(prefix = "sonarqube")
public class SonarQubeConfig {

	private String url;

	private String token;

	private String projectKey;

	public SonarQubeConfig() {
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getProjectKey() {
		return projectKey;
	}

	public void setProjectKey(String projectKey) {
		this.projectKey = projectKey;
	}

	/**
	 * RestTemplate for SonarQube API calls.
	 */
	@Bean
	public RestTemplate sonarQubeRestTemplate() {
		return new RestTemplate();
	}

}
