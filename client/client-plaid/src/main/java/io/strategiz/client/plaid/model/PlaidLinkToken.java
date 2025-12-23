package io.strategiz.client.plaid.model;

/**
 * Response from creating a Plaid Link token.
 */
public class PlaidLinkToken {

    private final String linkToken;
    private final String expiration;
    private final String requestId;

    public PlaidLinkToken(String linkToken, String expiration, String requestId) {
        this.linkToken = linkToken;
        this.expiration = expiration;
        this.requestId = requestId;
    }

    public String getLinkToken() {
        return linkToken;
    }

    public String getExpiration() {
        return expiration;
    }

    public String getRequestId() {
        return requestId;
    }
}
