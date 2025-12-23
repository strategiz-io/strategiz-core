package io.strategiz.client.plaid;

import com.plaid.client.model.*;
import com.plaid.client.request.PlaidApi;
import io.strategiz.client.plaid.config.PlaidConfig;
import io.strategiz.client.plaid.model.PlaidLinkToken;
import io.strategiz.client.plaid.model.PlaidAccessToken;
import io.strategiz.client.plaid.model.PlaidInvestmentHoldings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import retrofit2.Response;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Client for interacting with Plaid API.
 * Handles Link token creation, public token exchange, and data retrieval.
 */
@Component
public class PlaidClient {

    private static final Logger log = LoggerFactory.getLogger(PlaidClient.class);

    private final PlaidApi plaidApi;
    private final PlaidConfig plaidConfig;

    @Autowired
    public PlaidClient(PlaidApi plaidApi, PlaidConfig plaidConfig) {
        this.plaidApi = plaidApi;
        this.plaidConfig = plaidConfig;
        log.info("PlaidClient initialized");
    }

    /**
     * Create a Link token for initializing Plaid Link on the frontend.
     * This token is used to open the Plaid Link modal.
     *
     * @param userId Unique user identifier
     * @param redirectUri OAuth redirect URI (optional, for OAuth flows)
     * @return Link token response
     */
    public PlaidLinkToken createLinkToken(String userId, String redirectUri) throws IOException {
        log.info("Creating Plaid Link token for user: {}", userId);

        LinkTokenCreateRequestUser user = new LinkTokenCreateRequestUser()
            .clientUserId(userId);

        LinkTokenCreateRequest request = new LinkTokenCreateRequest()
            .user(user)
            .clientName("Strategiz")
            .products(Arrays.asList(Products.INVESTMENTS))
            .countryCodes(Arrays.asList(CountryCode.US))
            .language("en");

        // Add redirect URI for OAuth institutions (Schwab, Fidelity, etc.)
        if (redirectUri != null && !redirectUri.isEmpty()) {
            request.redirectUri(redirectUri);
        }

        Response<LinkTokenCreateResponse> response = plaidApi.linkTokenCreate(request).execute();

        if (!response.isSuccessful()) {
            String errorBody = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
            log.error("Failed to create Link token: {}", errorBody);
            throw new IOException("Failed to create Link token: " + errorBody);
        }

        LinkTokenCreateResponse body = response.body();
        log.info("Link token created successfully, expires at: {}", body.getExpiration());

        return new PlaidLinkToken(
            body.getLinkToken(),
            body.getExpiration() != null ? body.getExpiration().toString() : null,
            body.getRequestId()
        );
    }

    /**
     * Exchange a public token (from Link) for an access token.
     * The access token is used for subsequent API calls.
     *
     * @param publicToken Public token received from Plaid Link
     * @return Access token response
     */
    public PlaidAccessToken exchangePublicToken(String publicToken) throws IOException {
        log.info("Exchanging public token for access token");

        ItemPublicTokenExchangeRequest request = new ItemPublicTokenExchangeRequest()
            .publicToken(publicToken);

        Response<ItemPublicTokenExchangeResponse> response = plaidApi.itemPublicTokenExchange(request).execute();

        if (!response.isSuccessful()) {
            String errorBody = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
            log.error("Failed to exchange public token: {}", errorBody);
            throw new IOException("Failed to exchange public token: " + errorBody);
        }

        ItemPublicTokenExchangeResponse body = response.body();
        log.info("Public token exchanged successfully, item ID: {}", body.getItemId());

        return new PlaidAccessToken(
            body.getAccessToken(),
            body.getItemId(),
            body.getRequestId()
        );
    }

    /**
     * Get investment holdings for an account.
     *
     * @param accessToken Access token for the item
     * @return Investment holdings
     */
    public PlaidInvestmentHoldings getInvestmentHoldings(String accessToken) throws IOException {
        log.info("Fetching investment holdings");

        InvestmentsHoldingsGetRequest request = new InvestmentsHoldingsGetRequest()
            .accessToken(accessToken);

        Response<InvestmentsHoldingsGetResponse> response = plaidApi.investmentsHoldingsGet(request).execute();

        if (!response.isSuccessful()) {
            String errorBody = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
            log.error("Failed to get investment holdings: {}", errorBody);
            throw new IOException("Failed to get investment holdings: " + errorBody);
        }

        InvestmentsHoldingsGetResponse body = response.body();
        log.info("Retrieved {} holdings across {} accounts",
            body.getHoldings().size(),
            body.getAccounts().size());

        return new PlaidInvestmentHoldings(
            body.getAccounts(),
            body.getHoldings(),
            body.getSecurities(),
            body.getItem(),
            body.getRequestId()
        );
    }

