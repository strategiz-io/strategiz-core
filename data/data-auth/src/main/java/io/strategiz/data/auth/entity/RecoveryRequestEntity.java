package io.strategiz.data.auth.entity;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.PropertyName;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.strategiz.data.base.entity.BaseEntity;
import io.strategiz.data.base.annotation.Collection;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Entity for account recovery requests stored in recovery_requests collection.
 * Tracks the multi-step recovery process: email verification, optional SMS verification.
 *
 * <p>Recovery flow:</p>
 * <ol>
 *   <li>User initiates recovery → PENDING_EMAIL</li>
 *   <li>User verifies email → PENDING_SMS (if MFA) or issues recovery token</li>
 *   <li>User verifies SMS → issues recovery token</li>
 *   <li>Recovery token allows user to disable MFA, setup new passkey, etc.</li>
 * </ol>
 *
 * Inherits audit fields from BaseEntity (createdBy, modifiedBy, createdDate, modifiedDate, isActive, version)
 */
@Entity
@Table(name = "recovery_requests")
@Collection("recovery_requests")
public class RecoveryRequestEntity extends BaseEntity {

    @Id
    @DocumentId
    @PropertyName("id")
    @JsonProperty("id")
    @Column(name = "id")
    private String id;

    @PropertyName("userId")
    @JsonProperty("userId")
    @NotNull(message = "User ID is required")
    private String userId;

    @PropertyName("email")
    @JsonProperty("email")
    @NotNull(message = "Email is required")
    private String email;

    @PropertyName("status")
    @JsonProperty("status")
    @Enumerated(EnumType.STRING)
    @NotNull(message = "Status is required")
    private RecoveryStatus status;

    @PropertyName("emailVerified")
    @JsonProperty("emailVerified")
    private Boolean emailVerified = false;

    @PropertyName("smsVerified")
    @JsonProperty("smsVerified")
    private Boolean smsVerified = false;

    @PropertyName("mfaRequired")
    @JsonProperty("mfaRequired")
    private Boolean mfaRequired = false;

    @PropertyName("phoneNumber")
    @JsonProperty("phoneNumber")
    private String phoneNumber;

    @PropertyName("phoneNumberHint")
    @JsonProperty("phoneNumberHint")
    private String phoneNumberHint;

    @PropertyName("expiresAt")
    @JsonProperty("expiresAt")
    @NotNull(message = "Expiration time is required")
    private Instant expiresAt;

    @PropertyName("completedAt")
    @JsonProperty("completedAt")
    private Instant completedAt;

    @PropertyName("ipAddress")
    @JsonProperty("ipAddress")
    private String ipAddress;

    @PropertyName("userAgent")
    @JsonProperty("userAgent")
    private String userAgent;

    @PropertyName("emailCode")
    @JsonProperty("emailCode")
    private String emailCode;

    @PropertyName("smsCode")
    @JsonProperty("smsCode")
    private String smsCode;

    @PropertyName("emailCodeExpiresAt")
    @JsonProperty("emailCodeExpiresAt")
    private Instant emailCodeExpiresAt;

    @PropertyName("smsCodeExpiresAt")
    @JsonProperty("smsCodeExpiresAt")
    private Instant smsCodeExpiresAt;

    @PropertyName("emailAttempts")
    @JsonProperty("emailAttempts")
    private Integer emailAttempts = 0;

    @PropertyName("smsAttempts")
    @JsonProperty("smsAttempts")
    private Integer smsAttempts = 0;

    @PropertyName("usedForAuthentication")
    @JsonProperty("usedForAuthentication")
    private Boolean usedForAuthentication = false;

    // Constructors
    public RecoveryRequestEntity() {
        super();
        this.status = RecoveryStatus.PENDING_EMAIL;
        this.emailVerified = false;
        this.smsVerified = false;
        this.emailAttempts = 0;
        this.smsAttempts = 0;
    }

    public RecoveryRequestEntity(String userId, String email) {
        super();
        this.userId = userId;
        this.email = email;
        this.status = RecoveryStatus.PENDING_EMAIL;
        this.emailVerified = false;
        this.smsVerified = false;
        this.emailAttempts = 0;
        this.smsAttempts = 0;
        this.expiresAt = Instant.now().plusSeconds(30 * 60); // 30 minutes
    }

