package io.strategiz.data.device.model;

import io.strategiz.data.base.entity.BaseEntity;
import io.strategiz.data.base.annotation.Collection;
import io.strategiz.data.device.constants.DeviceConstants;
import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.PropertyName;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Entity representing a device identity with comprehensive fingerprinting Extends
 * BaseEntity for audit fields and includes all device fingerprint data
 *
 * Storage locations: - Anonymous devices: /devices (root collection) - Authenticated
 * devices: /users/{userId}/devices (subcollection)
 *
 * The collection path is determined by the repository implementation, not by this
 * annotation. The @Collection here is for the default/anonymous case.
 */
@Collection("devices")
public class DeviceIdentity extends BaseEntity {

	@DocumentId
	private String id;

	// Core identification
	@PropertyName("device_id")
	@JsonProperty("device_id")
	private String deviceId;

	@PropertyName("user_id")
	@JsonProperty("user_id")
	private String userId;

	@PropertyName("device_name")
	@JsonProperty("device_name")
	private String deviceName;

	// Timestamps
	@PropertyName("first_seen")
	@JsonProperty("first_seen")
	private Instant firstSeen;

	@PropertyName("last_seen")
	@JsonProperty("last_seen")
	private Instant lastSeen;

	// Fingerprint data
	@PropertyName("fingerprint_confidence")
	@JsonProperty("fingerprint_confidence")
	private Double fingerprintConfidence;

	@PropertyName("visitor_id")
	@JsonProperty("visitor_id")
	private String visitorId;

	// Browser information
	@PropertyName("browser_name")
	@JsonProperty("browser_name")
	private String browserName;

	@PropertyName("browser_version")
	@JsonProperty("browser_version")
	private String browserVersion;

	@PropertyName("browser_vendor")
	@JsonProperty("browser_vendor")
	private String browserVendor;

	@PropertyName("user_agent")
	@JsonProperty("user_agent")
	private String userAgent;

	@PropertyName("cookies_enabled")
	@JsonProperty("cookies_enabled")
	private Boolean cookiesEnabled;

	@PropertyName("do_not_track")
	@JsonProperty("do_not_track")
	private String doNotTrack;

	@PropertyName("language")
	@JsonProperty("language")
	private String language;

	@PropertyName("languages")
	@JsonProperty("languages")
	private List<String> languages;

	// Operating System
	@PropertyName("os_name")
	@JsonProperty("os_name")
	private String osName;

	@PropertyName("os_version")
	@JsonProperty("os_version")
	private String osVersion;

	@PropertyName("platform")
	@JsonProperty("platform")
	private String platform;

	@PropertyName("architecture")
	@JsonProperty("architecture")
	private String architecture;

	// Hardware capabilities
	@PropertyName("screen_resolution")
	@JsonProperty("screen_resolution")
	private String screenResolution;

	@PropertyName("available_screen_resolution")
	@JsonProperty("available_screen_resolution")
	private String availableScreenResolution;

	@PropertyName("color_depth")
	@JsonProperty("color_depth")
	private Integer colorDepth;

	@PropertyName("pixel_ratio")
	@JsonProperty("pixel_ratio")
	private Double pixelRatio;

	@PropertyName("hardware_concurrency")
	@JsonProperty("hardware_concurrency")
	private Integer hardwareConcurrency;

	@PropertyName("device_memory")
	@JsonProperty("device_memory")
	private Integer deviceMemory;

	@PropertyName("max_touch_points")
	@JsonProperty("max_touch_points")
	private Integer maxTouchPoints;

	// Rendering fingerprints
	@PropertyName("canvas_fingerprint")
	@JsonProperty("canvas_fingerprint")
	private String canvasFingerprint;

	@PropertyName("webgl_vendor")
	@JsonProperty("webgl_vendor")
	private String webglVendor;

	@PropertyName("webgl_renderer")
	@JsonProperty("webgl_renderer")
	private String webglRenderer;