    /**
     * Get investment transactions for an account.
     *
     * @param accessToken Access token for the item
     * @param startDate Start date (YYYY-MM-DD)
     * @param endDate End date (YYYY-MM-DD)
     * @return Investment transactions
     */
    public InvestmentsTransactionsGetResponse getInvestmentTransactions(
            String accessToken, String startDate, String endDate) throws IOException {
        log.info("Fetching investment transactions from {} to {}", startDate, endDate);

        InvestmentsTransactionsGetRequest request = new InvestmentsTransactionsGetRequest()
            .accessToken(accessToken)
            .startDate(java.time.LocalDate.parse(startDate))
            .endDate(java.time.LocalDate.parse(endDate));

        Response<InvestmentsTransactionsGetResponse> response = plaidApi.investmentsTransactionsGet(request).execute();

        if (!response.isSuccessful()) {
            String errorBody = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
            log.error("Failed to get investment transactions: {}", errorBody);
            throw new IOException("Failed to get investment transactions: " + errorBody);
        }

        InvestmentsTransactionsGetResponse body = response.body();
        log.info("Retrieved {} investment transactions", body.getInvestmentTransactions().size());

        return body;
    }

    /**
     * Get item information (institution details, status).
     *
     * @param accessToken Access token for the item
     * @return Item information
     */
    public ItemGetResponse getItem(String accessToken) throws IOException {
        log.info("Fetching item information");

        ItemGetRequest request = new ItemGetRequest()
            .accessToken(accessToken);

        Response<ItemGetResponse> response = plaidApi.itemGet(request).execute();

        if (!response.isSuccessful()) {
            String errorBody = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
            log.error("Failed to get item: {}", errorBody);
            throw new IOException("Failed to get item: " + errorBody);
        }

        return response.body();
    }

    /**
     * Get institution details by ID.
     *
     * @param institutionId Plaid institution ID
     * @return Institution details
     */
    public InstitutionsGetByIdResponse getInstitution(String institutionId) throws IOException {
        log.info("Fetching institution: {}", institutionId);

        InstitutionsGetByIdRequest request = new InstitutionsGetByIdRequest()
            .institutionId(institutionId)
            .countryCodes(Arrays.asList(CountryCode.US));

        Response<InstitutionsGetByIdResponse> response = plaidApi.institutionsGetById(request).execute();

        if (!response.isSuccessful()) {
            String errorBody = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
            log.error("Failed to get institution: {}", errorBody);
            throw new IOException("Failed to get institution: " + errorBody);
        }

        return response.body();
    }

    /**
     * Remove an item (disconnect the account).
     *
     * @param accessToken Access token for the item
     */
    public void removeItem(String accessToken) throws IOException {
        log.info("Removing Plaid item");

        ItemRemoveRequest request = new ItemRemoveRequest()
            .accessToken(accessToken);

        Response<ItemRemoveResponse> response = plaidApi.itemRemove(request).execute();

        if (!response.isSuccessful()) {
            String errorBody = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
            log.error("Failed to remove item: {}", errorBody);
            throw new IOException("Failed to remove item: " + errorBody);
        }

        log.info("Item removed successfully");
    }

    /**
     * Create a new Link token for updating an existing item (re-authentication).
     *
     * @param userId User ID
     * @param accessToken Existing access token
     * @return Link token for update mode
     */
    public PlaidLinkToken createUpdateLinkToken(String userId, String accessToken) throws IOException {
        log.info("Creating update Link token for user: {}", userId);

        LinkTokenCreateRequestUser user = new LinkTokenCreateRequestUser()
            .clientUserId(userId);

        LinkTokenCreateRequest request = new LinkTokenCreateRequest()
            .user(user)
            .clientName("Strategiz")
            .countryCodes(Arrays.asList(CountryCode.US))
            .language("en")
            .accessToken(accessToken); // This puts Link in update mode

        Response<LinkTokenCreateResponse> response = plaidApi.linkTokenCreate(request).execute();

        if (!response.isSuccessful()) {
            String errorBody = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
            log.error("Failed to create update Link token: {}", errorBody);
            throw new IOException("Failed to create update Link token: " + errorBody);
        }

        LinkTokenCreateResponse body = response.body();
        return new PlaidLinkToken(
            body.getLinkToken(),
            body.getExpiration() != null ? body.getExpiration().toString() : null,
            body.getRequestId()
        );
    }
}
