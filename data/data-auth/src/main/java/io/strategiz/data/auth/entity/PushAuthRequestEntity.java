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
 * Entity for pending push authentication requests.
 * Tracks the state of a push auth flow from initiation to completion.
 *
 * <p>Push auth flow:</p>
 * <ol>
 *   <li>User initiates sign-in → PENDING</li>
 *   <li>Push notification sent to device(s)</li>
 *   <li>User approves on device → APPROVED</li>
 *   <li>Or user denies → DENIED</li>
 *   <li>Or timeout → EXPIRED</li>
 * </ol>
 */
@Entity
@Table(name = "push_auth_requests")
@Collection("push_auth_requests")
public class PushAuthRequestEntity extends BaseEntity {

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

    @PropertyName("status")
    @JsonProperty("status")
    @Enumerated(EnumType.STRING)
    @NotNull(message = "Status is required")
    private PushAuthStatus status;

    /**
     * Purpose of this push auth request.
     * e.g., "signin", "mfa", "recovery"
     */
    @PropertyName("purpose")
    @JsonProperty("purpose")
    @NotNull(message = "Purpose is required")
    private String purpose;

    /**
     * Challenge token that must be returned when approving.
     * Prevents replay attacks.
     */
    @PropertyName("challenge")
    @JsonProperty("challenge")
    @NotNull(message = "Challenge is required")
    private String challenge;

    /**
     * When this request expires.
     */
    @PropertyName("expiresAt")
    @JsonProperty("expiresAt")
    @NotNull(message = "Expiration time is required")
    private Instant expiresAt;

    /**
     * When the user responded (approved/denied).
     */
    @PropertyName("respondedAt")
    @JsonProperty("respondedAt")
    private Instant respondedAt;

    /**
     * ID of the subscription that was used to approve.
     */
    @PropertyName("approvedBySubscriptionId")
    @JsonProperty("approvedBySubscriptionId")
    private String approvedBySubscriptionId;

    /**
     * IP address of the sign-in attempt.
     * Shown to user in push notification.
     */
    @PropertyName("ipAddress")
    @JsonProperty("ipAddress")
    private String ipAddress;

    /**
     * User agent of the sign-in attempt.
     * Shown to user in push notification.
     */
    @PropertyName("userAgent")
    @JsonProperty("userAgent")
    private String userAgent;

    /**
     * Location derived from IP (city, country).
     * Shown to user in push notification.
     */
    @PropertyName("location")
    @JsonProperty("location")
    private String location;

    /**
     * Number of push notifications sent for this request.
     */
    @PropertyName("notificationsSent")
    @JsonProperty("notificationsSent")
    private Integer notificationsSent = 0;

    /**
     * Related recovery request ID (if purpose is "recovery").
     */
    @PropertyName("recoveryRequestId")
    @JsonProperty("recoveryRequestId")
    private String recoveryRequestId;

    // Constructors
    public PushAuthRequestEntity() {
        super();
        this.status = PushAuthStatus.PENDING;
        this.notificationsSent = 0;
    }

    public PushAuthRequestEntity(String userId) {
        super();
        this.userId = userId;
        this.status = PushAuthStatus.PENDING;
        this.notificationsSent = 0;
    }

    public PushAuthRequestEntity(String userId, String purpose, String challenge, int expirySeconds) {
        super();
        this.userId = userId;
        this.purpose = purpose;
        this.challenge = challenge;
        this.status = PushAuthStatus.PENDING;
        this.expiresAt = Instant.now().plusSeconds(expirySeconds);
        this.notificationsSent = 0;
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

    public PushAuthStatus getStatus() {
        return status;
    }

    public void setStatus(PushAuthStatus status) {
        this.status = status;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getChallenge() {
        return challenge;
    }

    public void setChallenge(String challenge) {
        this.challenge = challenge;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(Instant respondedAt) {
        this.respondedAt = respondedAt;
    }

    public String getApprovedBySubscriptionId() {
        return approvedBySubscriptionId;
    }

    public void setApprovedBySubscriptionId(String approvedBySubscriptionId) {
        this.approvedBySubscriptionId = approvedBySubscriptionId;
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

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Integer getNotificationsSent() {
        return notificationsSent;
    }

    public void setNotificationsSent(Integer notificationsSent) {
        this.notificationsSent = notificationsSent;
    }

    public String getRecoveryRequestId() {
        return recoveryRequestId;
    }

    public void setRecoveryRequestId(String recoveryRequestId) {
        this.recoveryRequestId = recoveryRequestId;
    }

    // Convenience methods

    /**
     * Check if this request has expired.
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    /**
     * Check if this request is still pending and valid.
     */
    public boolean isPending() {
        return status == PushAuthStatus.PENDING && !isExpired();
    }

    /**
     * Approve this request.
     */
    public void approve(String subscriptionId) {
        this.status = PushAuthStatus.APPROVED;
        this.respondedAt = Instant.now();
        this.approvedBySubscriptionId = subscriptionId;
    }

    /**
     * Deny this request.
     */
    public void deny() {
        this.status = PushAuthStatus.DENIED;
        this.respondedAt = Instant.now();
    }

    /**
     * Mark this request as expired.
     */
    public void markExpired() {
        this.status = PushAuthStatus.EXPIRED;
    }

    /**
     * Increment notification count.
     */
    public void incrementNotificationCount() {
        this.notificationsSent = (this.notificationsSent == null ? 0 : this.notificationsSent) + 1;
    }
}