	@PropertyName("webgl_unmasked_vendor")
	@JsonProperty("webgl_unmasked_vendor")
	private String webglUnmaskedVendor;

	@PropertyName("webgl_unmasked_renderer")
	@JsonProperty("webgl_unmasked_renderer")
	private String webglUnmaskedRenderer;

	@PropertyName("fonts")
	@JsonProperty("fonts")
	private List<String> fonts;

	// Audio fingerprint
	@PropertyName("audio_fingerprint")
	@JsonProperty("audio_fingerprint")
	private String audioFingerprint;

	// Network information
	@PropertyName("timezone")
	@JsonProperty("timezone")
	private String timezone;

	@PropertyName("timezone_offset")
	@JsonProperty("timezone_offset")
	private Integer timezoneOffset;

	@PropertyName("ip_address")
	@JsonProperty("ip_address")
	private String ipAddress;

	@PropertyName("ip_location")
	@JsonProperty("ip_location")
	private String ipLocation;

	// Media capabilities
	@PropertyName("audio_codecs")
	@JsonProperty("audio_codecs")
	private List<String> audioCodecs;

	@PropertyName("video_codecs")
	@JsonProperty("video_codecs")
	private List<String> videoCodecs;

	@PropertyName("speaker_count")
	@JsonProperty("speaker_count")
	private Integer speakerCount;

	@PropertyName("microphone_count")
	@JsonProperty("microphone_count")
	private Integer microphoneCount;

	@PropertyName("webcam_count")
	@JsonProperty("webcam_count")
	private Integer webcamCount;

	// Browser features
	@PropertyName("has_local_storage")
	@JsonProperty("has_local_storage")
	private Boolean hasLocalStorage;

	@PropertyName("has_session_storage")
	@JsonProperty("has_session_storage")
	private Boolean hasSessionStorage;

	@PropertyName("has_indexed_db")
	@JsonProperty("has_indexed_db")
	private Boolean hasIndexedDB;

	@PropertyName("has_web_rtc")
	@JsonProperty("has_web_rtc")
	private Boolean hasWebRTC;

	@PropertyName("has_web_assembly")
	@JsonProperty("has_web_assembly")
	private Boolean hasWebAssembly;

	@PropertyName("has_service_worker")
	@JsonProperty("has_service_worker")
	private Boolean hasServiceWorker;

	@PropertyName("has_push_notifications")
	@JsonProperty("has_push_notifications")
	private Boolean hasPushNotifications;

	// FCM Push Notification Token
	@PropertyName("fcm_token")
	@JsonProperty("fcm_token")
	private String fcmToken;

	@PropertyName("fcm_token_updated_at")
	@JsonProperty("fcm_token_updated_at")
	private Instant fcmTokenUpdatedAt;

	// Trust indicators
	@PropertyName("incognito_mode")
	@JsonProperty("incognito_mode")
	private Boolean incognitoMode;

	@PropertyName("ad_block_enabled")
	@JsonProperty("ad_block_enabled")
	private Boolean adBlockEnabled;

	@PropertyName("vpn_detected")
	@JsonProperty("vpn_detected")
	private Boolean vpnDetected;

	@PropertyName("proxy_detected")
	@JsonProperty("proxy_detected")
	private Boolean proxyDetected;

	@PropertyName("bot_detected")
	@JsonProperty("bot_detected")
	private Boolean botDetected;

	@PropertyName("tampering_detected")
	@JsonProperty("tampering_detected")
	private Boolean tamperingDetected;

	// Lie detection
	@PropertyName("has_lied_languages")
	@JsonProperty("has_lied_languages")
	private Boolean hasLiedLanguages;

	@PropertyName("has_lied_resolution")
	@JsonProperty("has_lied_resolution")
	private Boolean hasLiedResolution;

	@PropertyName("has_lied_os")
	@JsonProperty("has_lied_os")
	private Boolean hasLiedOs;

