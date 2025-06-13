package io.strategiz.data.user.repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import io.strategiz.data.user.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Repository for accessing and manipulating user data according to the new schema design.
 * Implements operations for user profiles, authentication methods, and connected providers.
 */
@Repository
public class UserRepository {

    private static final String USERS_COLLECTION = "users";
    private static final String AUTH_METHODS_COLLECTION = "authentication_methods";
    private static final String API_CREDENTIALS_COLLECTION = "api_credentials";
    private static final String DEVICES_COLLECTION = "devices";
    private static final String PREFERENCES_COLLECTION = "preferences";
    private static final String WATCHLIST_COLLECTION = "market_watchlist";
    private static final String PROVIDERS_COLLECTION = "providers"; // For the Provider subcollection
    private static final String CREDENTIALS_COLLECTION = "credentials"; // Subcollection under a provider
    private static final String DEFAULT_CREDENTIALS_ID = "default"; // Default ID for credentials document
    
    private final Firestore firestore;

    @Autowired
    public UserRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    /**
     * Creates a new user with the given profile information.
     *
     * @param user The user object to create
     * @return The created user with ID
     */
    public User createUser(User user) {
        try {
            DocumentReference docRef = firestore.collection(USERS_COLLECTION).document(user.getId());
            
            // Set timestamps to server timestamp
            Map<String, Object> userData = new HashMap<>();
            userData.put("profile", user.getProfile());
            userData.put("connectedProviders", user.getConnectedProviders());
            userData.put("createdBy", user.getCreatedBy());
            userData.put("createdAt", FieldValue.serverTimestamp());
            userData.put("modifiedBy", user.getModifiedBy());
            userData.put("modifiedAt", FieldValue.serverTimestamp());
            userData.put("version", user.getVersion());
            userData.put("isActive", user.getIsActive());
            
            ApiFuture<WriteResult> future = docRef.set(userData);
            future.get(); // Wait for write to complete
            
            return user;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error creating user: " + e.getMessage(), e);
        }
    }
    
