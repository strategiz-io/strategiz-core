package io.strategiz.service.device.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Device registration request with comprehensive fingerprinting data
 * Combines FingerprintJS data with Web Crypto keys for secure device identification
 */
public class DeviceRequest {
    
    private String deviceId;
    private DeviceFingerprint fingerprint;
    private BrowserInfo browser;
    private OperatingSystem os;
    private HardwareInfo hardware;
    private RenderingInfo rendering;
    private NetworkInfo network;
    private MediaCapabilities media;
    private BrowserFeatures features;
    private TrustIndicators trust;
    private DeviceMetadata metadata;
    private String publicKey;
    
    /**
     * Device fingerprint information from FingerprintJS
     */
    public static class DeviceFingerprint {
        private String visitorId;
        private Double confidence;
        private String requestId;
        private Map<String, Object> components;
        
        // Getters and setters
        public String getVisitorId() { return visitorId; }
        public void setVisitorId(String visitorId) { this.visitorId = visitorId; }
        
        public Double getConfidence() { return confidence; }
        public void setConfidence(Double confidence) { this.confidence = confidence; }
        
        public String getRequestId() { return requestId; }
        public void setRequestId(String requestId) { this.requestId = requestId; }
        
        public Map<String, Object> getComponents() { return components; }
        public void setComponents(Map<String, Object> components) { this.components = components; }
    }
    
    /**
     * Browser information
     */
    public static class BrowserInfo {
        private String name;
        private String version;
        private String vendor;
        private String userAgent;
        private Boolean cookiesEnabled;
        private String doNotTrack;
        private String language;
        private List<String> languages;
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        
        public String getVendor() { return vendor; }
        public void setVendor(String vendor) { this.vendor = vendor; }
        
        public String getUserAgent() { return userAgent; }
        public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
        
        public Boolean getCookiesEnabled() { return cookiesEnabled; }
        public void setCookiesEnabled(Boolean cookiesEnabled) { this.cookiesEnabled = cookiesEnabled; }
        
        public String getDoNotTrack() { return doNotTrack; }
        public void setDoNotTrack(String doNotTrack) { this.doNotTrack = doNotTrack; }
        
        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }
        