	@PropertyName("has_lied_browser")
	@JsonProperty("has_lied_browser")
	private Boolean hasLiedBrowser;

	// Cryptographic key
	@PropertyName("public_key")
	@JsonProperty("public_key")
	private String publicKey;

	// Trust and metadata
	@PropertyName("trust_level")
	@JsonProperty("trust_level")
	private String trustLevel;

	@PropertyName("registration_type")
	@JsonProperty("registration_type")
	private String registrationType;

	@PropertyName("trusted")
	@JsonProperty("trusted")
	private boolean trusted;

	// Device trust fields
	@PropertyName("trust_expires_at")
	@JsonProperty("trust_expires_at")
	private Instant trustExpiresAt;

	@PropertyName("last_trust_verification")
	@JsonProperty("last_trust_verification")
	private Instant lastTrustVerification;

	@PropertyName("crypto_verified")
	@JsonProperty("crypto_verified")
	private Boolean cryptoVerified;

	@PropertyName("baseline_fingerprint")
	@JsonProperty("baseline_fingerprint")
	private String baselineFingerprint;

	// Additional metadata stored as map for flexibility
	@PropertyName("device_info")
	@JsonProperty("device_info")
	private Map<String, Object> deviceInfo;

	/**
	 * Default constructor
	 */
	public DeviceIdentity() {
		super();
	}

	/**
	 * Create a new device identity
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
		this.trustLevel = "pending";
	}

	/**
	 * Check if this device's trust is still valid. Trust is valid when the device is
	 * trusted, has a trust score >= 80, and the trust has not expired.
	 * @return true if device trust is currently valid
	 */
	public boolean isTrustValid() {
		if (!trusted) {
			return false;
		}
		if (trustExpiresAt != null && Instant.now().isAfter(trustExpiresAt)) {
			return false;
		}
		return getTrustScore() >= 80;
	}

	/**
	 * Check if this device is authenticated (has a userId)
	 * @return true if this device belongs to a user
	 */
	public boolean isAuthenticated() {
		return userId != null && !userId.isEmpty() && !"anonymous".equals(userId);
	}

