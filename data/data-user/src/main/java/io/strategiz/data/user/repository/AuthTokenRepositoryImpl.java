package io.strategiz.data.user.repository;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import io.strategiz.data.user.entity.AuthTokenEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Firestore implementation of AuthTokenRepository.
 * Stores one-time auth tokens for cross-app SSO.
 *
 * Simple implementation without BaseRepository overhead since
 * tokens are ephemeral and don't need lifecycle management.
 */
@Repository
public class AuthTokenRepositoryImpl implements AuthTokenRepository {

    private static final Logger logger = LoggerFactory.getLogger(AuthTokenRepositoryImpl.class);
    private static final String COLLECTION_NAME = "auth_tokens";

    private final Firestore firestore;

    public AuthTokenRepositoryImpl(Firestore firestore) {
        this.firestore = firestore;
    }

    @Override
    public Optional<AuthTokenEntity> findByToken(String token) {
        try {
            DocumentSnapshot doc = firestore.collection(COLLECTION_NAME)
                    .document(token)
                    .get()
                    .get();

            if (doc.exists()) {
                AuthTokenEntity entity = doc.toObject(AuthTokenEntity.class);
                if (entity != null) {
                    entity.setToken(doc.getId());
                }
                return Optional.ofNullable(entity);
            }
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Error finding auth token: {}", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public AuthTokenEntity save(AuthTokenEntity token) {
        try {
            firestore.collection(COLLECTION_NAME)
                    .document(token.getToken())
                    .set(token)
                    .get();
            logger.debug("Saved auth token for user: {}", token.getUserId());
            return token;
        } catch (Exception e) {
            logger.error("Error saving auth token: {}", e.getMessage());
            throw new RuntimeException("Failed to save auth token", e);
        }
    }

    @Override
    public void delete(String token) {
        try {
            firestore.collection(COLLECTION_NAME)
                    .document(token)
                    .delete()
                    .get();
            logger.debug("Deleted auth token: {}", token.substring(0, Math.min(8, token.length())) + "...");
        } catch (Exception e) {
            logger.error("Error deleting auth token: {}", e.getMessage());
        }
    }

    @Override
    public void markAsUsed(String token) {
        try {
            firestore.collection(COLLECTION_NAME)
                    .document(token)
                    .update("used", true)
                    .get();
            logger.debug("Marked auth token as used: {}", token.substring(0, Math.min(8, token.length())) + "...");
        } catch (Exception e) {
            logger.error("Error marking auth token as used: {}", e.getMessage());
        }
    }
}
