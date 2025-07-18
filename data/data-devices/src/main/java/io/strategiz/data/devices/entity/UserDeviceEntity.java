package io.strategiz.data.devices.entity;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.PropertyName;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.strategiz.data.base.entity.BaseEntity;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

/**
 * User device for users/{userId}/devices subcollection
 * Represents devices used to access the user account
 */
public class UserDeviceEntity extends BaseEntity {

    @DocumentId
    @PropertyName("deviceId")
    @JsonProperty("deviceId")
    private String deviceId;

    @PropertyName("deviceName")
    @JsonProperty("deviceName")
    @NotBlank(message = "Device name is required")
    private String deviceName; // User-friendly name

    @PropertyName("agentId")
    @JsonProperty("agentId")
    private String agentId; // Unique identifier for the device agent

    @PropertyName("platform")
    @JsonProperty("platform")
    private DevicePlatform platform; // Platform details

    @PropertyName("lastLoginAt")
    @JsonProperty("lastLoginAt")
    private Instant lastLoginAt;

    @PropertyName("ipAddress")
    @JsonProperty("ipAddress")
    private String ipAddress; // Last known IP

    @PropertyName("location")
    @JsonProperty("location")
    private String location; // Approximate location

    @PropertyName("trusted")
    @JsonProperty("trusted")
    private Boolean trusted = false; // Whether device is trusted

    @PropertyName("pushToken")
    @JsonProperty("pushToken")
    private String pushToken; // For push notifications

    // Constructors
    public UserDeviceEntity() {
        super();
    }

    public UserDeviceEntity(String deviceName, String agentId) {
        super();
        this.deviceName = deviceName;
        this.agentId = agentId;
        this.trusted = false;
    }

    public UserDeviceEntity(String deviceName, String agentId, DevicePlatform platform) {
        super();
        this.deviceName = deviceName;
        this.agentId = agentId;
        this.platform = platform;
        this.trusted = false;
    }

    // Getters and Setters
    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public DevicePlatform getPlatform() {
        return platform;
    }

    public void setPlatform(DevicePlatform platform) {
        this.platform = platform;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Boolean getTrusted() {
        return trusted;
    }

    public void setTrusted(Boolean trusted) {
        this.trusted = trusted;
    }

    public String getPushToken() {
        return pushToken;
    }

    public void setPushToken(String pushToken) {
        this.pushToken = pushToken;
    }

    // Convenience methods
    public boolean isTrusted() {
        return Boolean.TRUE.equals(trusted);
    }

    public void markAsUsed() {
        this.lastLoginAt = Instant.now();
    }

    public void markAsTrusted() {
        this.trusted = true;
    }

    public void markAsUntrusted() {
        this.trusted = false;
    }

    // Required BaseEntity methods
    @Override
    public String getId() {
        return deviceId;
    }

    @Override
    public void setId(String id) {
        this.deviceId = id;
    }

    @Override
    public String getCollectionName() {
        return "devices";
    }
}