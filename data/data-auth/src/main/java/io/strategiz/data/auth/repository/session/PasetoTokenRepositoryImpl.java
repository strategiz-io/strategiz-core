package io.strategiz.data.auth.repository.session;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import io.strategiz.data.auth.model.session.PasetoToken;
import io.strategiz.data.auth.repository.session.PasetoTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
// No need to import Instant as we're using primitive longs for timestamps

/**
 * Firestore implementation of PasetoTokenRepository.
 */
@Repository
public class PasetoTokenRepositoryImpl implements PasetoTokenRepository {
    private static final String TOKENS_COLLECTION = "paseto_tokens";
    
    private final CollectionReference tokensCollection;
    
    @Autowired
    public PasetoTokenRepositoryImpl(Firestore firestore) {
        this.tokensCollection = firestore.collection(TOKENS_COLLECTION);
    }
    
    @Override
    public PasetoToken save(PasetoToken token) {
        try {
            // If no ID is provided, generate one
            if (token.getId() == null || token.getId().isEmpty()) {
                token.setId(UUID.randomUUID().toString());
            }
            
            // Convert to map for Firestore
            Map<String, Object> tokenMap = convertToMap(token);
            
            // Save to Firestore
            tokensCollection.document(token.getId()).set(tokenMap).get();
            
            return token;
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to save token", e);
        }
    }
    
    @Override
    public void delete(PasetoToken token) {
        try {
            tokensCollection.document(token.getId()).delete().get();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to delete token", e);
        }
    }
    
    @Override
    public Optional<PasetoToken> findByTokenValue(String tokenValue) {
        try {
            QuerySnapshot querySnapshot = tokensCollection
                .whereEqualTo("tokenValue", tokenValue)
                .get()
                .get();
            
            if (querySnapshot.isEmpty()) {
                return Optional.empty();
            }
            
            DocumentSnapshot document = querySnapshot.getDocuments().get(0);
            PasetoToken token = convertToToken(document);
            return Optional.of(token);
            
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to find token by value", e);
        }
    }
    
    @Override
    public List<PasetoToken> findAllByUserId(String userId) {
        try {
            QuerySnapshot querySnapshot = tokensCollection
                .whereEqualTo("userId", userId)
                .get()
                .get();
            
            List<PasetoToken> tokens = new ArrayList<>();
            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                tokens.add(convertToToken(document));
            }
            
            return tokens;
            
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to find tokens by user ID", e);
        }
    }
    
    @Override
    public List<PasetoToken> findActiveTokensByUserId(String userId) {
        try {
            QuerySnapshot querySnapshot = tokensCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("revoked", false)
                .get()
                .get();
            
            List<PasetoToken> tokens = new ArrayList<>();
            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                PasetoToken token = convertToToken(document);
                if (token.getExpiresAt() > System.currentTimeMillis() / 1000) {
                    tokens.add(token);
                }
            }
            
            return tokens;
            
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to find active tokens by user ID", e);
        }
    }
    
    @Override
    public int deleteExpiredTokens(long timestamp) {
        try {
            QuerySnapshot querySnapshot = tokensCollection
                .whereLessThan("expiresAt", timestamp)
                .get()
                .get();
            
            int count = querySnapshot.getDocuments().size();
            
            if (count == 0) {
                return 0;
            }
            
            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                document.getReference().delete().get();
            }
            
            return count;
            
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to delete expired tokens", e);
        }
    }
    
    private Map<String, Object> convertToMap(PasetoToken token) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", token.getId());
        map.put("userId", token.getUserId());
        map.put("tokenValue", token.getTokenValue());
        map.put("tokenType", token.getTokenType());
        // Issue #5a143062: PasetoToken no longer has a createdAt field
        map.put("issuedAt", token.getIssuedAt());
        map.put("expiresAt", token.getExpiresAt());
        map.put("revoked", token.isRevoked());
        
        // Optional fields
        if (token.getDeviceId() != null) {
            map.put("deviceId", token.getDeviceId());
        }
        if (token.getIssuedFrom() != null) {
            map.put("issuedFrom", token.getIssuedFrom());
        }
        if (token.getRevokedAt() > 0) {
            map.put("revokedAt", token.getRevokedAt());
        }
        if (token.getRevocationReason() != null) {
            map.put("revocationReason", token.getRevocationReason());
        }
        if (token.getClaims() != null && !token.getClaims().isEmpty()) {
            map.put("claims", token.getClaims());
        }
        return map;
    }
    
    private PasetoToken convertToToken(DocumentSnapshot document) {
        Map<String, Object> data = document.getData();
        if (data == null) {
            return null;
        }
        
        PasetoToken token = new PasetoToken();
        token.setId(document.getId());
        token.setUserId((String) data.get("userId"));
        token.setTokenValue((String) data.get("tokenValue"));
        token.setTokenType((String) data.get("tokenType"));
        
        // PasetoToken no longer has createdAt field
        
        if (data.get("issuedAt") != null) {
            token.setIssuedAt((Long) data.get("issuedAt"));
        }
        
        if (data.get("expiresAt") != null) {
            token.setExpiresAt((Long) data.get("expiresAt"));
        }
        
        token.setRevoked(Boolean.TRUE.equals(data.get("revoked")));
        
        // Optional fields
        if (data.get("deviceId") != null) {
            token.setDeviceId((String) data.get("deviceId"));
        }
        
        if (data.get("issuedFrom") != null) {
            token.setIssuedFrom((String) data.get("issuedFrom"));
        }
        
        if (data.get("revokedAt") != null) {
            token.setRevokedAt((Long) data.get("revokedAt"));
        }
        
        if (data.get("revocationReason") != null) {
            token.setRevocationReason((String) data.get("revocationReason"));
        }
        
        if (data.get("claims") != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> claims = (Map<String, Object>) data.get("claims");
            token.setClaims(claims);
        }
        
        return token;
    }
}
