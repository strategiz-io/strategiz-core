package io.strategiz.data.device.model;

import io.strategiz.data.base.entity.BaseEntity;
import io.strategiz.data.device.constants.DeviceConstants;
import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.PropertyName;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Entity representing a device identity
 */
public class DeviceIdentity extends BaseEntity {

    @DocumentId
    private String id;

    @PropertyName("device_id")
    @JsonProperty("device_id")
    private String deviceId;

    @PropertyName("user_id")
    @JsonProperty("user_id")
    private String userId;

    @PropertyName("device_name")
    @JsonProperty("device_name")
    private String deviceName;

    @PropertyName("first_seen")
    @JsonProperty("first_seen")
    private Instant firstSeen;

    @PropertyName("last_seen")
    @JsonProperty("last_seen")
    private Instant lastSeen;

    // Platform properties as individual fields
    @PropertyName("platform_brand")
    @JsonProperty("platform_brand")
    private String platformBrand;

    @PropertyName("platform_model")
    @JsonProperty("platform_model")
    private String platformModel;

    @PropertyName("platform_os")
    @JsonProperty("platform_os")
    private String platformOs;

    @PropertyName("platform_type")
    @JsonProperty("platform_type")
    private String platformType;

    @PropertyName("user_agent")
    @JsonProperty("user_agent")
    private String userAgent;

    @PropertyName("platform_version")
    @JsonProperty("platform_version")
    private String platformVersion;

    @PropertyName("trusted")
    @JsonProperty("trusted")
    private boolean trusted;

    @PropertyName("public_key")
    @JsonProperty("public_key")
    private String publicKey;

    /**
     * Default constructor
     */
    public DeviceIdentity() {
        super();
    }

    /**
     * Create a new device identity
     *
     * @param deviceId Unique device identifier
     * @param deviceName User-friendly device name
     * @param userId User ID who owns this device
     */
    public DeviceIdentity(String deviceId, String deviceName, String userId) {
        super(userId);
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.userId = userId;
        this.firstSeen = Instant.now();
        this.lastSeen = this.firstSeen;
        this.trusted = false;
    }

    @Override
    public String getCollectionName() {
        // For authenticated devices, this would be overridden by the repository
        // to return the subcollection path. Default to anonymous devices collection.
        return DeviceConstants.Collections.ANONYMOUS_DEVICES;
    }
    
    /**
     * Check if this device is authenticated (has a userId)
     * @return true if this device belongs to a user
     */
    public boolean isAuthenticated() {
        return userId != null && !userId.isEmpty() && !"anonymous".equals(userId);
    }

    // Getters and setters

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public Instant getFirstSeen() {
        return firstSeen;
    }

    public void setFirstSeen(Instant firstSeen) {
        this.firstSeen = firstSeen;
    }

    public Instant getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(Instant lastSeen) {
        this.lastSeen = lastSeen;
    }

    public String getPlatformBrand() {
        return platformBrand;
    }

    public void setPlatformBrand(String platformBrand) {
        this.platformBrand = platformBrand;
    }

    public String getPlatformModel() {
        return platformModel;
    }

    public void setPlatformModel(String platformModel) {
        this.platformModel = platformModel;
    }

    public String getPlatformOs() {
        return platformOs;
    }

    public void setPlatformOs(String platformOs) {
        this.platformOs = platformOs;
    }

    public String getPlatformType() {
        return platformType;
    }

    public void setPlatformType(String platformType) {
        this.platformType = platformType;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getPlatformVersion() {
        return platformVersion;
    }

    public void setPlatformVersion(String platformVersion) {
        this.platformVersion = platformVersion;
    }

    public boolean isTrusted() {
        return trusted;
    }

    public void setTrusted(boolean trusted) {
        this.trusted = trusted;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    @Override
    public String toString() {
        return "DeviceIdentity{" +
                "id='" + id + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", deviceName='" + deviceName + '\'' +
                ", userId='" + userId + '\'' +
                ", platformType='" + platformType + '\'' +
                ", trusted=" + trusted +
                '}';
    }
}