    // Getters and Setters
    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public RecoveryStatus getStatus() {
        return status;
    }

    public void setStatus(RecoveryStatus status) {
        this.status = status;
    }

    public Boolean getEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public Boolean getSmsVerified() {
        return smsVerified;
    }

    public void setSmsVerified(Boolean smsVerified) {
        this.smsVerified = smsVerified;
    }

    public Boolean getMfaRequired() {
        return mfaRequired;
    }

    public void setMfaRequired(Boolean mfaRequired) {
        this.mfaRequired = mfaRequired;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getPhoneNumberHint() {
        return phoneNumberHint;
    }

    public void setPhoneNumberHint(String phoneNumberHint) {
        this.phoneNumberHint = phoneNumberHint;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getEmailCode() {
        return emailCode;
    }

    public void setEmailCode(String emailCode) {
        this.emailCode = emailCode;
    }

    public String getSmsCode() {
        return smsCode;
    }

    public void setSmsCode(String smsCode) {
        this.smsCode = smsCode;
    }

    public Instant getEmailCodeExpiresAt() {
        return emailCodeExpiresAt;
    }

    public void setEmailCodeExpiresAt(Instant emailCodeExpiresAt) {
        this.emailCodeExpiresAt = emailCodeExpiresAt;
    }

    public Instant getSmsCodeExpiresAt() {
        return smsCodeExpiresAt;
    }

    public void setSmsCodeExpiresAt(Instant smsCodeExpiresAt) {
        this.smsCodeExpiresAt = smsCodeExpiresAt;
    }

    public Integer getEmailAttempts() {
        return emailAttempts;
    }

    public void setEmailAttempts(Integer emailAttempts) {
        this.emailAttempts = emailAttempts;
    }

    public Integer getSmsAttempts() {
        return smsAttempts;
    }

    public void setSmsAttempts(Integer smsAttempts) {
        this.smsAttempts = smsAttempts;
    }

    public Boolean getUsedForAuthentication() {
        return usedForAuthentication;
    }

    public void setUsedForAuthentication(Boolean usedForAuthentication) {
        this.usedForAuthentication = usedForAuthentication;
    }

    // Convenience methods
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    public boolean isEmailCodeExpired() {
        return emailCodeExpiresAt != null && Instant.now().isAfter(emailCodeExpiresAt);
    }

    public boolean isSmsCodeExpired() {
        return smsCodeExpiresAt != null && Instant.now().isAfter(smsCodeExpiresAt);
    }

    public void incrementEmailAttempts() {
        this.emailAttempts = (this.emailAttempts == null ? 0 : this.emailAttempts) + 1;
    }

    public void incrementSmsAttempts() {
        this.smsAttempts = (this.smsAttempts == null ? 0 : this.smsAttempts) + 1;
    }

    public boolean hasExceededEmailAttempts(int maxAttempts) {
        return this.emailAttempts != null && this.emailAttempts >= maxAttempts;
    }

    public boolean hasExceededSmsAttempts(int maxAttempts) {
        return this.smsAttempts != null && this.smsAttempts >= maxAttempts;
    }

    public void markEmailVerified() {
        this.emailVerified = true;
        if (this.mfaRequired != null && this.mfaRequired) {
            this.status = RecoveryStatus.PENDING_SMS;
        }
    }

    public void markSmsVerified() {
        this.smsVerified = true;
    }

    public void markCompleted() {
        this.status = RecoveryStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    public void markExpired() {
        this.status = RecoveryStatus.EXPIRED;
    }

    public void markCancelled() {
        this.status = RecoveryStatus.CANCELLED;
    }

    /**
     * Check if recovery is ready to issue a token.
     * Either: email verified + no MFA required
     * Or: email verified + SMS verified
     */
    public boolean isReadyForToken() {
        if (!Boolean.TRUE.equals(emailVerified)) {
            return false;
        }
        if (Boolean.TRUE.equals(mfaRequired)) {
            return Boolean.TRUE.equals(smsVerified);
        }
        return true;
    }
}
