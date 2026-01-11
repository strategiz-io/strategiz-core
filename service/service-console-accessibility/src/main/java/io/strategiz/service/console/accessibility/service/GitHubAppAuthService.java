package io.strategiz.service.console.accessibility.service;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

/**
 * Service for GitHub App authentication. Generates installation access tokens
 * for the Strategiz Accessibility Scanner GitHub App.
 */
@Service
public class GitHubAppAuthService {

	private static final Logger log = LoggerFactory.getLogger(GitHubAppAuthService.class);

	private static final String GITHUB_API_URL = "https://api.github.com";

	@Value("${github.app.id:}")
	private String appId;

	@Value("${github.app.installation-id:}")
	private String installationId;

	@Value("${github.app.private-key:}")
	private String privateKeyPem;

	private final RestTemplate restTemplate;

	private String cachedToken;

	private Instant tokenExpiry;

	public GitHubAppAuthService() {
		this.restTemplate = new RestTemplate();
	}

	/**
	 * Get a valid installation access token. Uses cached token if still valid,
	 * otherwise generates a new one.
	 * @return GitHub installation access token
	 */
	public String getInstallationToken() {
		// Return cached token if still valid
		if (cachedToken != null && tokenExpiry != null && Instant.now().isBefore(tokenExpiry.minusSeconds(60))) {
			log.debug("Using cached GitHub installation token");
			return cachedToken;
		}

		log.info("Generating new GitHub installation token");

		try {
			// Step 1: Generate JWT
			String jwt = generateJWT();

			// Step 2: Get installation access token
			String url = String.format("%s/app/installations/%s/access_tokens", GITHUB_API_URL, installationId);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.set("Authorization", "Bearer " + jwt);
			headers.set("Accept", "application/vnd.github+json");
			headers.set("X-GitHub-Api-Version", "2022-11-28");

			HttpEntity<String> entity = new HttpEntity<>("{}", headers);
			ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

			if (response.getBody() != null) {
				cachedToken = (String) response.getBody().get("token");
				String expiresAt = (String) response.getBody().get("expires_at");
				tokenExpiry = Instant.parse(expiresAt);

				log.info("GitHub installation token generated successfully, expires at: {}", expiresAt);
				return cachedToken;
			}

			throw new RuntimeException("Failed to get installation token: empty response");

		}
		catch (Exception e) {
			log.error("Failed to generate GitHub installation token: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to authenticate with GitHub App", e);
		}
	}

	/**
	 * Generate a JWT for GitHub App authentication.
	 * @return JWT token
	 */
	private String generateJWT() {
		try {
			// Parse private key
			PrivateKey privateKey = parsePrivateKey(privateKeyPem);

			// Generate JWT (valid for 10 minutes)
			Instant now = Instant.now();
			Instant expiry = now.plusSeconds(600);

			return Jwts.builder()
				.setIssuedAt(Date.from(now.minusSeconds(60))) // 60 seconds in the past
				.setExpiration(Date.from(expiry))
				.setIssuer(appId)
				.signWith(privateKey, SignatureAlgorithm.RS256)
				.compact();

		}
		catch (Exception e) {
			log.error("Failed to generate JWT: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to generate GitHub App JWT", e);
		}
	}

	/**
	 * Parse PEM-encoded private key.
	 * @param pemKey PEM-encoded private key
	 * @return PrivateKey object
	 */
	private PrivateKey parsePrivateKey(String pemKey) throws Exception {
		// Remove PEM headers and whitespace
		String privateKeyPEM = pemKey.replace("-----BEGIN RSA PRIVATE KEY-----", "")
			.replace("-----BEGIN PRIVATE KEY-----", "")
			.replace("-----END RSA PRIVATE KEY-----", "")
			.replace("-----END PRIVATE KEY-----", "")
			.replaceAll("\\s", "");

		// Decode base64
		byte[] encoded = Base64.getDecoder().decode(privateKeyPEM);

		// Handle both PKCS#1 and PKCS#8 formats
		try {
			// Try PKCS#8 first
			PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			return keyFactory.generatePrivate(keySpec);
		}
		catch (Exception e) {
			// If PKCS#8 fails, try converting from PKCS#1
			// This is a simplified conversion - in production you might need a library
			// like BouncyCastle for proper PKCS#1 support
			throw new RuntimeException("Private key must be in PKCS#8 format. "
					+ "Convert using: openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in key.pem", e);
		}
	}

	/**
	 * Check if GitHub App is configured.
	 * @return true if all required credentials are present
	 */
	public boolean isConfigured() {
		return appId != null && !appId.isEmpty() && installationId != null && !installationId.isEmpty()
				&& privateKeyPem != null && !privateKeyPem.isEmpty();
	}

}
