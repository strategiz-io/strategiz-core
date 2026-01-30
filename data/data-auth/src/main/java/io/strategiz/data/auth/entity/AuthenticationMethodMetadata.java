package io.strategiz.data.auth.entity;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Type-specific metadata schemas and validation for authentication methods
 * Each authentication method type has unique storage requirements
 */
public class AuthenticationMethodMetadata {

    /**
     * Metadata schema for PASSKEY authentication methods
     */
    public static class PasskeyMetadata {
        // Core WebAuthn/FIDO2 fields
        public static final String CREDENTIAL_ID = "credentialId";           // Unique credential identifier
        public static final String PUBLIC_KEY_BASE64 = "publicKeyBase64";    // Public key for verification
        public static final String SIGNATURE_COUNT = "signatureCount";       // Anti-replay counter
        public static final String AAGUID = "aaguid";                       // Authenticator AAGUID
        
        // Device information
        public static final String DEVICE_NAME = "deviceName";              // User-friendly device name
        public static final String USER_AGENT = "userAgent";                // Browser/platform info
        public static final String AUTHENTICATOR_NAME = "authenticatorName"; // Authenticator type
        
        // Security flags
        public static final String TRUSTED = "trusted";                     // Is this a trusted authenticator
        public static final String VERIFIED = "verified";                   // Has been successfully verified
        public static final String BACKUP_ELIGIBLE = "backupEligible";      // Can be backed up
        public static final String BACKUP_STATE = "backupState";            // Current backup state

        // Note: Use BaseEntity.createdDate for registration time
        // Note: Use AuthenticationMethodEntity.lastUsedAt for last used time

        // WebAuthn specific
        public static final String ATTESTATION_TYPE = "attestationType";     // none, basic, self, etc.
        public static final String TRANSPORT = "transport";                 // usb, nfc, ble, internal
        public static final String RESIDENT_KEY = "residentKey";            // Is resident key
    }

    /**
     * Metadata schema for TOTP authentication methods
     */
    public static class TotpMetadata {
        // Core TOTP fields
        public static final String SECRET_KEY = "secretKey";                // Base32 encoded secret
        public static final String ALGORITHM = "algorithm";                 // SHA1, SHA256, SHA512
        public static final String DIGITS = "digits";                       // Usually 6 or 8
        public static final String PERIOD = "period";                       // Time step (usually 30s)
        
        // Backup and recovery
        public static final String BACKUP_CODES = "backupCodes";            // List of one-time backup codes
        public static final String BACKUP_CODES_USED = "backupCodesUsed";   // Used backup codes
        
        // QR Code generation
        public static final String QR_CODE_GENERATED = "qrCodeGenerated";   // Has QR been generated
        public static final String ISSUER = "issuer";                       // App/service name
        public static final String ACCOUNT_NAME = "accountName";            // User identifier
        
        // Verification status
        public static final String VERIFIED = "verified";                   // Successfully set up
        public static final String VERIFICATION_TIME = "verificationTime";  // When verified
        
        // Usage tracking
        public static final String LAST_CODE_USED = "lastCodeUsed";         // Last TOTP code used
        public static final String LAST_USED_TIME = "lastUsedTime";         // Last authentication
        public static final String USAGE_COUNT = "usageCount";              // Number of times used
    }

    /**
     * Metadata schema for SMS_OTP authentication methods
     */
    public static class SmsOtpMetadata {
        // Phone number details
        public static final String PHONE_NUMBER = "phoneNumber";            // Full E.164 format
        public static final String COUNTRY_CODE = "countryCode";            // ISO country code
        public static final String CARRIER = "carrier";                     // Mobile carrier
        
        // Verification status
        public static final String IS_VERIFIED = "isVerified";              // Phone verified
        public static final String VERIFICATION_TIME = "verificationTime";  // When verified
        public static final String VERIFICATION_CODE = "verificationCode";   // Last sent code (temporary)
        
        // SMS delivery
        public static final String LAST_SMS_SENT = "lastSmsSent";           // Last SMS timestamp
        public static final String LAST_OTP_SENT_AT = "lastOtpSentAt";      // Last OTP sent timestamp
        public static final String SMS_DELIVERY_STATUS = "smsDeliveryStatus"; // delivered, failed, pending
        public static final String SMS_PROVIDER = "smsProvider";            // Twilio, Firebase, etc.
        
        // Rate limiting
        public static final String DAILY_SMS_COUNT = "dailySmsCount";       // SMS sent today
        public static final String LAST_SMS_DATE = "lastSmsDate";           // Date of last SMS
        public static final String DAILY_COUNT_RESET_AT = "dailyCountResetAt"; // When daily count was reset
        public static final String RATE_LIMIT_EXCEEDED = "rateLimitExceeded"; // Rate limit hit
        
        // Usage tracking
        public static final String LAST_USED_TIME = "lastUsedTime";         // Last authentication
        public static final String USAGE_COUNT = "usageCount";              // Number of times used
        public static final String FAILED_ATTEMPTS = "failedAttempts";      // Failed verification attempts
    }

    /**
     * Metadata schema for EMAIL_OTP authentication methods
     */
    public static class EmailOtpMetadata {
        // Email details
        public static final String EMAIL_ADDRESS = "emailAddress";          // Email address
        public static final String IS_VERIFIED = "isVerified";              // Email verified
        public static final String VERIFICATION_TIME = "verificationTime";  // When verified
        
        // Email delivery
        public static final String LAST_EMAIL_SENT = "lastEmailSent";       // Last email timestamp
        public static final String EMAIL_DELIVERY_STATUS = "emailDeliveryStatus"; // delivered, failed, pending
        public static final String EMAIL_PROVIDER = "emailProvider";        // SendGrid, SES, etc.
        
