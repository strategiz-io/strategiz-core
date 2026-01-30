package io.strategiz.data.base.config;

import io.strategiz.framework.secrets.controller.SecretManager;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * Configuration class that loads Firebase service account JSON from Vault.
 * Runs before FirebaseConfig so the credentials are available at initialization time.
 *
 * Vault path: secret/strategiz/firebase
 * Required field: service-account-json (full JSON content of the service account file)
 */
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class FirebaseVaultConfig {

	private static final Logger log = LoggerFactory.getLogger(FirebaseVaultConfig.class);

	private static String serviceAccountJson;

	private final SecretManager secretManager;

	@Autowired
	public FirebaseVaultConfig(@Qualifier("vaultSecretService") SecretManager secretManager) {
		this.secretManager = secretManager;
	}

	@PostConstruct
	public void loadFirebasePropertiesFromVault() {
		try {
			log.info("Loading Firebase service account from Vault...");

			String json = secretManager.readSecret("firebase.service-account-json", null);
			if (json != null && !json.isEmpty()) {
				serviceAccountJson = json;
				log.info("Loaded Firebase service account JSON from Vault");
			}
			else {
				log.warn("Firebase service account JSON not found in Vault - will fall back to classpath or env var");
			}

		}
		catch (Exception e) {
			log.error("Failed to load Firebase service account from Vault", e);
		}
	}

	public static String getServiceAccountJson() {
		return serviceAccountJson;
	}

}
