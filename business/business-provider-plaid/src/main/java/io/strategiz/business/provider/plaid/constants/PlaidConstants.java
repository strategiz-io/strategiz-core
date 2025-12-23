package io.strategiz.business.provider.plaid.constants;

/**
 * Constants for Plaid provider integration.
 */
public final class PlaidConstants {

    private PlaidConstants() {
        // Utility class
    }

    public static final String PROVIDER_ID = "plaid";
    public static final String PROVIDER_NAME = "Plaid";
    public static final String PROVIDER_TYPE = "aggregator";
    public static final String PROVIDER_CATEGORY = "aggregator";

    // Plaid products we use
    public static final String PRODUCT_INVESTMENTS = "investments";
    public static final String PRODUCT_TRANSACTIONS = "transactions";

    // Country codes we support
    public static final String COUNTRY_US = "US";

    // Vault path for Plaid access tokens
    public static final String VAULT_PATH_PREFIX = "strategiz/plaid/users/";

    // Feature flag key
    public static final String FEATURE_FLAG_KEY = "plaid_enabled";
}
