package io.strategiz.data.auth.repository.passkey.challenge;

import io.strategiz.data.auth.model.passkey.PasskeyChallenge;
import io.strategiz.data.base.repository.BaseRepository;
import com.google.cloud.firestore.Firestore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of PasskeyChallengeRepository using Firestore
 */
@Repository
public class PasskeyChallengeRepositoryImpl extends BaseRepository<PasskeyChallenge> implements PasskeyChallengeRepository {
    
    @Autowired
    public PasskeyChallengeRepositoryImpl(Firestore firestore) {
        super(firestore, PasskeyChallenge.class);
    }

    @Override
    public Optional<PasskeyChallenge> findByChallenge(String challenge) {
        return findByField("challenge", challenge).stream().findFirst();
    }

    @Override
    public List<PasskeyChallenge> findByUserId(String userId) {
        return findByField("userId", userId);
    }

    @Override
    public List<PasskeyChallenge> findBySessionId(String sessionId) {
        return findByField("sessionId", sessionId);
    }

    @Override
    public List<PasskeyChallenge> findByChallengeType(String type) {
        return findByField("type", type);
    }

    @Override
    public List<PasskeyChallenge> findByType(String type) {
        return findByField("type", type);
    }

    @Override
    public List<PasskeyChallenge> findByCreatedAtBefore(Instant before) {
        return findAll().stream()
            .filter(c -> c.getCreatedAt().isBefore(before))
            .toList();
    }

    @Override
    public List<PasskeyChallenge> findByExpiresAtBefore(Instant now) {
        return findAll().stream()
            .filter(c -> c.getExpiresAt().isBefore(now))
            .toList();
    }

    @Override
    public List<PasskeyChallenge> findByUsedTrue() {
        return findByField("used", true);
    }

    @Override
    public boolean existsByChallenge(String challenge) {
        return existsByField("challenge", challenge);
    }

    @Override
    public void deleteByExpiresAtBefore(Instant now) {
        List<PasskeyChallenge> expired = findByExpiresAtBefore(now);
        expired.forEach(c -> delete(c.getId(), "system"));
    }

    @Override
    public void deleteByUsedTrue() {
        List<PasskeyChallenge> used = findByUsedTrue();
        used.forEach(c -> delete(c.getId(), "system"));
    }

    @Override
    public void deleteByUserId(String userId) {
        List<PasskeyChallenge> userChallenges = findByUserId(userId);
        userChallenges.forEach(c -> delete(c.getId(), "system"));
    }

    // Repository method implementations
    @Override
    public PasskeyChallenge save(PasskeyChallenge challenge) {
        return save(challenge, "system");
    }

    @Override
    public PasskeyChallenge saveAndFlush(PasskeyChallenge challenge) {
        return save(challenge, "system");
    }

    @Override
    public void delete(PasskeyChallenge challenge) {
        delete(challenge.getId(), "system");
    }

    @Override
    public void deleteAll(Iterable<? extends PasskeyChallenge> challenges) {
        challenges.forEach(this::delete);
    }
}