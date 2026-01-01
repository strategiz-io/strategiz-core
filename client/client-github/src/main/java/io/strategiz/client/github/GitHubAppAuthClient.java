package io.strategiz.client.github;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.StringReader;
import java.security.PrivateKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * GitHub App authentication client
 * Handles JWT generation and installation token management
 */
@Component
public class GitHubAppAuthClient {

    private static final Logger log = LoggerFactory.getLogger(GitHubAppAuthClient.class);
    private static final String GITHUB_API_URL = "https://api.github.com";

    private final GitHubAppConfig config;
    private final RestTemplate restTemplate;
    private PrivateKey privateKey;
    private String cachedToken;
    private Instant tokenExpiry;

    public GitHubAppAuthClient(GitHubAppConfig config, RestTemplate restTemplate) {
        this.config = config;
        this.restTemplate = restTemplate;
        this.privateKey = loadPrivateKey();
    }

    /**
     * Get installation access token for GitHub API calls
     * Generates new token if expired or not cached
     */
    public String getInstallationToken(String owner, String repo) {
        if (cachedToken != null && tokenExpiry != null && Instant.now().isBefore(tokenExpiry)) {
            return cachedToken;
        }

        try {
            // Step 1: Generate JWT
            String jwt = generateJWT();

            // Step 2: Find installation ID for the repository
            Long installationId = getInstallationId(jwt, owner, repo);
            if (installationId == null) {
                log.error("No installation found for repository {}/{}", owner, repo);
                return null;
            }

            // Step 3: Get installation access token
            String token = getInstallationAccessToken(jwt, installationId);
            if (token != null) {
                cachedToken = token;
                // GitHub installation tokens expire after 1 hour, cache for 50 minutes
                tokenExpiry = Instant.now().plus(50, ChronoUnit.MINUTES);
            }

            return token;

        } catch (Exception e) {
            log.error("Error getting installation token: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Generate JWT signed with GitHub App private key
     */
    private String generateJWT() {
        Instant now = Instant.now();
        Instant expiry = now.plus(10, ChronoUnit.MINUTES);

        return Jwts.builder()
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiry))
                .setIssuer(config.getAppId())
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    /**
     * Get installation ID for a specific repository
     */
    private Long getInstallationId(String jwt, String owner, String repo) {
        try {
            // First, try to get installation for the specific repo
            String url = String.format("%s/repos/%s/%s/installation", GITHUB_API_URL, owner, repo);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + jwt);
            headers.set("Accept", "application/vnd.github+json");

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            Map<String, Object> installation = response.getBody();
            if (installation != null && installation.containsKey("id")) {
                return ((Number) installation.get("id")).longValue();
            }

        } catch (Exception e) {
            log.debug("Could not get installation for {}/{}, trying to list all installations", owner, repo);

            // Fallback: List all installations and find the right one
            try {
                String url = String.format("%s/app/installations", GITHUB_API_URL);

                HttpHeaders headers = new HttpHeaders();
                headers.set("Authorization", "Bearer " + jwt);
                headers.set("Accept", "application/vnd.github+json");

                HttpEntity<String> entity = new HttpEntity<>(headers);
                ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, entity, List.class);

                List<Map<String, Object>> installations = response.getBody();
                if (installations != null && !installations.isEmpty()) {
                    // Return the first installation ID
                    Map<String, Object> firstInstallation = installations.get(0);
                    return ((Number) firstInstallation.get("id")).longValue();
                }

            } catch (Exception ex) {
                log.error("Could not list installations: {}", ex.getMessage());
            }
        }

        return null;
    }

    /**
     * Get installation access token using JWT
     */
    private String getInstallationAccessToken(String jwt, Long installationId) {
        try {
            String url = String.format("%s/app/installations/%d/access_tokens", GITHUB_API_URL, installationId);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + jwt);
            headers.set("Accept", "application/vnd.github+json");

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            Map<String, Object> tokenResponse = response.getBody();
            if (tokenResponse != null && tokenResponse.containsKey("token")) {
                log.info("Successfully obtained installation access token");
                return (String) tokenResponse.get("token");
            }

        } catch (Exception e) {
            log.error("Error getting installation access token: {}", e.getMessage());
        }

        return null;
    }

    /**
     * Load private key from PEM format string
     */
    private PrivateKey loadPrivateKey() {
        try {
            String privateKeyPem = config.getPrivateKey();
            if (privateKeyPem == null || privateKeyPem.isEmpty()) {
                throw new IllegalStateException("GitHub App private key not configured");
            }

            // Parse PEM format
            try (PEMParser pemParser = new PEMParser(new StringReader(privateKeyPem))) {
                Object object = pemParser.readObject();
                JcaPEMKeyConverter converter = new JcaPEMKeyConverter();

                if (object instanceof PrivateKeyInfo) {
                    return converter.getPrivateKey((PrivateKeyInfo) object);
                } else {
                    throw new IllegalStateException("Invalid private key format");
                }
            }

        } catch (Exception e) {
            log.error("Error loading private key: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to load GitHub App private key", e);
        }
    }

    /**
     * Clear cached token (force refresh on next call)
     */
    public void clearCache() {
        cachedToken = null;
        tokenExpiry = null;
    }

}