    /**
     * Retrieves a user by ID.
     *
     * @param userId The user ID
     * @return An Optional containing the user if found
     */
    public Optional<User> getUserById(String userId) {
        try {
            DocumentReference docRef = firestore.collection(USERS_COLLECTION).document(userId);
            ApiFuture<DocumentSnapshot> future = docRef.get();
            DocumentSnapshot document = future.get();
            
            if (document.exists()) {
                User user = new User();
                user.setId(document.getId());
                
                // Get profile data
                Map<String, Object> profileData = (Map<String, Object>) document.get("profile");
                if (profileData != null) {
                    UserProfile profile = new UserProfile();
                    profile.setName((String) profileData.get("name"));
                    profile.setEmail((String) profileData.get("email"));
                    profile.setPhotoURL((String) profileData.get("photoURL"));
                    profile.setVerifiedEmail((Boolean) profileData.get("verifiedEmail"));
                    profile.setSubscriptionTier((String) profileData.get("subscriptionTier"));
                    profile.setTradingMode((String) profileData.get("tradingMode"));
                    profile.setIsActive((Boolean) profileData.get("isActive"));
                    user.setProfile(profile);
                }
                
                // Get connected providers
                List<Map<String, Object>> providersData = (List<Map<String, Object>>) document.get("connectedProviders");
                if (providersData != null) {
                    List<ConnectedProvider> providers = new ArrayList<>();
                    for (Map<String, Object> providerData : providersData) {
                        ConnectedProvider provider = new ConnectedProvider();
                        provider.setId((String) providerData.get("id"));
                        provider.setProviderId((String) providerData.get("providerId"));
                        provider.setProviderName((String) providerData.get("providerName"));
                        provider.setAccountId((String) providerData.get("accountId"));
                        provider.setAccountType((String) providerData.get("accountType"));
                        provider.setAccountName((String) providerData.get("accountName"));
                        provider.setStatus((String) providerData.get("status"));
                        provider.setLastSyncAt(((Timestamp) providerData.get("lastSyncAt")).toDate());
                        provider.setCreatedBy((String) providerData.get("createdBy"));
                        provider.setCreatedAt(((Timestamp) providerData.get("createdAt")).toDate());
                        provider.setModifiedBy((String) providerData.get("modifiedBy"));
                        provider.setModifiedAt(((Timestamp) providerData.get("modifiedAt")).toDate());
                        provider.setVersion(((Long) providerData.get("version")).intValue());
                        provider.setIsActive((Boolean) providerData.get("isActive"));
                        providers.add(provider);
                    }
                    user.setConnectedProviders(providers);
                }
                
                // Set audit fields
                user.setCreatedBy((String) document.get("createdBy"));
                user.setCreatedAt(((Timestamp) document.get("createdAt")).toDate());
                user.setModifiedBy((String) document.get("modifiedBy"));
                user.setModifiedAt(((Timestamp) document.get("modifiedAt")).toDate());
                user.setVersion(((Long) document.get("version")).intValue());
                user.setIsActive((Boolean) document.get("isActive"));
                
                return Optional.of(user);
            } else {
                return Optional.empty();
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error retrieving user: " + e.getMessage(), e);
        }
    }
    
    /**
     * Updates a user's profile information.
     *
     * @param userId The user ID
     * @param profile The updated profile
     * @param modifiedBy ID of the entity making the modification
     * @return true if successful
     */
    public boolean updateUserProfile(String userId, UserProfile profile, String modifiedBy) {
        try {
            DocumentReference userRef = firestore.collection(USERS_COLLECTION).document(userId);
            
            // First get the current document to check version
            DocumentSnapshot doc = userRef.get().get();
            if (!doc.exists()) {
                return false;
            }
            
            Long version = ((Long) doc.get("version"));
            int newVersion = version != null ? version.intValue() + 1 : 1;
            
            Map<String, Object> updates = new HashMap<>();
            updates.put("profile", profile);
            updates.put("modifiedBy", modifiedBy);
            updates.put("modifiedAt", FieldValue.serverTimestamp());
            updates.put("version", newVersion);
            
            ApiFuture<WriteResult> future = userRef.update(updates);
            future.get();
            
            return true;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error updating user profile: " + e.getMessage(), e);
        }
    }
    
    /**
     * Adds a new authentication method to a user's account.
     *
     * @param userId The user ID
     * @param authMethod The authentication method to add
     * @return The ID of the created authentication method
     */
    public String addAuthenticationMethod(String userId, AuthenticationMethod authMethod) {
        try {
            CollectionReference authMethodsRef = firestore.collection(USERS_COLLECTION)
                    .document(userId)
                    .collection(AUTH_METHODS_COLLECTION);
            
            // Set timestamps to server timestamp
            Map<String, Object> authMethodData = new HashMap<>();
            authMethodData.put("type", authMethod.getType());
            authMethodData.put("name", authMethod.getName());
            authMethodData.put("lastVerifiedAt", FieldValue.serverTimestamp());
            authMethodData.put("createdBy", authMethod.getCreatedBy());
            authMethodData.put("createdAt", FieldValue.serverTimestamp());
            authMethodData.put("modifiedBy", authMethod.getModifiedBy());
            authMethodData.put("modifiedAt", FieldValue.serverTimestamp());
            authMethodData.put("version", authMethod.getVersion());
            authMethodData.put("isActive", authMethod.isActive());
            
            // Add type-specific fields
            if (authMethod instanceof TotpAuthenticationMethod) {
                TotpAuthenticationMethod totp = (TotpAuthenticationMethod) authMethod;
                authMethodData.put("secret", totp.getSecret());
            } else if (authMethod instanceof PasskeyAuthenticationMethod) {
                PasskeyAuthenticationMethod passkey = (PasskeyAuthenticationMethod) authMethod;
                authMethodData.put("credentialId", passkey.getCredentialId());
                authMethodData.put("publicKey", passkey.getPublicKey());
                authMethodData.put("counter", passkey.getCounter());
            } else if (authMethod instanceof SmsOtpAuthenticationMethod) {
                SmsOtpAuthenticationMethod sms = (SmsOtpAuthenticationMethod) authMethod;
                authMethodData.put("phoneNumber", sms.getPhoneNumber());
                authMethodData.put("verified", sms.getVerified());
            } else if (authMethod instanceof OAuthAuthenticationMethod) {
                OAuthAuthenticationMethod oauth = (OAuthAuthenticationMethod) authMethod;
                authMethodData.put("provider", oauth.getProvider());
                authMethodData.put("uid", oauth.getUid());
                authMethodData.put("email", oauth.getEmail());
            }
            
            // Add the document and get the generated ID
            DocumentReference docRef = authMethodsRef.add(authMethodData).get();
            String authMethodId = docRef.getId();
            
            // Update the parent user document's modified timestamp
            Map<String, Object> userUpdates = new HashMap<>();
            userUpdates.put("modifiedBy", authMethod.getCreatedBy());
            userUpdates.put("modifiedAt", FieldValue.serverTimestamp());
            
            DocumentReference userRef = firestore.collection(USERS_COLLECTION).document(userId);
            userRef.update(userUpdates).get();
            
            return authMethodId;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error adding authentication method: " + e.getMessage(), e);
        }
    }
    
    /**
     * Gets all authentication methods for a user.
     *
     * @param userId The user ID
     * @return List of authentication methods
     */
    public List<AuthenticationMethod> getAuthenticationMethods(String userId) {
        try {
            CollectionReference authMethodsRef = firestore.collection(USERS_COLLECTION)
                    .document(userId)
                    .collection(AUTH_METHODS_COLLECTION);
            
            ApiFuture<QuerySnapshot> future = authMethodsRef.get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();
            
            List<AuthenticationMethod> methods = new ArrayList<>();
            for (QueryDocumentSnapshot doc : documents) {
                String type = doc.getString("type");
                AuthenticationMethod method;
                
                switch (type) {
                    case "TOTP":
                        TotpAuthenticationMethod totp = new TotpAuthenticationMethod();
                        totp.setSecret(doc.getString("secret"));
                        method = totp;
                        break;
                    case "PASSKEY":
                        PasskeyAuthenticationMethod passkey = new PasskeyAuthenticationMethod();
                        passkey.setCredentialId(doc.getString("credentialId"));
                        passkey.setPublicKey(doc.getString("publicKey"));
                        passkey.setCounter(doc.getLong("counter"));
                        method = passkey;
                        break;
                    case "SMS_OTP":
                        SmsOtpAuthenticationMethod sms = new SmsOtpAuthenticationMethod();
                        sms.setPhoneNumber(doc.getString("phoneNumber"));
                        sms.setVerified(doc.getBoolean("verified"));
                        method = sms;
                        break;
                    case "OAUTH_GOOGLE":
                    case "OAUTH_FACEBOOK":
                        OAuthAuthenticationMethod oauth = new OAuthAuthenticationMethod();
                        oauth.setProvider(doc.getString("provider"));
                        oauth.setUid(doc.getString("uid"));
                        oauth.setEmail(doc.getString("email"));
                        method = oauth;
                        break;
                    default:
                        method = new AuthenticationMethod();
                }
                
                // Set common fields
                method.setId(doc.getId());
                method.setType(type);
                method.setName(doc.getString("name"));
                method.setLastVerifiedAt(doc.getTimestamp("lastVerifiedAt") != null ? 
                        doc.getTimestamp("lastVerifiedAt").toDate() : null);
                method.setCreatedBy(doc.getString("createdBy"));
                method.setCreatedAt(doc.getTimestamp("createdAt").toDate());
                method.setModifiedBy(doc.getString("modifiedBy"));
                method.setModifiedAt(doc.getTimestamp("modifiedAt").toDate());
                method.setVersion(doc.getLong("version").intValue());
                method.setIsActive(doc.getBoolean("isActive"));
                
                methods.add(method);
            }
            
            return methods;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error getting authentication methods: " + e.getMessage(), e);
        }
    }

    /**
     * Adds a connected provider to a user's account.
     *
     * @param userId The user ID
     * @param provider The provider to connect
     * @return true if successful
     */
    public boolean addConnectedProvider(String userId, ConnectedProvider provider) {
        try {
            DocumentReference userRef = firestore.collection(USERS_COLLECTION).document(userId);
            
            // First get the current document
            DocumentSnapshot doc = userRef.get().get();
            if (!doc.exists()) {
                return false;
            }
            
            // Get current version and increment
            Long version = ((Long) doc.get("version"));
            int newVersion = version != null ? version.intValue() + 1 : 1;
            
            // Get the current connectedProviders array
            List<Map<String, Object>> currentProviders = 
                    (List<Map<String, Object>>) doc.get("connectedProviders");
            List<Map<String, Object>> updatedProviders = new ArrayList<>();
            
            if (currentProviders != null) {
                updatedProviders.addAll(currentProviders);
            }
            
            // Convert the provider to a Map
            Map<String, Object> providerMap = new HashMap<>();
            providerMap.put("id", provider.getId());
            providerMap.put("providerId", provider.getProviderId());
            providerMap.put("providerName", provider.getProviderName());
            providerMap.put("accountId", provider.getAccountId());
            providerMap.put("accountType", provider.getAccountType());
            providerMap.put("accountName", provider.getAccountName());
            providerMap.put("status", provider.getStatus());
            providerMap.put("lastSyncAt", FieldValue.serverTimestamp());
            providerMap.put("createdBy", provider.getCreatedBy());
            providerMap.put("createdAt", FieldValue.serverTimestamp());
            providerMap.put("modifiedBy", provider.getModifiedBy());
            providerMap.put("modifiedAt", FieldValue.serverTimestamp());
            providerMap.put("version", provider.getVersion());
            providerMap.put("isActive", provider.getIsActive());
            
            updatedProviders.add(providerMap);
            
            // Update the user document
            Map<String, Object> updates = new HashMap<>();
            updates.put("connectedProviders", updatedProviders);
            updates.put("modifiedBy", provider.getCreatedBy());
            updates.put("modifiedAt", FieldValue.serverTimestamp());
            updates.put("version", newVersion);
            
            ApiFuture<WriteResult> future = userRef.update(updates);
            future.get();
            
            return true;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error adding connected provider: " + e.getMessage(), e);
        }
    }
    
    /**
     * Adds a device to a user's devices subcollection.
     *
     * @param userId The user ID
     * @param deviceName Device name
     * @param agentId Unique agent ID for the device
     * @param platform Platform information
     * @param createdBy ID of the entity creating the device
     * @return The ID of the created device
     */
    public String addDevice(String userId, String deviceName, String agentId, 
                           Map<String, Object> platform, String createdBy) {
        try {
            CollectionReference devicesRef = firestore.collection(USERS_COLLECTION)
                    .document(userId)
                    .collection(DEVICES_COLLECTION);
            
            Map<String, Object> deviceData = new HashMap<>();
            deviceData.put("deviceName", deviceName);
            deviceData.put("agentId", agentId);
            deviceData.put("platform", platform);
            deviceData.put("firstSeen", FieldValue.serverTimestamp());
            deviceData.put("lastSeen", FieldValue.serverTimestamp());
            deviceData.put("createdBy", createdBy);
            deviceData.put("createdAt", FieldValue.serverTimestamp());
            deviceData.put("modifiedBy", createdBy);
            deviceData.put("modifiedAt", FieldValue.serverTimestamp());
            deviceData.put("version", 1);
            deviceData.put("isActive", true);
            deviceData.put("authMethods", new ArrayList<>());
            
            DocumentReference docRef = devicesRef.add(deviceData).get();
            
            // Update the parent user document
            DocumentReference userRef = firestore.collection(USERS_COLLECTION).document(userId);
            Map<String, Object> updates = new HashMap<>();
            updates.put("modifiedBy", createdBy);
            updates.put("modifiedAt", FieldValue.serverTimestamp());
            userRef.update(updates).get();
            
            return docRef.getId();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error adding device: " + e.getMessage(), e);
        }
    }

    /**
     * Updates the account mode for a user.
     *
     * @param userId The user ID
     * @param accountMode The account mode ("PAPER" or "LIVE")
     * @return true if successful
     */
    public boolean updateAccountMode(String userId, String accountMode) {
        try {
            DocumentReference docRef = firestore.collection(USERS_COLLECTION).document(userId);
            ApiFuture<WriteResult> future = docRef.update("accountMode", accountMode);
            future.get();
            return true;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error updating account mode: " + e.getMessage(), e);
        }
    }

    /**
     * Gets provider configuration for a specific user and provider.
     *
     * @param userId The user ID
     * @param providerId The provider ID
     * @return An Optional containing the provider if found
     */
    public Optional<Provider> getProvider(String userId, String providerId) {
        try {
            DocumentReference docRef = firestore.collection(USERS_COLLECTION)
                    .document(userId)
                    .collection(PROVIDERS_COLLECTION)
                    .document(providerId);
                    
            ApiFuture<DocumentSnapshot> future = docRef.get();
            DocumentSnapshot document = future.get();
            
            if (document.exists()) {
                Provider provider = document.toObject(Provider.class);
                provider.setId(document.getId());
                return Optional.of(provider);
            } else {
                return Optional.empty();
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error retrieving provider: " + e.getMessage(), e);
        }
    }

    /**
     * Saves or updates provider configuration for a user.
     *
     * @param userId The user ID
     * @param providerId The provider ID
     * @param provider The provider configuration to save
     * @return true if successful
     */
    public boolean saveProvider(String userId, String providerId, Provider provider) {
        try {
            DocumentReference docRef = firestore.collection(USERS_COLLECTION)
                    .document(userId)
                    .collection(PROVIDERS_COLLECTION)
                    .document(providerId);
                    
            provider.setId(providerId);
            ApiFuture<WriteResult> future = docRef.set(provider, SetOptions.merge());
            future.get();
            return true;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error saving provider: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes provider configuration for a user.
     *
     * @param userId The user ID
     * @param providerId The provider ID
     * @return true if successful
     */
    public boolean deleteProvider(String userId, String providerId) {
        try {
            // First delete any credentials documents under this provider
            deleteCredentials(userId, providerId);
            
            // Then delete the provider document itself
            DocumentReference docRef = firestore.collection(USERS_COLLECTION)
                    .document(userId)
                    .collection(PROVIDERS_COLLECTION)
                    .document(providerId);
                    
            ApiFuture<WriteResult> future = docRef.delete();
            future.get();
            return true;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error deleting provider: " + e.getMessage(), e);
        }
    }
    
    /**
     * Gets API credentials for a specific user and provider.
     *
     * @param userId The user ID
     * @param providerId The provider ID
     * @return An Optional containing the credentials if found
     */
    public Optional<Credentials> getCredentials(String userId, String providerId) {
        try {
            DocumentReference docRef = firestore.collection(USERS_COLLECTION)
                    .document(userId)
                    .collection(PROVIDERS_COLLECTION)
                    .document(providerId)
                    .collection(CREDENTIALS_COLLECTION)
                    .document(DEFAULT_CREDENTIALS_ID);
                    
            ApiFuture<DocumentSnapshot> future = docRef.get();
            DocumentSnapshot document = future.get();
            
            if (document.exists()) {
                Credentials credentials = document.toObject(Credentials.class);
                credentials.setId(document.getId());
                return Optional.of(credentials);
            } else {
                return Optional.empty();
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error retrieving credentials: " + e.getMessage(), e);
        }
    }
    
    /**
     * Saves or updates API credentials for a user's provider.
     *
     * @param userId The user ID
     * @param providerId The provider ID
     * @param credentials The credentials to save
     * @return true if successful
     */
    public boolean saveCredentials(String userId, String providerId, Credentials credentials) {
        try {
            DocumentReference docRef = firestore.collection(USERS_COLLECTION)
                    .document(userId)
                    .collection(PROVIDERS_COLLECTION)
                    .document(providerId)
                    .collection(CREDENTIALS_COLLECTION)
                    .document(DEFAULT_CREDENTIALS_ID);
                    
            credentials.setId(DEFAULT_CREDENTIALS_ID);
            ApiFuture<WriteResult> future = docRef.set(credentials, SetOptions.merge());
            future.get();
            return true;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error saving credentials: " + e.getMessage(), e);
        }
    }
    
    /**
     * Deletes API credentials for a user's provider.
     *
     * @param userId The user ID
     * @param providerId The provider ID
     * @return true if successful
     */
    public boolean deleteCredentials(String userId, String providerId) {
        try {
            DocumentReference docRef = firestore.collection(USERS_COLLECTION)
                    .document(userId)
                    .collection(PROVIDERS_COLLECTION)
                    .document(providerId)
                    .collection(CREDENTIALS_COLLECTION)
                    .document(DEFAULT_CREDENTIALS_ID);
                    
            ApiFuture<WriteResult> future = docRef.delete();
            future.get();
            return true;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error deleting credentials: " + e.getMessage(), e);
        }
    }
}