        // OTP details
        public static final String VERIFICATION_CODE = "verificationCode";   // Last sent code (temporary)
        public static final String CODE_EXPIRY = "codeExpiry";              // When code expires
        
        // Rate limiting
        public static final String DAILY_EMAIL_COUNT = "dailyEmailCount";   // Emails sent today
        public static final String LAST_EMAIL_DATE = "lastEmailDate";       // Date of last email
        
        // Usage tracking
        public static final String LAST_USED_TIME = "lastUsedTime";         // Last authentication
        public static final String USAGE_COUNT = "usageCount";              // Number of times used
        public static final String FAILED_ATTEMPTS = "failedAttempts";      // Failed verification attempts
    }

    /**
     * Metadata schema for OAuth authentication methods
     */
    public static class OAuthMetadata {
        // OAuth provider details
        public static final String PROVIDER = "provider";                   // google, facebook, etc.
        public static final String PROVIDER_USER_ID = "providerUserId";     // User ID from provider
        public static final String EMAIL = "email";                         // Email from provider
        public static final String DISPLAY_NAME = "displayName";            // Display name from provider
        public static final String PROFILE_PICTURE = "profilePicture";      // Profile picture URL
        
        // Tokens (encrypted/hashed)
        public static final String ACCESS_TOKEN_HASH = "accessTokenHash";   // Hashed access token
        public static final String REFRESH_TOKEN_HASH = "refreshTokenHash"; // Hashed refresh token
        public static final String TOKEN_EXPIRY = "tokenExpiry";            // When tokens expire
        
        // Scopes and permissions
        public static final String GRANTED_SCOPES = "grantedScopes";        // Scopes granted by user
        public static final String PROFILE_VERIFIED = "profileVerified";    // Provider verified profile
        
        // Connection details
        public static final String CONNECTED_TIME = "connectedTime";        // When first connected
        public static final String LAST_SYNC_TIME = "lastSyncTime";         // Last profile sync
        public static final String CONNECTION_STATUS = "connectionStatus";   // active, revoked, expired
        
        // Usage tracking
        public static final String LAST_USED_TIME = "lastUsedTime";         // Last authentication
        public static final String LOGIN_COUNT = "loginCount";              // Number of OAuth logins
    }

    /**
     * Common metadata fields used across all authentication methods
     */
    public static class CommonMetadata {
        public static final String CREATED_TIME = "createdTime";            // When method was created
        public static final String UPDATED_TIME = "updatedTime";            // Last update time
        public static final String DEVICE_INFO = "deviceInfo";              // Device information
        public static final String IP_ADDRESS = "ipAddress";                // Registration IP
        public static final String USER_AGENT = "userAgent";                // Browser/app info
        public static final String RISK_SCORE = "riskScore";                // Security risk assessment
        public static final String NOTES = "notes";                         // Admin/user notes
    }

    /**
     * Validate metadata for a specific authentication method type
     */
    public static boolean validateMetadata(AuthenticationMethodType type, Map<String, Object> metadata) {
        if (metadata == null) {
            return false;
        }

        return switch (type) {
            case PASSKEY -> validatePasskeyMetadata(metadata);
            case TOTP -> validateTotpMetadata(metadata);
            case SMS_OTP -> validateSmsOtpMetadata(metadata);
            case EMAIL_OTP -> validateEmailOtpMetadata(metadata);
            case OAUTH_GOOGLE, OAUTH_FACEBOOK, OAUTH_MICROSOFT,
                 OAUTH_GITHUB, OAUTH_LINKEDIN, OAUTH_TWITTER -> validateOAuthMetadata(metadata);
            case DEVICE_TRUST -> true;
        };
    }

    private static boolean validatePasskeyMetadata(Map<String, Object> metadata) {
        // Required fields for passkey
        return metadata.containsKey(PasskeyMetadata.CREDENTIAL_ID) &&
               metadata.containsKey(PasskeyMetadata.PUBLIC_KEY_BASE64) &&
               metadata.get(PasskeyMetadata.CREDENTIAL_ID) instanceof String &&
               metadata.get(PasskeyMetadata.PUBLIC_KEY_BASE64) instanceof String;
    }

    private static boolean validateTotpMetadata(Map<String, Object> metadata) {
        // Required fields for TOTP
        return metadata.containsKey(TotpMetadata.SECRET_KEY) &&
               metadata.get(TotpMetadata.SECRET_KEY) instanceof String &&
               !((String) metadata.get(TotpMetadata.SECRET_KEY)).isEmpty();
    }

    private static boolean validateSmsOtpMetadata(Map<String, Object> metadata) {
        // Required fields for SMS OTP
        return metadata.containsKey(SmsOtpMetadata.PHONE_NUMBER) &&
               metadata.get(SmsOtpMetadata.PHONE_NUMBER) instanceof String &&
               ((String) metadata.get(SmsOtpMetadata.PHONE_NUMBER)).startsWith("+");
    }

    private static boolean validateEmailOtpMetadata(Map<String, Object> metadata) {
        // Required fields for Email OTP
        return metadata.containsKey(EmailOtpMetadata.EMAIL_ADDRESS) &&
               metadata.get(EmailOtpMetadata.EMAIL_ADDRESS) instanceof String &&
               ((String) metadata.get(EmailOtpMetadata.EMAIL_ADDRESS)).contains("@");
    }

    private static boolean validateOAuthMetadata(Map<String, Object> metadata) {
        // Required fields for OAuth
        return metadata.containsKey(OAuthMetadata.PROVIDER) &&
               metadata.containsKey(OAuthMetadata.PROVIDER_USER_ID) &&
               metadata.get(OAuthMetadata.PROVIDER) instanceof String &&
               metadata.get(OAuthMetadata.PROVIDER_USER_ID) instanceof String;
    }
}