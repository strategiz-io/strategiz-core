package io.strategiz.client.robinhood.model;

/**
 * Result of Robinhood login attempt.
 * Encapsulates the various states that can occur during login:
 * - Success with tokens
 * - MFA required (SMS or email challenge)
 * - Device approval required
 * - Error
 */
public class RobinhoodLoginResult {

    public enum Status {
        SUCCESS,
        MFA_REQUIRED,
        DEVICE_APPROVAL_REQUIRED,
        ERROR
    }

    private Status status;
    private String accessToken;
    private String refreshToken;
    private Integer expiresIn;
    private RobinhoodChallenge challenge;
    private String challengeType; // "sms" or "email"
    private String deviceToken;
    private String errorMessage;
    private String errorCode;

    // Factory methods
    public static RobinhoodLoginResult success(String accessToken, String refreshToken, Integer expiresIn) {
        RobinhoodLoginResult result = new RobinhoodLoginResult();
        result.status = Status.SUCCESS;
        result.accessToken = accessToken;
        result.refreshToken = refreshToken;
        result.expiresIn = expiresIn;
        return result;
    }

    public static RobinhoodLoginResult mfaRequired(RobinhoodChallenge challenge, String challengeType, String deviceToken) {
        RobinhoodLoginResult result = new RobinhoodLoginResult();
        result.status = Status.MFA_REQUIRED;
        result.challenge = challenge;
        result.challengeType = challengeType;
        result.deviceToken = deviceToken;
        return result;
    }

    public static RobinhoodLoginResult deviceApprovalRequired(String deviceToken) {
        RobinhoodLoginResult result = new RobinhoodLoginResult();
        result.status = Status.DEVICE_APPROVAL_REQUIRED;
        result.deviceToken = deviceToken;
        return result;
    }

    public static RobinhoodLoginResult error(String errorMessage, String errorCode) {
        RobinhoodLoginResult result = new RobinhoodLoginResult();
        result.status = Status.ERROR;
        result.errorMessage = errorMessage;
        result.errorCode = errorCode;
        return result;
    }

    // Getters
    public Status getStatus() {
        return status;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public Integer getExpiresIn() {
        return expiresIn;
    }

    public RobinhoodChallenge getChallenge() {
        return challenge;
    }

    public String getChallengeType() {
        return challengeType;
    }

    public String getDeviceToken() {
        return deviceToken;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    public boolean isMfaRequired() {
        return status == Status.MFA_REQUIRED;
    }

    public boolean isDeviceApprovalRequired() {
        return status == Status.DEVICE_APPROVAL_REQUIRED;
    }

    public boolean isError() {
        return status == Status.ERROR;
    }
}
