package io.strategiz.data.device.model;

import io.strategiz.data.device.constants.DeviceConstants;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Entity representing a device identity
 */
@Entity(name = "DeviceIdentity")
@Table(name = DeviceConstants.Collections.DEVICE_IDENTITIES)
public class DeviceIdentity {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "device_id")
    private String deviceId;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "device_name")
    private String deviceName;

    @Column(name = "first_seen", columnDefinition = "TIMESTAMP")
    private Instant firstSeen;

    @Column(name = "last_seen", columnDefinition = "TIMESTAMP")
    private Instant lastSeen;

    // Platform properties as individual columns
    @Column(name = "platform_brand")
    private String platformBrand;

    @Column(name = "platform_model")
    private String platformModel;

    @Column(name = "platform_os")
    private String platformOs;

    @Column(name = "platform_type")
    private String platformType;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "platform_version")
    private String platformVersion;

    @Column(name = "trusted")
    private boolean trusted;

    @Column(name = "public_key")
    private String publicKey;

    /**
     * Default constructor
     */
    public DeviceIdentity() {
        // Required by JPA
    }

    /**
     * Create a new device identity
     *
     * @param deviceId Unique device identifier
     * @param deviceName User-friendly device name
     */
    public DeviceIdentity(String deviceId, String deviceName) {
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.firstSeen = Instant.now();
        this.lastSeen = this.firstSeen;
        this.trusted = false;
    }

    @PrePersist
    protected void onCreate() {
        if (firstSeen == null) {
            firstSeen = Instant.now();
        }
        if (lastSeen == null) {
            lastSeen = firstSeen;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        lastSeen = Instant.now();
    }

    // Getters and setters

    public String getId() {
        return id;
    }

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
