package io.strategiz.data.auth.repository.passkey.challenge;

import io.strategiz.data.auth.model.passkey.PasskeyChallenge;
import io.strategiz.data.base.exception.DataRepositoryErrorDetails;
import io.strategiz.data.base.exception.DataRepositoryException;
import io.strategiz.data.base.repository.BaseRepository;
import com.google.cloud.firestore.Firestore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of PasskeyChallengeRepository using Firestore
 */
@Repository
public class PasskeyChallengeRepositoryImpl extends BaseRepository<PasskeyChallenge>
		implements PasskeyChallengeRepository {

	private static final Logger log = LoggerFactory.getLogger(PasskeyChallengeRepositoryImpl.class);

	private static final String SYSTEM_USER_ID = "SYSTEM";

	@Autowired
	public PasskeyChallengeRepositoryImpl(Firestore firestore) {
		super(firestore, PasskeyChallenge.class);
	}

	@Override
	protected String getModuleName() {
		return "data-auth";
	}

	@Override
	public Optional<PasskeyChallenge> findByChallenge(String challenge) {
		// Use direct query without isActive filter for PasskeyChallenge
		return findByFieldWithoutActiveFilter("challenge", challenge).stream().findFirst();
	}

	// Custom method to query without isActive filter since PasskeyChallenge doesn't use
	// soft deletes
	private List<PasskeyChallenge> findByFieldWithoutActiveFilter(String fieldName, Object value) {
		try {
			log.debug("Querying {} = {} without isActive filter", fieldName, value);
			var query = getCollection().whereEqualTo(fieldName, value);
			var docs = query.get().get().getDocuments();
			log.debug("Query found {} documents", docs.size());

			return docs.stream().map(doc -> {
				PasskeyChallenge challenge = doc.toObject(PasskeyChallenge.class);
				challenge.setId(doc.getId());
				log.debug("Found challenge: ID={}, challenge={}, userId={}", challenge.getId(),
						challenge.getChallenge(), challenge.getUserId());
				return challenge;
			}).collect(java.util.stream.Collectors.toList());
		}
		catch (Exception e) {
			log.error("Error in findByFieldWithoutActiveFilter", e);
			return java.util.Collections.emptyList();
		}
	}

	@Override
	public List<PasskeyChallenge> findByUserId(String userId) {
		return findByFieldWithoutActiveFilter("userId", userId);
	}

	@Override
	public List<PasskeyChallenge> findBySessionId(String sessionId) {
		return findByFieldWithoutActiveFilter("sessionId", sessionId);
	}

	@Override
	public List<PasskeyChallenge> findByChallengeType(String type) {
		return findByFieldWithoutActiveFilter("type", type);
	}

	@Override
	public List<PasskeyChallenge> findByType(String type) {
		return findByFieldWithoutActiveFilter("type", type);
	}

	@Override
	public List<PasskeyChallenge> findByCreatedAtBefore(Instant before) {
		return findAll().stream().filter(c -> c.getCreatedAt().isBefore(before)).toList();
	}

	@Override
	public List<PasskeyChallenge> findByExpiresAtBefore(Instant now) {
		return findAll().stream().filter(c -> c.getExpiresAt().isBefore(now)).toList();
	}

	@Override
	public List<PasskeyChallenge> findByUsedTrue() {
		return findByField("used", true);
	}

	@Override
	public boolean existsByChallenge(String challenge) {
		return !findByFieldWithoutActiveFilter("challenge", challenge).isEmpty();
	}

	@Override
	public void deleteByExpiresAtBefore(Instant now) {
		List<PasskeyChallenge> expired = findByExpiresAtBefore(now);
		expired.forEach(this::delete); // Use the hard delete method
	}

	@Override
	public void deleteByUsedTrue() {
		List<PasskeyChallenge> used = findByUsedTrue();
		used.forEach(this::delete); // Use the hard delete method
	}

	@Override
	public void deleteByUserId(String userId) {
		List<PasskeyChallenge> userChallenges = findByUserId(userId);
		userChallenges.forEach(this::delete); // Use the hard delete method
	}

	// Repository method implementations
	@Override
	public PasskeyChallenge save(PasskeyChallenge challenge) {
		// If challenge has a userId, use it; otherwise use SYSTEM user constant
		String userId = challenge.getUserId() != null ? challenge.getUserId() : SYSTEM_USER_ID;
		return save(challenge, userId);
	}

	@Override
	public PasskeyChallenge saveAndFlush(PasskeyChallenge challenge) {
		return save(challenge);
	}

	@Override
	public void delete(PasskeyChallenge challenge) {
		// For PasskeyChallenge, we do hard delete since they're temporary
		// and don't need audit trail
		if (challenge.getId() != null) {
			try {
				getCollection().document(challenge.getId()).delete().get();
				log.debug("Hard deleted PasskeyChallenge with ID: {}", challenge.getId());
			}
			catch (Exception e) {
				log.error("Failed to delete PasskeyChallenge: {}", challenge.getId(), e);
				throw new DataRepositoryException(DataRepositoryErrorDetails.ENTITY_DELETE_FAILED, e,
						"PasskeyChallenge", challenge.getId());
			}
		}
	}

	@Override
	public void deleteAll(Iterable<? extends PasskeyChallenge> challenges) {
		challenges.forEach(this::delete);
	}

	// Debug method to inspect what's actually in Firestore
	public void debugFirestoreContents() {
		try {
			log.debug("=== DEBUG: Raw Firestore Contents ===");
			var allDocs = getCollection().get().get().getDocuments();
			log.debug("Total documents in collection: {}", allDocs.size());

			for (var doc : allDocs) {
				log.debug("Document ID: {}", doc.getId());
				log.debug("Document data: {}", doc.getData());

				// Try to convert to object
				try {
					PasskeyChallenge challenge = doc.toObject(PasskeyChallenge.class);
					if (challenge != null) {
						challenge.setId(doc.getId());
						log.debug("Converted object - challenge: {}, userId: {}, isActive: {}",
								challenge.getChallenge(), challenge.getUserId(), challenge.getIsActive());
					}
				}
				catch (Exception e) {
					log.debug("Failed to convert document to PasskeyChallenge: {}", e.getMessage());
				}
			}
			log.debug("=== END DEBUG ===");
		}
		catch (Exception e) {
			log.error("Error in debugFirestoreContents", e);
		}
	}

}