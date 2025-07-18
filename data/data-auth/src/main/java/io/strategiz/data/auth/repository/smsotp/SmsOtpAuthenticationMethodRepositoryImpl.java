package io.strategiz.data.auth.repository.smsotp;

import io.strategiz.data.auth.entity.smsotp.SmsOtpAuthenticationMethodEntity;
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
 * Firestore implementation of SmsOtpAuthenticationMethodRepository
 * Provides CRUD operations for SMS OTP authentication methods stored in Firestore
 */
@Repository
public class SmsOtpAuthenticationMethodRepositoryImpl implements SmsOtpAuthenticationMethodRepository {

    private static final Logger log = LoggerFactory.getLogger(SmsOtpAuthenticationMethodRepositoryImpl.class);
    
    private final BaseRepository<SmsOtpAuthenticationMethodEntity> baseRepository;
    private final Firestore firestore;

    @Autowired
    public SmsOtpAuthenticationMethodRepositoryImpl(Firestore firestore) {
        this.firestore = firestore;
        this.baseRepository = new BaseRepository<SmsOtpAuthenticationMethodEntity>(firestore, SmsOtpAuthenticationMethodEntity.class) {};
    }

    // ===============================
    // CrudRepository Implementation
    // ===============================

    @Override
    public <S extends SmsOtpAuthenticationMethodEntity> S save(S entity) {
        String userId = entity.getUserId() != null ? entity.getUserId() : "system";
        return (S) baseRepository.save(entity, userId);
    }

    @Override
    public <S extends SmsOtpAuthenticationMethodEntity> Iterable<S> saveAll(Iterable<S> entities) {
        return StreamSupport.stream(entities.spliterator(), false)
                .map(this::save)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<SmsOtpAuthenticationMethodEntity> findById(String id) {
        return baseRepository.findById(id);
    }

    @Override
    public boolean existsById(String id) {
        return findById(id).isPresent();
    }

    @Override
    public Iterable<SmsOtpAuthenticationMethodEntity> findAll() {
        return baseRepository.findAll();
    }

    @Override
    public Iterable<SmsOtpAuthenticationMethodEntity> findAllById(Iterable<String> ids) {
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
    public void delete(SmsOtpAuthenticationMethodEntity entity) {
        deleteById(entity.getId());
    }

    @Override
    public void deleteAllById(Iterable<? extends String> ids) {
        ids.forEach(this::deleteById);
    }

    @Override
    public void deleteAll(Iterable<? extends SmsOtpAuthenticationMethodEntity> entities) {
        entities.forEach(this::delete);
    }

    @Override
    public void deleteAll() {
        List<SmsOtpAuthenticationMethodEntity> all = baseRepository.findAll();
        all.forEach(this::delete);
    }

    // ===============================
    // Custom Query Methods
    // ===============================

    @Override
    public Optional<SmsOtpAuthenticationMethodEntity> findByPhoneNumber(String phoneNumber) {
        try {
            Query query = getCollection()
                    .whereEqualTo("phoneNumber", phoneNumber)
                    .whereEqualTo("auditFields.isActive", true)
                    .limit(1);
            List<SmsOtpAuthenticationMethodEntity> results = executeQuery(query);
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (Exception e) {
            throw new RuntimeException("Failed to find SMS OTP method by phone number", e);
        }
    }

    @Override
    public List<SmsOtpAuthenticationMethodEntity> findByUserIdAndVerified(String userId, boolean verified) {
        try {
            Query query = getCollection()
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("verified", verified)
                    .whereEqualTo("auditFields.isActive", true);
            return executeQuery(query);
        } catch (Exception e) {
            throw new RuntimeException("Failed to find SMS OTP methods by user ID and verified status", e);
        }
    }

    @Override
    public List<SmsOtpAuthenticationMethodEntity> findByUserIdAndEnabled(String userId, boolean enabled) {
        try {
            Query query = getCollection()
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("enabled", enabled)
                    .whereEqualTo("auditFields.isActive", true);
            return executeQuery(query);
        } catch (Exception e) {
            throw new RuntimeException("Failed to find SMS OTP methods by user ID and enabled status", e);
        }
    }

    @Override
    public List<SmsOtpAuthenticationMethodEntity> findByCountryCode(String countryCode) {
        try {
            Query query = getCollection()
                    .whereEqualTo("countryCode", countryCode)
                    .whereEqualTo("auditFields.isActive", true);
            return executeQuery(query);
        } catch (Exception e) {
            throw new RuntimeException("Failed to find SMS OTP methods by country code", e);
        }
    }

    @Override
    public List<SmsOtpAuthenticationMethodEntity> findByLastOtpSentAtAfter(Instant timestamp) {
        try {
            Query query = getCollection()
                    .whereGreaterThan("lastOtpSentAt", timestamp)
                    .whereEqualTo("auditFields.isActive", true);
            return executeQuery(query);
        } catch (Exception e) {
            throw new RuntimeException("Failed to find SMS OTP methods by last OTP sent time", e);
        }
    }

    @Override
    public List<SmsOtpAuthenticationMethodEntity> findByDailySmsCountGreaterThan(int count) {
        try {
            Query query = getCollection()
                    .whereGreaterThan("dailySmsCount", count)
                    .whereEqualTo("auditFields.isActive", true);
            return executeQuery(query);
        } catch (Exception e) {
            throw new RuntimeException("Failed to find SMS OTP methods by daily SMS count", e);
        }
    }

    @Override
    public boolean existsByPhoneNumberAndVerified(String phoneNumber, boolean verified) {
        try {
            Query query = getCollection()
                    .whereEqualTo("phoneNumber", phoneNumber)
                    .whereEqualTo("verified", verified)
                    .whereEqualTo("auditFields.isActive", true)
                    .limit(1);
            return !executeQuery(query).isEmpty();
        } catch (Exception e) {
            throw new RuntimeException("Failed to check if phone number exists with verified status", e);
        }
    }

    @Override
    public boolean existsByPhoneNumber(String phoneNumber) {
        return findByPhoneNumber(phoneNumber).isPresent();
    }

    @Override
    public long countByVerified(boolean verified) {
        try {
            Query query = getCollection()
                    .whereEqualTo("verified", verified)
                    .whereEqualTo("auditFields.isActive", true);
            return executeQuery(query).size();
        } catch (Exception e) {
            throw new RuntimeException("Failed to count SMS OTP methods by verified status", e);
        }
    }

    @Override
    public long countByCountryCode(String countryCode) {
        return findByCountryCode(countryCode).size();
    }

    @Override
    public Optional<SmsOtpAuthenticationMethodEntity> findByUserIdAndPhoneNumber(String userId, String phoneNumber) {
        try {
            Query query = getCollection()
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("phoneNumber", phoneNumber)
                    .whereEqualTo("auditFields.isActive", true)
                    .limit(1);
            List<SmsOtpAuthenticationMethodEntity> results = executeQuery(query);
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (Exception e) {
            throw new RuntimeException("Failed to find SMS OTP method by user ID and phone number", e);
        }
    }

    // ===============================
    // Helper Methods
    // ===============================

    protected CollectionReference getCollection() {
        return firestore.collection("sms_otp_authentication_methods");
    }

    private List<SmsOtpAuthenticationMethodEntity> executeQuery(Query query) {
        try {
            ApiFuture<QuerySnapshot> future = query.get();
            QuerySnapshot querySnapshot = future.get();

            return querySnapshot.getDocuments().stream()
                    .map(doc -> {
                        SmsOtpAuthenticationMethodEntity entity = doc.toObject(SmsOtpAuthenticationMethodEntity.class);
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