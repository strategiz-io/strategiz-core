package io.strategiz.client.plaid.model;

/**
 * Response from exchanging a public token for an access token.
 */
public class PlaidAccessToken {

    private final String accessToken;
    private final String itemId;
    private final String requestId;

    public PlaidAccessToken(String accessToken, String itemId, String requestId) {
        this.accessToken = accessToken;
        this.itemId = itemId;
        this.requestId = requestId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getItemId() {
        return itemId;
    }

    public String getRequestId() {
        return requestId;
    }
}
