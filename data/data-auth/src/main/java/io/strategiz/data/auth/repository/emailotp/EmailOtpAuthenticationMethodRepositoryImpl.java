package io.strategiz.data.auth.repository.emailotp;

import io.strategiz.data.auth.entity.emailotp.EmailOtpAuthenticationMethodEntity;
import io.strategiz.data.base.repository.BaseRepository;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.CollectionReference;
import com.google.api.core.ApiFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Firestore implementation of EmailOtpAuthenticationMethodRepository
 * Provides CRUD operations for Email OTP authentication methods stored in Firestore
 */
@Repository
public class EmailOtpAuthenticationMethodRepositoryImpl implements EmailOtpAuthenticationMethodRepository {

    private static final Logger log = LoggerFactory.getLogger(EmailOtpAuthenticationMethodRepositoryImpl.class);
    
    private final BaseRepository<EmailOtpAuthenticationMethodEntity> baseRepository;
    private final Firestore firestore;

    @Autowired
    public EmailOtpAuthenticationMethodRepositoryImpl(Firestore firestore) {
        this.firestore = firestore;
        this.baseRepository = new BaseRepository<EmailOtpAuthenticationMethodEntity>(firestore, EmailOtpAuthenticationMethodEntity.class) {};
    }

    // ===============================
    // CrudRepository Implementation
    // ===============================

    @Override
    public <S extends EmailOtpAuthenticationMethodEntity> S save(S entity) {
        String userId = entity.getUserId() != null ? entity.getUserId() : "system";
        return (S) baseRepository.save(entity, userId);
    }

    @Override
    public <S extends EmailOtpAuthenticationMethodEntity> Iterable<S> saveAll(Iterable<S> entities) {
        return StreamSupport.stream(entities.spliterator(), false)
                .map(this::save)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<EmailOtpAuthenticationMethodEntity> findById(String id) {
        return baseRepository.findById(id);
    }

    @Override
    public boolean existsById(String id) {
        return findById(id).isPresent();
    }

    @Override
    public Iterable<EmailOtpAuthenticationMethodEntity> findAll() {
        return baseRepository.findAll();
    }

    @Override
    public Iterable<EmailOtpAuthenticationMethodEntity> findAllById(Iterable<String> ids) {
        return StreamSupport.stream(ids.spliterator(), false)
                .map(this::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    @Override
    public long count() {
        return baseRepository.findAll().size();
    }

    @Override
    public void deleteById(String id) {
        String userId = "system"; // TODO: Get from SecurityContext when available
        baseRepository.delete(id, userId);
    }

    @Override
    public void delete(EmailOtpAuthenticationMethodEntity entity) {
        deleteById(entity.getId());
    }

    @Override
    public void deleteAllById(Iterable<? extends String> ids) {
        ids.forEach(this::deleteById);
    }

    @Override
    public void deleteAll(Iterable<? extends EmailOtpAuthenticationMethodEntity> entities) {
        entities.forEach(this::delete);
    }

    @Override
    public void deleteAll() {
        List<EmailOtpAuthenticationMethodEntity> all = baseRepository.findAll();
        all.forEach(this::delete);
    }

    // ===============================
    // Custom Query Methods
    // ===============================

    @Override
    public Optional<EmailOtpAuthenticationMethodEntity> findByEmail(String email) {
        try {
            Query query = getCollection()
                    .whereEqualTo("email", email)
                    .whereEqualTo("auditFields.isActive", true)
                    .limit(1);
            List<EmailOtpAuthenticationMethodEntity> results = executeQuery(query);
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (Exception e) {
            throw new RuntimeException("Failed to find Email OTP method by email", e);
        }
    }

    @Override
    public List<EmailOtpAuthenticationMethodEntity> findByUserIdAndVerified(String userId, boolean verified) {
        try {
            Query query = getCollection()
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("verified", verified)
                    .whereEqualTo("auditFields.isActive", true);
            return executeQuery(query);
        } catch (Exception e) {
            throw new RuntimeException("Failed to find Email OTP methods by user ID and verified status", e);
        }
    }

    @Override
    public List<EmailOtpAuthenticationMethodEntity> findByUserIdAndEnabled(String userId, boolean enabled) {
        try {
            Query query = getCollection()
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("enabled", enabled)
                    .whereEqualTo("auditFields.isActive", true);
            return executeQuery(query);
        } catch (Exception e) {
            throw new RuntimeException("Failed to find Email OTP methods by user ID and enabled status", e);
        }
    }

    @Override
    public List<EmailOtpAuthenticationMethodEntity> findByLastOtpSentAtAfter(Instant timestamp) {
        try {
            Query query = getCollection()
                    .whereGreaterThan("lastOtpSentAt", timestamp)
                    .whereEqualTo("auditFields.isActive", true);
            return executeQuery(query);
        } catch (Exception e) {
            throw new RuntimeException("Failed to find Email OTP methods by last OTP sent time", e);
        }
    }

    @Override
    public List<EmailOtpAuthenticationMethodEntity> findByDailyEmailCountGreaterThan(int count) {
        try {
            Query query = getCollection()
                    .whereGreaterThan("dailyEmailCount", count)
                    .whereEqualTo("auditFields.isActive", true);
            return executeQuery(query);
        } catch (Exception e) {
            throw new RuntimeException("Failed to find Email OTP methods by daily email count", e);
        }
    }

    @Override
    public boolean existsByEmailAndVerified(String email, boolean verified) {
        try {
            Query query = getCollection()
                    .whereEqualTo("email", email)
                    .whereEqualTo("verified", verified)
                    .whereEqualTo("auditFields.isActive", true)
                    .limit(1);
            return !executeQuery(query).isEmpty();
        } catch (Exception e) {
            throw new RuntimeException("Failed to check if email exists with verified status", e);
        }
    }

    @Override
    public boolean existsByEmail(String email) {
        return findByEmail(email).isPresent();
    }

    @Override
    public long countByVerified(boolean verified) {
        try {
            Query query = getCollection()
                    .whereEqualTo("verified", verified)
                    .whereEqualTo("auditFields.isActive", true);
            return executeQuery(query).size();
        } catch (Exception e) {
            throw new RuntimeException("Failed to count Email OTP methods by verified status", e);
        }
    }

    @Override
    public Optional<EmailOtpAuthenticationMethodEntity> findByUserIdAndEmail(String userId, String email) {
        try {
            Query query = getCollection()
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("email", email)
                    .whereEqualTo("auditFields.isActive", true)
                    .limit(1);
            List<EmailOtpAuthenticationMethodEntity> results = executeQuery(query);
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (Exception e) {
            throw new RuntimeException("Failed to find Email OTP method by user ID and email", e);
        }
    }

    // ===============================
    // Helper Methods
    // ===============================

    protected CollectionReference getCollection() {
        return firestore.collection("email_otp_authentication_methods");
    }

    private List<EmailOtpAuthenticationMethodEntity> executeQuery(Query query) {
        try {
            ApiFuture<QuerySnapshot> future = query.get();
            QuerySnapshot querySnapshot = future.get();

            return querySnapshot.getDocuments().stream()
                    .map(doc -> {
                        EmailOtpAuthenticationMethodEntity entity = doc.toObject(EmailOtpAuthenticationMethodEntity.class);
                        entity.setId(doc.getId());
                        return entity;
                    })
                    .collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to execute Firestore query", e);
        }
    }
}