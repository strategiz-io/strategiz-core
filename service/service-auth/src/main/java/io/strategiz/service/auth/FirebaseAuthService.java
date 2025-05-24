package io.strategiz.service.auth;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for Firebase authentication operations
 */
@Service
public class FirebaseAuthService {
    
    private static final Logger log = LoggerFactory.getLogger(FirebaseAuthService.class);
    
    /**
     * Verify a Firebase ID token
     *
     * @param idToken Firebase ID token
     * @return FirebaseToken if valid, null otherwise
     */
    public FirebaseToken verifyIdToken(String idToken) {
        try {
            return FirebaseAuth.getInstance().verifyIdToken(idToken);
        } catch (FirebaseAuthException e) {
            log.error("Error verifying Firebase ID token", e);
            return null;
        }
    }
    
    /**
     * Get user by ID
     *
     * @param uid User ID
     * @return UserRecord if found, null otherwise
     */
    public UserRecord getUserById(String uid) {
        try {
            return FirebaseAuth.getInstance().getUser(uid);
        } catch (FirebaseAuthException e) {
            log.error("Error getting Firebase user by ID: {}", uid, e);
            return null;
        }
    }
    
    /**
     * Get user by email
     *
     * @param email User email
     * @return UserRecord if found, null otherwise
     */
    public UserRecord getUserByEmail(String email) {
        try {
            return FirebaseAuth.getInstance().getUserByEmail(email);
        } catch (FirebaseAuthException e) {
            log.error("Error getting Firebase user by email: {}", email, e);
            return null;
        }
    }
    
    /**
     * Create a new user
     *
     * @param email User email
     * @param password User password
     * @param displayName User display name
     * @return UserRecord if created, null otherwise
     */
    public UserRecord createUser(String email, String password, String displayName) {
        try {
            UserRecord.CreateRequest request = new UserRecord.CreateRequest()
                    .setEmail(email)
                    .setPassword(password)
                    .setDisplayName(displayName)
                    .setEmailVerified(false);
            
            return FirebaseAuth.getInstance().createUser(request);
        } catch (FirebaseAuthException e) {
            log.error("Error creating Firebase user: {}", email, e);
            return null;
        }
    }
    
    /**
     * Update a user
     *
     * @param uid User ID
     * @param updateFields Map of fields to update
     * @return UserRecord if updated, null otherwise
     */
    public UserRecord updateUser(String uid, Map<String, Object> updateFields) {
        try {
            UserRecord.UpdateRequest request = new UserRecord.UpdateRequest(uid);
            
            if (updateFields.containsKey("email")) {
                request.setEmail((String) updateFields.get("email"));
            }
            
            if (updateFields.containsKey("password")) {
                request.setPassword((String) updateFields.get("password"));
            }
            
            if (updateFields.containsKey("displayName")) {
                request.setDisplayName((String) updateFields.get("displayName"));
            }
            
            if (updateFields.containsKey("photoUrl")) {
                request.setPhotoUrl((String) updateFields.get("photoUrl"));
            }
            
            if (updateFields.containsKey("emailVerified")) {
                request.setEmailVerified((Boolean) updateFields.get("emailVerified"));
            }
            
            if (updateFields.containsKey("disabled")) {
                request.setDisabled((Boolean) updateFields.get("disabled"));
            }
            
            return FirebaseAuth.getInstance().updateUser(request);
        } catch (FirebaseAuthException e) {
            log.error("Error updating Firebase user: {}", uid, e);
            return null;
        }
    }
    
    /**
     * Delete a user
     *
     * @param uid User ID
     * @return true if deleted, false otherwise
     */
    public boolean deleteUser(String uid) {
        try {
            FirebaseAuth.getInstance().deleteUser(uid);
            return true;
        } catch (FirebaseAuthException e) {
            log.error("Error deleting Firebase user: {}", uid, e);
            return false;
        }
    }
    
    /**
     * Extract user data from a Firebase token
     *
     * @param token Firebase token
     * @return Map containing user data
     */
    public Map<String, Object> getUserDataFromToken(FirebaseToken token) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("userId", token.getUid());
        userData.put("email", token.getEmail());
        userData.put("name", token.getName());
        userData.put("picture", token.getPicture());
        userData.put("emailVerified", token.isEmailVerified());
        userData.put("issuer", token.getIssuer());
        userData.put("authTime", token.getAuthTime());
        return userData;
    }
}