	/**
	 * Get trust score based on various indicators
	 * @return trust score between 0 and 100
	 */
	public int getTrustScore() {
		int score = 100;

		// Deduct points for suspicious indicators
		if (Boolean.TRUE.equals(incognitoMode))
			score -= 10;
		if (Boolean.TRUE.equals(vpnDetected))
			score -= 20;
		if (Boolean.TRUE.equals(proxyDetected))
			score -= 20;
		if (Boolean.TRUE.equals(botDetected))
			score -= 40;
		if (Boolean.TRUE.equals(tamperingDetected))
			score -= 30;
		if (Boolean.TRUE.equals(hasLiedLanguages))
			score -= 15;
		if (Boolean.TRUE.equals(hasLiedResolution))
			score -= 15;
		if (Boolean.TRUE.equals(hasLiedOs))
			score -= 15;
		if (Boolean.TRUE.equals(hasLiedBrowser))
			score -= 15;

		// Add points for positive indicators
		if (fingerprintConfidence != null && fingerprintConfidence > 0.95)
			score += 10;
		if (publicKey != null && !publicKey.isEmpty())
			score += 5;

		return Math.max(0, Math.min(100, score));
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

	public Double getFingerprintConfidence() {
		return fingerprintConfidence;
	}

	public void setFingerprintConfidence(Double fingerprintConfidence) {
		this.fingerprintConfidence = fingerprintConfidence;
	}

	public String getVisitorId() {
		return visitorId;
	}

	public void setVisitorId(String visitorId) {
		this.visitorId = visitorId;
	}

	public String getBrowserName() {
		return browserName;
	}

	public void setBrowserName(String browserName) {
		this.browserName = browserName;
	}

	public String getBrowserVersion() {
		return browserVersion;
	}

	public void setBrowserVersion(String browserVersion) {
		this.browserVersion = browserVersion;
	}

	public String getBrowserVendor() {
		return browserVendor;
	}

	public void setBrowserVendor(String browserVendor) {
		this.browserVendor = browserVendor;
	}

	public String getUserAgent() {
		return userAgent;
	}

	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	public Boolean getCookiesEnabled() {
		return cookiesEnabled;
	}

	public void setCookiesEnabled(Boolean cookiesEnabled) {
		this.cookiesEnabled = cookiesEnabled;
	}

	public String getDoNotTrack() {
		return doNotTrack;
	}

	public void setDoNotTrack(String doNotTrack) {
		this.doNotTrack = doNotTrack;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public List<String> getLanguages() {
		return languages;
	}

	public void setLanguages(List<String> languages) {
		this.languages = languages;
	}

	public String getOsName() {
		return osName;
	}

	public void setOsName(String osName) {
		this.osName = osName;
	}

	public String getOsVersion() {
		return osVersion;
	}

	public void setOsVersion(String osVersion) {
		this.osVersion = osVersion;
	}

	public String getPlatform() {
		return platform;
	}

	public void setPlatform(String platform) {
		this.platform = platform;
	}

	public String getArchitecture() {
		return architecture;
	}

	public void setArchitecture(String architecture) {
		this.architecture = architecture;
	}

	public String getScreenResolution() {
		return screenResolution;
	}

	public void setScreenResolution(String screenResolution) {
		this.screenResolution = screenResolution;
	}

	public String getAvailableScreenResolution() {
		return availableScreenResolution;
	}

	public void setAvailableScreenResolution(String availableScreenResolution) {
		this.availableScreenResolution = availableScreenResolution;
	}

	public Integer getColorDepth() {
		return colorDepth;
	}

	public void setColorDepth(Integer colorDepth) {
		this.colorDepth = colorDepth;
	}

	public Double getPixelRatio() {
		return pixelRatio;
	}

	public void setPixelRatio(Double pixelRatio) {
		this.pixelRatio = pixelRatio;
	}

	public Integer getHardwareConcurrency() {
		return hardwareConcurrency;
	}

	public void setHardwareConcurrency(Integer hardwareConcurrency) {
		this.hardwareConcurrency = hardwareConcurrency;
	}

	public Integer getDeviceMemory() {
		return deviceMemory;
	}

	public void setDeviceMemory(Integer deviceMemory) {
		this.deviceMemory = deviceMemory;
	}

	public Integer getMaxTouchPoints() {
		return maxTouchPoints;
	}

	public void setMaxTouchPoints(Integer maxTouchPoints) {
		this.maxTouchPoints = maxTouchPoints;
	}

	public String getCanvasFingerprint() {
		return canvasFingerprint;
	}

	public void setCanvasFingerprint(String canvasFingerprint) {
		this.canvasFingerprint = canvasFingerprint;
	}

	public String getWebglVendor() {
		return webglVendor;
	}

	public void setWebglVendor(String webglVendor) {
		this.webglVendor = webglVendor;
	}

	public String getWebglRenderer() {
		return webglRenderer;
	}

	public void setWebglRenderer(String webglRenderer) {
		this.webglRenderer = webglRenderer;
	}

	public String getWebglUnmaskedVendor() {
		return webglUnmaskedVendor;
	}

	public void setWebglUnmaskedVendor(String webglUnmaskedVendor) {
		this.webglUnmaskedVendor = webglUnmaskedVendor;
	}

	public String getWebglUnmaskedRenderer() {
		return webglUnmaskedRenderer;
	}

	public void setWebglUnmaskedRenderer(String webglUnmaskedRenderer) {
		this.webglUnmaskedRenderer = webglUnmaskedRenderer;
	}

	public List<String> getFonts() {
		return fonts;
	}

	public void setFonts(List<String> fonts) {
		this.fonts = fonts;
	}

	public String getAudioFingerprint() {
		return audioFingerprint;
	}

	public void setAudioFingerprint(String audioFingerprint) {
		this.audioFingerprint = audioFingerprint;
	}

	public String getTimezone() {
		return timezone;
	}

	public void setTimezone(String timezone) {
		this.timezone = timezone;
	}

	public Integer getTimezoneOffset() {
		return timezoneOffset;
	}

	public void setTimezoneOffset(Integer timezoneOffset) {
		this.timezoneOffset = timezoneOffset;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public String getIpLocation() {
		return ipLocation;
	}

	public void setIpLocation(String ipLocation) {
		this.ipLocation = ipLocation;
	}

	public List<String> getAudioCodecs() {
		return audioCodecs;
	}

	public void setAudioCodecs(List<String> audioCodecs) {
		this.audioCodecs = audioCodecs;
	}

	public List<String> getVideoCodecs() {
		return videoCodecs;
	}

	public void setVideoCodecs(List<String> videoCodecs) {
		this.videoCodecs = videoCodecs;
	}

	public Integer getSpeakerCount() {
		return speakerCount;
	}

	public void setSpeakerCount(Integer speakerCount) {
		this.speakerCount = speakerCount;
	}

	public Integer getMicrophoneCount() {
		return microphoneCount;
	}

	public void setMicrophoneCount(Integer microphoneCount) {
		this.microphoneCount = microphoneCount;
	}

	public Integer getWebcamCount() {
		return webcamCount;
	}

	public void setWebcamCount(Integer webcamCount) {
		this.webcamCount = webcamCount;
	}

	public Boolean getHasLocalStorage() {
		return hasLocalStorage;
	}

	public void setHasLocalStorage(Boolean hasLocalStorage) {
		this.hasLocalStorage = hasLocalStorage;
	}

	public Boolean getHasSessionStorage() {
		return hasSessionStorage;
	}

	public void setHasSessionStorage(Boolean hasSessionStorage) {
		this.hasSessionStorage = hasSessionStorage;
	}

	public Boolean getHasIndexedDB() {
		return hasIndexedDB;
	}

	public void setHasIndexedDB(Boolean hasIndexedDB) {
		this.hasIndexedDB = hasIndexedDB;
	}

	public Boolean getHasWebRTC() {
		return hasWebRTC;
	}

	public void setHasWebRTC(Boolean hasWebRTC) {
		this.hasWebRTC = hasWebRTC;
	}

	public Boolean getHasWebAssembly() {
		return hasWebAssembly;
	}

	public void setHasWebAssembly(Boolean hasWebAssembly) {
		this.hasWebAssembly = hasWebAssembly;
	}

	public Boolean getHasServiceWorker() {
		return hasServiceWorker;
	}

	public void setHasServiceWorker(Boolean hasServiceWorker) {
		this.hasServiceWorker = hasServiceWorker;
	}

	public Boolean getHasPushNotifications() {
		return hasPushNotifications;
	}

	public void setHasPushNotifications(Boolean hasPushNotifications) {
		this.hasPushNotifications = hasPushNotifications;
	}

	public String getFcmToken() {
		return fcmToken;
	}

	public void setFcmToken(String fcmToken) {
		this.fcmToken = fcmToken;
		this.fcmTokenUpdatedAt = Instant.now();
	}

	public Instant getFcmTokenUpdatedAt() {
		return fcmTokenUpdatedAt;
	}

	public void setFcmTokenUpdatedAt(Instant fcmTokenUpdatedAt) {
		this.fcmTokenUpdatedAt = fcmTokenUpdatedAt;
	}

	/**
	 * Check if this device has a valid FCM token for push notifications
	 */
	public boolean hasFcmToken() {
		return fcmToken != null && !fcmToken.isEmpty();
	}

	public Boolean getIncognitoMode() {
		return incognitoMode;
	}

	public void setIncognitoMode(Boolean incognitoMode) {
		this.incognitoMode = incognitoMode;
	}

	public Boolean getAdBlockEnabled() {
		return adBlockEnabled;
	}

	public void setAdBlockEnabled(Boolean adBlockEnabled) {
		this.adBlockEnabled = adBlockEnabled;
	}

	public Boolean getVpnDetected() {
		return vpnDetected;
	}

	public void setVpnDetected(Boolean vpnDetected) {
		this.vpnDetected = vpnDetected;
	}

	public Boolean getProxyDetected() {
		return proxyDetected;
	}

	public void setProxyDetected(Boolean proxyDetected) {
		this.proxyDetected = proxyDetected;
	}

	public Boolean getBotDetected() {
		return botDetected;
	}

	public void setBotDetected(Boolean botDetected) {
		this.botDetected = botDetected;
	}

	public Boolean getTamperingDetected() {
		return tamperingDetected;
	}

	public void setTamperingDetected(Boolean tamperingDetected) {
		this.tamperingDetected = tamperingDetected;
	}

	public Boolean getHasLiedLanguages() {
		return hasLiedLanguages;
	}

	public void setHasLiedLanguages(Boolean hasLiedLanguages) {
		this.hasLiedLanguages = hasLiedLanguages;
	}

	public Boolean getHasLiedResolution() {
		return hasLiedResolution;
	}

	public void setHasLiedResolution(Boolean hasLiedResolution) {
		this.hasLiedResolution = hasLiedResolution;
	}

	public Boolean getHasLiedOs() {
		return hasLiedOs;
	}

	public void setHasLiedOs(Boolean hasLiedOs) {
		this.hasLiedOs = hasLiedOs;
	}

	public Boolean getHasLiedBrowser() {
		return hasLiedBrowser;
	}

	public void setHasLiedBrowser(Boolean hasLiedBrowser) {
		this.hasLiedBrowser = hasLiedBrowser;
	}

	public String getPublicKey() {
		return publicKey;
	}

	public void setPublicKey(String publicKey) {
		this.publicKey = publicKey;
	}

	public String getTrustLevel() {
		return trustLevel;
	}

	public void setTrustLevel(String trustLevel) {
		this.trustLevel = trustLevel;
	}

	public String getRegistrationType() {
		return registrationType;
	}

	public void setRegistrationType(String registrationType) {
		this.registrationType = registrationType;
	}

	public boolean isTrusted() {
		return trusted;
	}

	public void setTrusted(boolean trusted) {
		this.trusted = trusted;
	}

	public Map<String, Object> getDeviceInfo() {
		return deviceInfo;
	}

	public void setDeviceInfo(Map<String, Object> deviceInfo) {
		this.deviceInfo = deviceInfo;
	}

	public Instant getTrustExpiresAt() {
		return trustExpiresAt;
	}

	public void setTrustExpiresAt(Instant trustExpiresAt) {
		this.trustExpiresAt = trustExpiresAt;
	}

	public Instant getLastTrustVerification() {
		return lastTrustVerification;
	}

	public void setLastTrustVerification(Instant lastTrustVerification) {
		this.lastTrustVerification = lastTrustVerification;
	}

	public Boolean getCryptoVerified() {
		return cryptoVerified;
	}

	public void setCryptoVerified(Boolean cryptoVerified) {
		this.cryptoVerified = cryptoVerified;
	}

	public String getBaselineFingerprint() {
		return baselineFingerprint;
	}

	public void setBaselineFingerprint(String baselineFingerprint) {
		this.baselineFingerprint = baselineFingerprint;
	}

	@Override
	public String toString() {
		return "DeviceIdentity{" + "id='" + id + '\'' + ", deviceId='" + deviceId + '\'' + ", deviceName='" + deviceName
				+ '\'' + ", userId='" + userId + '\'' + ", browserName='" + browserName + '\'' + ", osName='" + osName
				+ '\'' + ", trustScore=" + getTrustScore() + ", trusted=" + trusted + '}';
	}

}