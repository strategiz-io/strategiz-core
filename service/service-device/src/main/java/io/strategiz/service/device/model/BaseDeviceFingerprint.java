package io.strategiz.service.device.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Base class for device fingerprint data from FingerprintJS and Web Crypto API
 * Shared by both anonymous and authenticated device requests
 * Captures comprehensive fingerprinting data for maximum device identification accuracy
 */
public class BaseDeviceFingerprint {
    
    // FingerprintJS Core Data
    @JsonProperty("visitor_id")
    private String visitorId;
    
    @JsonProperty("confidence")
    private Double confidence;
    
    // Web Crypto API
    @JsonProperty("public_key")
    private String publicKey;  // Base64 encoded public key
    
    // Browser Information
    @JsonProperty("user_agent")
    private String userAgent;
    
    @JsonProperty("browser_name")
    private String browserName;
    
    @JsonProperty("browser_version")
    private String browserVersion;
    
    @JsonProperty("browser_vendor")
    private String browserVendor;
    
    @JsonProperty("cookies_enabled")
    private Boolean cookiesEnabled;
    
    @JsonProperty("do_not_track")
    private String doNotTrack;
    
    // Operating System
    @JsonProperty("os_name")
    private String osName;
    
    @JsonProperty("os_version")
    private String osVersion;
    
    @JsonProperty("platform")
    private String platform;
    
    @JsonProperty("architecture")
    private String architecture;
    
    // Hardware - Enhanced
    @JsonProperty("screen_resolution")
    private String screenResolution;
    
    @JsonProperty("available_screen_resolution")
    private String availableScreenResolution;
    
    @JsonProperty("color_depth")
    private Integer colorDepth;
    
    @JsonProperty("pixel_ratio")
    private Double pixelRatio;
    
    @JsonProperty("hardware_concurrency")
    private Integer hardwareConcurrency;
    
    @JsonProperty("device_memory")
    private Integer deviceMemory;
    
    @JsonProperty("max_touch_points")
    private Integer maxTouchPoints;
    
    // Rendering Fingerprints
    @JsonProperty("canvas_fingerprint")
    private String canvasFingerprint;
    
    @JsonProperty("webgl_vendor")
    private String webglVendor;
    
    @JsonProperty("webgl_renderer")
    private String webglRenderer;
    
    @JsonProperty("webgl_unmasked_vendor")
    private String webglUnmaskedVendor;
    
    @JsonProperty("webgl_unmasked_renderer")
    private String webglUnmaskedRenderer;
    
    @JsonProperty("fonts")
    private List<String> fonts;
    
    // Audio Fingerprint
    @JsonProperty("audio_fingerprint")
    private String audioFingerprint;
    
    // Network & Location
    @JsonProperty("timezone")
    private String timezone;
    
    @JsonProperty("timezone_offset")
    private Integer timezoneOffset;
    
    @JsonProperty("language")
    private String language;
    
    @JsonProperty("languages")
    private List<String> languages;
    
    @JsonProperty("ip_address")
    private String ipAddress;
    
    @JsonProperty("ip_location")
    private String ipLocation;
    
    // Media Capabilities
    @JsonProperty("audio_codecs")
    private List<String> audioCodecs;
    
    @JsonProperty("video_codecs")
    private List<String> videoCodecs;
    
    @JsonProperty("speaker_count")
    private Integer speakerCount;
    
    @JsonProperty("microphone_count")
    private Integer microphoneCount;
    
    @JsonProperty("webcam_count")
    private Integer webcamCount;
    
    // Browser Features
    @JsonProperty("has_local_storage")
    private Boolean hasLocalStorage;
    
    @JsonProperty("has_session_storage")
    private Boolean hasSessionStorage;
    
    @JsonProperty("has_indexed_db")
    private Boolean hasIndexedDB;
    
    @JsonProperty("has_web_rtc")
    private Boolean hasWebRTC;
    
    @JsonProperty("has_web_assembly")
    private Boolean hasWebAssembly;
    
    @JsonProperty("has_service_worker")
    private Boolean hasServiceWorker;
    
    @JsonProperty("has_push_notifications")
    private Boolean hasPushNotifications;
    
    // Trust Indicators
    @JsonProperty("incognito_mode")
    private Boolean incognitoMode;
    
    @JsonProperty("ad_block_enabled")
    private Boolean adBlockEnabled;
    
    @JsonProperty("vpn_detected")
    private Boolean vpnDetected;
    
    @JsonProperty("proxy_detected")
    private Boolean proxyDetected;
    
    @JsonProperty("bot_detected")
    private Boolean botDetected;
    
    @JsonProperty("tampering_detected")
    private Boolean tamperingDetected;
    
    // Lie Detection
    @JsonProperty("has_lied_languages")
    private Boolean hasLiedLanguages;
    
    @JsonProperty("has_lied_resolution")
    private Boolean hasLiedResolution;
    
    @JsonProperty("has_lied_os")
    private Boolean hasLiedOs;
    
    @JsonProperty("has_lied_browser")
    private Boolean hasLiedBrowser;
    
    // Additional Metadata
    @JsonProperty("device_info")
    private Map<String, Object> deviceInfo;
    
    // Getters and Setters
    
    public String getVisitorId() {
        return visitorId;
    }
    
    public void setVisitorId(String visitorId) {
        this.visitorId = visitorId;
    }
    
    public Double getConfidence() {
        return confidence;
    }
    
    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }
    
    public String getPublicKey() {
        return publicKey;
    }
    
    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }
    
    public String getUserAgent() {
        return userAgent;
    }
    
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
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
    
    public String getScreenResolution() {
        return screenResolution;
    }
    
    public void setScreenResolution(String screenResolution) {
        this.screenResolution = screenResolution;
    }
    
    public Integer getHardwareConcurrency() {
        return hardwareConcurrency;
    }
    
    public void setHardwareConcurrency(Integer hardwareConcurrency) {
        this.hardwareConcurrency = hardwareConcurrency;
    }
    
    public String getCanvasFingerprint() {
        return canvasFingerprint;
    }
    
    public void setCanvasFingerprint(String canvasFingerprint) {
        this.canvasFingerprint = canvasFingerprint;
    }
    
    public String getTimezone() {
        return timezone;
    }
    
    public void setTimezone(String timezone) {
        this.timezone = timezone;
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
    
    public String getBrowserVendor() {
        return browserVendor;
    }
    
    public void setBrowserVendor(String browserVendor) {
        this.browserVendor = browserVendor;
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
    
    public String getArchitecture() {
        return architecture;
    }
    
    public void setArchitecture(String architecture) {
        this.architecture = architecture;
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
    
    public Map<String, Object> getDeviceInfo() {
        return deviceInfo;
    }
    
    public void setDeviceInfo(Map<String, Object> deviceInfo) {
        this.deviceInfo = deviceInfo;
    }
}