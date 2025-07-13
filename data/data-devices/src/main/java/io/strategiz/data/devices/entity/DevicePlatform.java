package io.strategiz.data.devices.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Platform information for user devices
 */
public class DevicePlatform {

    @JsonProperty("userAgent")
    private String userAgent;

    @JsonProperty("platform")
    private String platform; // iOS, Android, Windows, macOS, Linux

    @JsonProperty("type")
    private String type; // mobile, desktop, tablet

    @JsonProperty("brand")
    private String brand; // Apple, Samsung, Google, Microsoft, etc.

    @JsonProperty("model")
    private String model; // iPhone 15 Pro, Galaxy S24, etc.

    @JsonProperty("version")
    private String version; // OS version

    @JsonProperty("browser")
    private String browser; // Safari, Chrome, Firefox, etc.

    @JsonProperty("browserVersion")
    private String browserVersion;

    // Constructors
    public DevicePlatform() {
    }

    public DevicePlatform(String userAgent, String platform, String type) {
        this.userAgent = userAgent;
        this.platform = platform;
        this.type = type;
    }

    // Getters and Setters
    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getBrowser() {
        return browser;
    }

    public void setBrowser(String browser) {
        this.browser = browser;
    }

    public String getBrowserVersion() {
        return browserVersion;
    }

    public void setBrowserVersion(String browserVersion) {
        this.browserVersion = browserVersion;
    }

    // Convenience methods
    public boolean isMobile() {
        return "mobile".equalsIgnoreCase(type);
    }

    public boolean isDesktop() {
        return "desktop".equalsIgnoreCase(type);
    }

    public boolean isTablet() {
        return "tablet".equalsIgnoreCase(type);
    }

    @Override
    public String toString() {
        return "DevicePlatform{" +
                "platform='" + platform + '\'' +
                ", type='" + type + '\'' +
                ", brand='" + brand + '\'' +
                ", model='" + model + '\'' +
                ", version='" + version + '\'' +
                '}';
    }
}