package io.strategiz.framework.secrets.client;

import io.strategiz.framework.exception.StrategizException;
import io.strategiz.framework.secrets.config.VaultProperties;
import io.strategiz.framework.secrets.exception.SecretsErrors;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * HTTP implementation of VaultClient. Handles all HTTP communication with Vault server.
 */
@Component
public class VaultHttpClient implements VaultClient {

	private static final Logger log = LoggerFactory.getLogger(VaultHttpClient.class);

	private final RestTemplate restTemplate;

	private final VaultProperties properties;

	private final Environment environment;

	/**
	 * Creates a VaultHttpClient with the given properties and environment.
	 * @param properties Vault configuration properties
	 * @param environment Spring environment for accessing configuration
	 */
	@Autowired
	public VaultHttpClient(VaultProperties properties, Environment environment) {
		this.restTemplate = new RestTemplate();
		this.properties = properties;
		this.environment = environment;
		log.info("VaultHttpClient initialized with Vault address: {}", properties.getAddress());
		String token = environment.getProperty("VAULT_TOKEN");
		log.info("VAULT_TOKEN available: {}, length: {}", token != null, token != null ? token.length() : 0);
	}

	@Override
	public Map<String, Object> read(String path) {
		log.info("=== VaultHttpClient.read START ===");
		log.info("Input path: {}", path);
		log.info("Vault address from properties: {}", properties.getAddress());
		try {
			String url = properties.getAddress() + "/v1/" + path;
			log.info("Full Vault URL: {}", url);
			HttpHeaders headers = createHeaders();
			log.info("VAULT_TOKEN present in headers: {}", headers.containsKey("X-Vault-Token"));
			HttpEntity<String> entity = new HttpEntity<>(headers);

			log.info("Making HTTP GET request to Vault...");
			ResponseEntity<Map<String, Object>> response = restTemplate.exchange(url, HttpMethod.GET, entity,
					new ParameterizedTypeReference<Map<String, Object>>() {
					});

			log.info("Response status: {}", response.getStatusCode());
			log.info("Response body present: {}", response.getBody() != null);
			if (response.getBody() != null) {
				log.info("Response body keys: {}", response.getBody().keySet());
			}

			if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
				Map<String, Object> responseBody = response.getBody();
				Object dataObj = responseBody.get("data");
				if (dataObj instanceof Map) {
					Map<?, ?> dataMap = (Map<?, ?>) dataObj;
					// Convert to Map<String, Object> safely
					Map<String, Object> data = new HashMap<>();
					dataMap.forEach((k, v) -> {
						if (k instanceof String) {
							data.put((String) k, v);
						}
					});

					if (data.containsKey("data")) {
						// KV v2 format
						Object innerDataObj = data.get("data");
						if (innerDataObj instanceof Map) {
							Map<?, ?> innerDataMap = (Map<?, ?>) innerDataObj;
							Map<String, Object> innerData = new HashMap<>();
							innerDataMap.forEach((k, v) -> {
								if (k instanceof String) {
									innerData.put((String) k, v);
								}
							});
							return innerData;
						}
					}
					return data;
				}
				else {
					// KV v1 format or direct data
					return null;
				}
			}

			return null;

		}
		catch (HttpClientErrorException e) {
			log.error("=== VaultHttpClient HTTP ERROR ===");
			log.error("HTTP Status: {}", e.getStatusCode());
			log.error("Response body: {}", e.getResponseBodyAsString());
			log.error("Path: {}", path);
			if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
				log.warn("Secret not found at path: {}", path);
				return null;
			}
			throw new StrategizException(SecretsErrors.VAULT_READ_FAILED, "framework-secrets", e, path);
		}
		catch (Exception e) {
			log.error("=== VaultHttpClient UNEXPECTED ERROR ===");
			log.error("Exception type: {}", e.getClass().getName());
			log.error("Message: {}", e.getMessage());
			log.error("Path: {}", path);
			throw e;
		}
	}

	@Override
	public void write(String path, Map<String, Object> data) {
		String url = properties.getAddress() + "/v1/" + path;
		HttpHeaders headers = createHeaders();

		// For KV v2, wrap data in "data" field
		Map<String, Object> payload = new HashMap<>();
		payload.put("data", data);

		HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

		try {
			restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
			log.debug("Successfully wrote to Vault path: {}", path);
		}
		catch (Exception e) {
			throw new StrategizException(SecretsErrors.VAULT_WRITE_FAILED, "framework-secrets", e, path);
		}
	}

	@Override
	public void delete(String path) {
		String url = properties.getAddress() + "/v1/" + path;
		HttpHeaders headers = createHeaders();
		HttpEntity<String> entity = new HttpEntity<>(headers);

		try {
			restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);
			log.debug("Successfully deleted Vault path: {}", path);
		}
		catch (Exception e) {
			throw new StrategizException(SecretsErrors.VAULT_DELETE_FAILED, "framework-secrets", e, path);
		}
	}

	@Override
	public boolean isHealthy() {
		try {
			String url = properties.getAddress() + "/v1/sys/health";
			ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
			return response.getStatusCode() == HttpStatus.OK;
		}
		catch (Exception e) {
			log.debug("Vault health check failed: {}", e.getMessage());
			return false;
		}
	}

	/** Create HTTP headers with Vault token. */
	private HttpHeaders createHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		// Get token from environment or Spring Cloud Vault configuration
		String token = environment.getProperty("VAULT_TOKEN");
		log.info("VAULT_TOKEN from environment.getProperty: {}",
				token != null ? "present (" + token.length() + " chars)" : "NULL");

		if (token == null) {
			token = environment.getProperty("spring.cloud.vault.token", "root-token");
			log.info("Fallback to spring.cloud.vault.token: {}",
					token != null ? "present (" + token.length() + " chars)" : "using default");
		}
		// Trim any whitespace/newlines that might come from Secret Manager
		if (token != null) {
			String originalLength = String.valueOf(token.length());
			token = token.trim().replaceAll("[\\r\\n]", "");
			log.info("Token after trim - original len: {}, new len: {}", originalLength, token.length());
		}
		headers.set("X-Vault-Token", token);
		log.info("X-Vault-Token header set: {}", token != null && !token.isEmpty());

		return headers;
	}

}
