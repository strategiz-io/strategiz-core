package io.strategiz.client.github;

import io.strategiz.framework.secrets.controller.SecretManager;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class that loads GitHub App credentials from Vault and injects them into
 * GitHubAppConfig. This ensures all GitHub App secrets are loaded from Vault at startup.
 *
 * Vault path: secret/strategiz/github-app
 * Required fields:
 * - app-id: GitHub App ID (numeric string, e.g. "2564716")
 * - private-key: GitHub App private key in PEM format (the full content of the .pem file)
 */
@Configuration
public class GitHubAppVaultConfig {

    private static final Logger log = LoggerFactory.getLogger(GitHubAppVaultConfig.class);

    private final SecretManager secretManager;
    private final GitHubAppConfig githubAppConfig;

    @Autowired
    public GitHubAppVaultConfig(
            @Qualifier("vaultSecretManager") SecretManager secretManager,
            GitHubAppConfig githubAppConfig
    ) {
        this.secretManager = secretManager;
        this.githubAppConfig = githubAppConfig;
    }

    @PostConstruct
    public void loadGitHubAppPropertiesFromVault() {
        try {
            log.info("Loading GitHub App configuration...");

            // Try environment variables first (for production/Cloud Run)
            String appId = System.getenv("GITHUB_APP_ID");
            String privateKey = System.getenv("GITHUB_APP_PRIVATE_KEY");

            if (appId != null && !appId.isEmpty()) {
                githubAppConfig.setAppId(appId);
                log.info("Loaded GitHub App ID from environment variable: {}", appId);
            } else {
                // Fall back to Vault (for local development)
                log.info("GitHub App ID not in environment, checking Vault...");
                appId = secretManager.readSecret("github-app.app-id", null);
                if (appId != null && !appId.isEmpty()) {
                    githubAppConfig.setAppId(appId);
                    log.info("Loaded GitHub App ID from Vault: {}", appId);
                } else {
                    log.warn("GitHub App ID not found in environment or Vault - Automation & Agents will be disabled");
                }
            }

            if (privateKey != null && !privateKey.isEmpty()) {
                githubAppConfig.setPrivateKey(privateKey);
                log.info("Loaded GitHub App private key from environment variable");
            } else {
                // Fall back to Vault (for local development)
                log.info("GitHub App private key not in environment, checking Vault...");
                privateKey = secretManager.readSecret("github-app.private-key", null);
                if (privateKey != null && !privateKey.isEmpty()) {
                    githubAppConfig.setPrivateKey(privateKey);
                    log.info("Loaded GitHub App private key from Vault");
                } else {
                    log.warn("GitHub App private key not found in environment or Vault - Automation & Agents will be disabled");
                }
            }

            // Load installation ID (required for GitHub App authentication)
            String installationId = System.getenv("GITHUB_APP_INSTALLATION_ID");
            if (installationId != null && !installationId.isEmpty()) {
                githubAppConfig.setInstallationId(installationId);
                log.info("Loaded GitHub App installation ID from environment variable: {}", installationId);
            } else {
                // Fall back to Vault (for local development)
                log.info("GitHub App installation ID not in environment, checking Vault...");
                installationId = secretManager.readSecret("github-app.installation-id", null);
                if (installationId != null && !installationId.isEmpty()) {
                    githubAppConfig.setInstallationId(installationId);
                    log.info("Loaded GitHub App installation ID from Vault: {}", installationId);
                } else {
                    log.warn("GitHub App installation ID not found in environment or Vault - Automation & Agents will be disabled");
                }
            }

            if (githubAppConfig.hasAllCredentials()) {
                githubAppConfig.setEnabled(true);
                log.info("GitHub App configuration loaded successfully - Automation & Agents enabled");
            } else {
                log.warn("GitHub App is not fully configured - Automation & Agents will be disabled");
            }

        } catch (Exception e) {
            log.error("Failed to load GitHub App configuration", e);
        }
    }

}
