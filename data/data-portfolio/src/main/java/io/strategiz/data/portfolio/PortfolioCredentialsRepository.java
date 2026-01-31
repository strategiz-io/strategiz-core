package io.strategiz.data.portfolio;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.Firestore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;

/**
 * Repository for accessing portfolio and brokerage credentials
 */
@Repository
public class PortfolioCredentialsRepository {

	private static final Logger log = LoggerFactory.getLogger(PortfolioCredentialsRepository.class);

	private final Firestore firestore;

	@Autowired
	public PortfolioCredentialsRepository(Firestore firestore) {
		this.firestore = firestore;
	}

	/**
	 * Get brokerage credentials for a user from the api_credentials subcollection
	 * @param userId User ID (email)
	 * @param provider Provider name (e.g., "robinhood", "coinbase")
	 * @return Map with credentials, or null if not found
	 */
	public Map<String, String> getBrokerageCredentials(String userId, String provider) {
		try {
			log.info("Getting {} credentials for user: {}", provider, userId);

			QuerySnapshot querySnapshot = firestore.collection("users")
				.document(userId)
				.collection("api_credentials")
				.whereEqualTo("provider", provider)
				.get()
				.get(); // Use get() to wait for the ApiFuture result

			// Check if we have results
			if (querySnapshot.isEmpty()) {
				log.info("No {} credentials found for user: {}", provider, userId);
				return null;
			}

			// Get the credential document
			DocumentSnapshot document = querySnapshot.getDocuments().get(0);
			log.info("Found {} credentials for user: {}", provider, userId);

			// Build the credentials map
			Map<String, String> credentials = new HashMap<>();
			credentials.put("apiKey", document.getString("apiKey"));
			credentials.put("secretKey", document.getString("secretKey"));
			credentials.put("passphrase", document.getString("passphrase"));

			return credentials;
		}
		catch (IllegalStateException e) {
			log.warn("Document storage not initialized. Cannot retrieve {} credentials for user: {}", provider, userId,
					e);
			return null;
		}
		catch (Exception e) {
			log.error("Error getting {} credentials for user: {}", provider, userId, e);
			return null;
		}
	}

}
