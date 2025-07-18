package io.strategiz.data.auth.repository.smsotp;

import io.strategiz.data.auth.entity.smsotp.SmsOtpSessionEntity;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation of SmsOtpSessionRepository for development
 * TODO: Replace with Firestore implementation
 */
@Repository
public class SmsOtpSessionRepositoryImpl implements SmsOtpSessionRepository {
    
    private final ConcurrentHashMap<String, SmsOtpSessionEntity> storage = new ConcurrentHashMap<>();
    
    @Override
    public <S extends SmsOtpSessionEntity> S save(S entity) {
        if (entity.getSessionId() == null) {
            entity.setSessionId("sms_session_" + System.currentTimeMillis());
        }
        storage.put(entity.getSessionId(), entity);
        return entity;
    }
    
    @Override
    public <S extends SmsOtpSessionEntity> Iterable<S> saveAll(Iterable<S> entities) {
        List<S> saved = new ArrayList<>();
        for (S entity : entities) {
            saved.add(save(entity));
        }
        return saved;
    }
    
    @Override
    public Optional<SmsOtpSessionEntity> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }
    
    @Override
    public boolean existsById(String id) {
        return storage.containsKey(id);
    }
    
    @Override
    public Iterable<SmsOtpSessionEntity> findAll() {
        return new ArrayList<>(storage.values());
    }
    
    @Override
    public Iterable<SmsOtpSessionEntity> findAllById(Iterable<String> ids) {
        List<SmsOtpSessionEntity> result = new ArrayList<>();
        for (String id : ids) {
            storage.computeIfPresent(id, (k, v) -> {
                result.add(v);
                return v;
            });
        }
        return result;
    }
    
    @Override
    public long count() {
        return storage.size();
    }
    
    @Override
    public void deleteById(String id) {
        storage.remove(id);
    }
    
    @Override
    public void delete(SmsOtpSessionEntity entity) {
        if (entity != null && entity.getSessionId() != null) {
            storage.remove(entity.getSessionId());
        }
    }
    
    @Override
    public void deleteAllById(Iterable<? extends String> ids) {
        for (String id : ids) {
            storage.remove(id);
        }
    }
    
    @Override
    public void deleteAll(Iterable<? extends SmsOtpSessionEntity> entities) {
        for (SmsOtpSessionEntity entity : entities) {
            delete(entity);
        }
    }
    
    @Override
    public void deleteAll() {
        storage.clear();
    }
    
    // Custom query methods
    
    public Optional<SmsOtpSessionEntity> findBySessionId(String sessionId) {
        return Optional.ofNullable(storage.get(sessionId));
    }
    
    @Override
    public Optional<SmsOtpSessionEntity> findByPhoneNumberAndVerifiedFalse(String phoneNumber) {
        return storage.values().stream()
                .filter(session -> phoneNumber.equals(session.getPhoneNumber()) && !session.isVerified())
                .findFirst();
    }
    
    @Override
    public Optional<SmsOtpSessionEntity> findByPhoneNumberAndOtpCode(String phoneNumber, String otpCode) {
        return storage.values().stream()
                .filter(session -> phoneNumber.equals(session.getPhoneNumber()) && otpCode.equals(session.getOtpCode()))
                .findFirst();
    }
    
    @Override
    public List<SmsOtpSessionEntity> findByUserId(String userId) {
        return storage.values().stream()
                .filter(session -> userId.equals(session.getUserId()))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<SmsOtpSessionEntity> findByCreatedAtAfter(Instant timestamp) {
        return storage.values().stream()
                .filter(session -> session.getCreatedAt() != null && session.getCreatedAt().isAfter(timestamp))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<SmsOtpSessionEntity> findByIpAddress(String ipAddress) {
        return storage.values().stream()
                .filter(session -> ipAddress.equals(session.getIpAddress()))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<SmsOtpSessionEntity> findByIpAddressAndCreatedAtAfter(String ipAddress, Instant timestamp) {
        return storage.values().stream()
                .filter(session -> ipAddress.equals(session.getIpAddress()) && 
                        session.getCreatedAt() != null && session.getCreatedAt().isAfter(timestamp))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<SmsOtpSessionEntity> findByVerifiedTrue() {
        return storage.values().stream()
                .filter(session -> Boolean.TRUE.equals(session.isVerified()))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<SmsOtpSessionEntity> findByExpiresAtBefore(Instant expiredBefore) {
        return storage.values().stream()
                .filter(session -> session.getExpiresAt() != null && session.getExpiresAt().isBefore(expiredBefore))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<SmsOtpSessionEntity> findByCountryCode(String countryCode) {
        return storage.values().stream()
                .filter(session -> countryCode.equals(session.getCountryCode()))
                .collect(Collectors.toList());
    }
    
    @Override
    public boolean existsByPhoneNumberAndCreatedAtAfter(String phoneNumber, Instant timestamp) {
        return storage.values().stream()
                .anyMatch(session -> phoneNumber.equals(session.getPhoneNumber()) && 
                        session.getCreatedAt() != null && session.getCreatedAt().isAfter(timestamp));
    }
    
    @Override
    public boolean existsByIpAddressAndCreatedAtAfter(String ipAddress, Instant timestamp) {
        return storage.values().stream()
                .anyMatch(session -> ipAddress.equals(session.getIpAddress()) && 
                        session.getCreatedAt() != null && session.getCreatedAt().isAfter(timestamp));
    }
    
    @Override
    public long countByVerified(boolean verified) {
        return storage.values().stream()
                .filter(session -> verified == session.isVerified())
                .count();
    }
    
    @Override
    public long countByCreatedAtAfter(Instant timestamp) {
        return storage.values().stream()
                .filter(session -> session.getCreatedAt() != null && session.getCreatedAt().isAfter(timestamp))
                .count();
    }
    
    @Override
    public long countByCountryCode(String countryCode) {
        return storage.values().stream()
                .filter(session -> countryCode.equals(session.getCountryCode()))
                .count();
    }
    
    @Override
    public void deleteByVerifiedTrueAndCreatedAtBefore(Instant timestamp) {
        List<String> toDelete = storage.values().stream()
                .filter(session -> session.isVerified() && 
                        session.getCreatedAt() != null && session.getCreatedAt().isBefore(timestamp))
                .map(SmsOtpSessionEntity::getSessionId)
                .collect(Collectors.toList());
        toDelete.forEach(storage::remove);
    }
    
    @Override
    public void deleteByExpiresAtBefore(Instant expiredBefore) {
        List<String> toDelete = storage.values().stream()
                .filter(session -> session.getExpiresAt() != null && session.getExpiresAt().isBefore(expiredBefore))
                .map(SmsOtpSessionEntity::getSessionId)
                .collect(Collectors.toList());
        toDelete.forEach(storage::remove);
    }
    
    @Override
    public List<SmsOtpSessionEntity> findByPhoneNumberAndCreatedAtBetween(String phoneNumber, Instant startTime, Instant endTime) {
        return storage.values().stream()
                .filter(session -> phoneNumber.equals(session.getPhoneNumber()) && 
                        session.getCreatedAt() != null && 
                        session.getCreatedAt().isAfter(startTime) && 
                        session.getCreatedAt().isBefore(endTime))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<SmsOtpSessionEntity> findByPhoneNumberAndCreatedAtAfter(String phoneNumber, Instant timestamp) {
        return storage.values().stream()
                .filter(session -> phoneNumber.equals(session.getPhoneNumber()) && 
                        session.getCreatedAt() != null && session.getCreatedAt().isAfter(timestamp))
                .collect(Collectors.toList());
    }
}