        public List<String> getLanguages() { return languages; }
        public void setLanguages(List<String> languages) { this.languages = languages; }
    }
    
    /**
     * Operating system information
     */
    public static class OperatingSystem {
        private String name;
        private String version;
        private String platform;
        private String architecture;
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        
        public String getPlatform() { return platform; }
        public void setPlatform(String platform) { this.platform = platform; }
        
        public String getArchitecture() { return architecture; }
        public void setArchitecture(String architecture) { this.architecture = architecture; }
    }
    
    /**
     * Hardware capabilities
     */
    public static class HardwareInfo {
        private String screenResolution;
        private String availableScreenResolution;
        private Integer colorDepth;
        private Double pixelRatio;
        private Integer hardwareConcurrency;
        private Integer deviceMemory;
        private Integer maxTouchPoints;
        
        // Getters and setters
        public String getScreenResolution() { return screenResolution; }
        public void setScreenResolution(String screenResolution) { this.screenResolution = screenResolution; }
        
        public String getAvailableScreenResolution() { return availableScreenResolution; }
        public void setAvailableScreenResolution(String availableScreenResolution) { 
            this.availableScreenResolution = availableScreenResolution; 
        }
        
        public Integer getColorDepth() { return colorDepth; }
        public void setColorDepth(Integer colorDepth) { this.colorDepth = colorDepth; }
        
        public Double getPixelRatio() { return pixelRatio; }
        public void setPixelRatio(Double pixelRatio) { this.pixelRatio = pixelRatio; }
        
        public Integer getHardwareConcurrency() { return hardwareConcurrency; }
        public void setHardwareConcurrency(Integer hardwareConcurrency) { 
            this.hardwareConcurrency = hardwareConcurrency; 
        }
        
        public Integer getDeviceMemory() { return deviceMemory; }
        public void setDeviceMemory(Integer deviceMemory) { this.deviceMemory = deviceMemory; }
        
        public Integer getMaxTouchPoints() { return maxTouchPoints; }
        public void setMaxTouchPoints(Integer maxTouchPoints) { this.maxTouchPoints = maxTouchPoints; }
    }
    
    /**
     * Rendering fingerprints (Canvas, WebGL, fonts)
     */
    public static class RenderingInfo {
        private String canvas;
        private WebGLInfo webgl;
        private List<String> fonts;
        
        public static class WebGLInfo {
            private String vendor;
            private String renderer;
            private String unmaskedVendor;
            private String unmaskedRenderer;
            private String shadingLanguageVersion;
            private List<String> extensions;
            
            // Getters and setters
            public String getVendor() { return vendor; }
            public void setVendor(String vendor) { this.vendor = vendor; }
            
            public String getRenderer() { return renderer; }
            public void setRenderer(String renderer) { this.renderer = renderer; }
            
            public String getUnmaskedVendor() { return unmaskedVendor; }
            public void setUnmaskedVendor(String unmaskedVendor) { this.unmaskedVendor = unmaskedVendor; }
            
            public String getUnmaskedRenderer() { return unmaskedRenderer; }
            public void setUnmaskedRenderer(String unmaskedRenderer) { this.unmaskedRenderer = unmaskedRenderer; }
            
            public String getShadingLanguageVersion() { return shadingLanguageVersion; }
            public void setShadingLanguageVersion(String version) { this.shadingLanguageVersion = version; }
            
            public List<String> getExtensions() { return extensions; }
            public void setExtensions(List<String> extensions) { this.extensions = extensions; }
        }
        
        // Getters and setters
        public String getCanvas() { return canvas; }
        public void setCanvas(String canvas) { this.canvas = canvas; }
        
        public WebGLInfo getWebgl() { return webgl; }
        public void setWebgl(WebGLInfo webgl) { this.webgl = webgl; }
        
        public List<String> getFonts() { return fonts; }
        public void setFonts(List<String> fonts) { this.fonts = fonts; }
    }
    
    /**
     * Network information
     */
    public static class NetworkInfo {
        private String ip;  // Server-side only
        private String timezone;
        private Integer timezoneOffset;
        private String ipLocation;  // Server-side only
        
        // Getters and setters
        public String getIp() { return ip; }
        public void setIp(String ip) { this.ip = ip; }
        
        public String getTimezone() { return timezone; }
        public void setTimezone(String timezone) { this.timezone = timezone; }
        
        public Integer getTimezoneOffset() { return timezoneOffset; }
        public void setTimezoneOffset(Integer timezoneOffset) { this.timezoneOffset = timezoneOffset; }
        
        public String getIpLocation() { return ipLocation; }
        public void setIpLocation(String ipLocation) { this.ipLocation = ipLocation; }
    }
    
    /**
     * Media capabilities
     */
    public static class MediaCapabilities {
        private String audioContext;
        private List<String> audioCodecs;
        private List<String> videoCodecs;
        private MediaDevices mediaDevices;
        
        public static class MediaDevices {
            private Integer speakers;
            private Integer microphones;
            private Integer webcams;
            
            // Getters and setters
            public Integer getSpeakers() { return speakers; }
            public void setSpeakers(Integer speakers) { this.speakers = speakers; }
            
            public Integer getMicrophones() { return microphones; }
            public void setMicrophones(Integer microphones) { this.microphones = microphones; }
            
            public Integer getWebcams() { return webcams; }
            public void setWebcams(Integer webcams) { this.webcams = webcams; }
        }
        
        // Getters and setters
        public String getAudioContext() { return audioContext; }
        public void setAudioContext(String audioContext) { this.audioContext = audioContext; }
        
        public List<String> getAudioCodecs() { return audioCodecs; }
        public void setAudioCodecs(List<String> audioCodecs) { this.audioCodecs = audioCodecs; }
        
        public List<String> getVideoCodecs() { return videoCodecs; }
        public void setVideoCodecs(List<String> videoCodecs) { this.videoCodecs = videoCodecs; }
        
        public MediaDevices getMediaDevices() { return mediaDevices; }
        public void setMediaDevices(MediaDevices mediaDevices) { this.mediaDevices = mediaDevices; }
    }
    
    /**
     * Browser features detection
     */
    public static class BrowserFeatures {
        private Boolean localStorage;
        private Boolean sessionStorage;
        private Boolean indexedDB;
        private Boolean webRTC;
        private Boolean webAssembly;
        private Boolean serviceWorker;
        private Boolean pushNotifications;
        
        // Getters and setters
        public Boolean getLocalStorage() { return localStorage; }
        public void setLocalStorage(Boolean localStorage) { this.localStorage = localStorage; }
        
        public Boolean getSessionStorage() { return sessionStorage; }
        public void setSessionStorage(Boolean sessionStorage) { this.sessionStorage = sessionStorage; }
        
        public Boolean getIndexedDB() { return indexedDB; }
        public void setIndexedDB(Boolean indexedDB) { this.indexedDB = indexedDB; }
        
        public Boolean getWebRTC() { return webRTC; }
        public void setWebRTC(Boolean webRTC) { this.webRTC = webRTC; }
        
        public Boolean getWebAssembly() { return webAssembly; }
        public void setWebAssembly(Boolean webAssembly) { this.webAssembly = webAssembly; }
        
        public Boolean getServiceWorker() { return serviceWorker; }
        public void setServiceWorker(Boolean serviceWorker) { this.serviceWorker = serviceWorker; }
        
        public Boolean getPushNotifications() { return pushNotifications; }
        public void setPushNotifications(Boolean pushNotifications) { this.pushNotifications = pushNotifications; }
    }
    
    /**
     * Trust indicators and security signals
     */
    public static class TrustIndicators {
        private Boolean incognito;
        private Boolean adBlock;
        private Boolean vpn;  // Server-side detection
        private Boolean proxy;  // Server-side detection
        private Boolean bot;  // Server-side detection
        private Boolean tampering;
        private Boolean hasLiedLanguages;
        private Boolean hasLiedResolution;
        private Boolean hasLiedOs;
        private Boolean hasLiedBrowser;
        
        // Getters and setters
        public Boolean getIncognito() { return incognito; }
        public void setIncognito(Boolean incognito) { this.incognito = incognito; }
        
        public Boolean getAdBlock() { return adBlock; }
        public void setAdBlock(Boolean adBlock) { this.adBlock = adBlock; }
        
        public Boolean getVpn() { return vpn; }
        public void setVpn(Boolean vpn) { this.vpn = vpn; }
        
        public Boolean getProxy() { return proxy; }
        public void setProxy(Boolean proxy) { this.proxy = proxy; }
        
        public Boolean getBot() { return bot; }
        public void setBot(Boolean bot) { this.bot = bot; }
        
        public Boolean getTampering() { return tampering; }
        public void setTampering(Boolean tampering) { this.tampering = tampering; }
        
        public Boolean getHasLiedLanguages() { return hasLiedLanguages; }
        public void setHasLiedLanguages(Boolean hasLiedLanguages) { this.hasLiedLanguages = hasLiedLanguages; }
        
        public Boolean getHasLiedResolution() { return hasLiedResolution; }
        public void setHasLiedResolution(Boolean hasLiedResolution) { this.hasLiedResolution = hasLiedResolution; }
        
        public Boolean getHasLiedOs() { return hasLiedOs; }
        public void setHasLiedOs(Boolean hasLiedOs) { this.hasLiedOs = hasLiedOs; }
        
        public Boolean getHasLiedBrowser() { return hasLiedBrowser; }
        public void setHasLiedBrowser(Boolean hasLiedBrowser) { this.hasLiedBrowser = hasLiedBrowser; }
    }
    
    /**
     * Device metadata
     */
    public static class DeviceMetadata {
        private String deviceName;
        private String trustLevel;
        private String registrationType;
        
        // Getters and setters
        public String getDeviceName() { return deviceName; }
        public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
        
        public String getTrustLevel() { return trustLevel; }
        public void setTrustLevel(String trustLevel) { this.trustLevel = trustLevel; }
        
        public String getRegistrationType() { return registrationType; }
        public void setRegistrationType(String registrationType) { this.registrationType = registrationType; }
    }
    
    // Main class getters and setters
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    
    public DeviceFingerprint getFingerprint() { return fingerprint; }
    public void setFingerprint(DeviceFingerprint fingerprint) { this.fingerprint = fingerprint; }
    
    public BrowserInfo getBrowser() { return browser; }
    public void setBrowser(BrowserInfo browser) { this.browser = browser; }
    
    public OperatingSystem getOs() { return os; }
    public void setOs(OperatingSystem os) { this.os = os; }
    
    public HardwareInfo getHardware() { return hardware; }
    public void setHardware(HardwareInfo hardware) { this.hardware = hardware; }
    
    public RenderingInfo getRendering() { return rendering; }
    public void setRendering(RenderingInfo rendering) { this.rendering = rendering; }
    
    public NetworkInfo getNetwork() { return network; }
    public void setNetwork(NetworkInfo network) { this.network = network; }
    
    public MediaCapabilities getMedia() { return media; }
    public void setMedia(MediaCapabilities media) { this.media = media; }
    
    public BrowserFeatures getFeatures() { return features; }
    public void setFeatures(BrowserFeatures features) { this.features = features; }
    
    public TrustIndicators getTrust() { return trust; }
    public void setTrust(TrustIndicators trust) { this.trust = trust; }
    
    public DeviceMetadata getMetadata() { return metadata; }
    public void setMetadata(DeviceMetadata metadata) { this.metadata = metadata; }
    
    public String getPublicKey() { return publicKey; }
    public void setPublicKey(String publicKey) { this.publicKey = publicKey; }
}