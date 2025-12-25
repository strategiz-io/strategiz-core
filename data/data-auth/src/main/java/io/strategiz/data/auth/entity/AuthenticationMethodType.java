package io.strategiz.data.auth.entity;

import io.strategiz.data.base.exception.DataRepositoryException;
import io.strategiz.data.base.exception.DataRepositoryErrorDetails;

/**
 * Enum representing different types of authentication methods
 */
public enum AuthenticationMethodType {
    PASSKEY("passkey", "Passkey/WebAuthn"),
    TOTP("totp", "Time-based One-Time Password"),
    SMS_OTP("sms_otp", "SMS One-Time Password"),
    EMAIL_OTP("email_otp", "Email One-Time Password"),
    OAUTH_GOOGLE("oauth_google", "Google OAuth"),
    OAUTH_FACEBOOK("oauth_facebook", "Facebook OAuth"),
    OAUTH_MICROSOFT("oauth_microsoft", "Microsoft OAuth"),
    OAUTH_GITHUB("oauth_github", "GitHub OAuth"),
    OAUTH_LINKEDIN("oauth_linkedin", "LinkedIn OAuth"),
    OAUTH_TWITTER("oauth_twitter", "Twitter OAuth");

    private final String value;
    private final String displayName;

    AuthenticationMethodType(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    public String getValue() {
        return value;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static AuthenticationMethodType fromValue(String value) {
        for (AuthenticationMethodType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new DataRepositoryException(DataRepositoryErrorDetails.INVALID_ARGUMENT,
            "AuthenticationMethodType", "Unknown authentication method type: " + value);
    }

    public boolean isOAuth() {
        return this.name().startsWith("OAUTH_");
    }

    public boolean isOTP() {
        return this == TOTP || this == SMS_OTP || this == EMAIL_OTP;
    }

    public boolean isBiometric() {
        return this == PASSKEY;
    }
}