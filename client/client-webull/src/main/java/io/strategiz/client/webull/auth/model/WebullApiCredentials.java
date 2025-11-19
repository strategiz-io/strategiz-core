package io.strategiz.client.webull.auth.model;

/**
 * Webull API credentials model
 *
 * Webull uses App Key and App Secret for authentication.
 * Account ID is required for account-specific operations.
 */
public class WebullApiCredentials {

    private String appKey;
    private String appSecret;
    private String accountId;
    private String userId;

    public WebullApiCredentials() {
    }

    public WebullApiCredentials(String appKey, String appSecret) {
        this.appKey = appKey;
        this.appSecret = appSecret;
    }

    public WebullApiCredentials(String appKey, String appSecret, String accountId) {
        this.appKey = appKey;
        this.appSecret = appSecret;
        this.accountId = accountId;
    }

    public WebullApiCredentials(String appKey, String appSecret, String accountId, String userId) {
        this.appKey = appKey;
        this.appSecret = appSecret;
        this.accountId = accountId;
        this.userId = userId;
    }

    public String getAppKey() {
        return appKey;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public String toString() {
        return "WebullApiCredentials{" +
                "appKey='" + (appKey != null ? appKey.substring(0, Math.min(appKey.length(), 4)) + "****" : null) + '\'' +
                ", hasAccountId=" + (accountId != null && !accountId.isEmpty()) +
                ", userId='" + userId + '\'' +
                '}';
    }
}
