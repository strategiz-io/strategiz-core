package io.strategiz.framework.authorization.model;

/**
 * Enum representing resource types for fine-grained authorization.
 * Used with {@link io.strategiz.framework.authorization.annotation.Authorize} annotation.
 */
public enum ResourceType {

    /**
     * User portfolio resource
     */
    PORTFOLIO("portfolio"),

    /**
     * Trading strategy resource
     */
    STRATEGY("strategy"),

    /**
     * Watchlist resource
     */
    WATCHLIST("watchlist"),

    /**
     * Provider integration (Coinbase, Kraken, etc.)
     */
    PROVIDER_INTEGRATION("provider_integration"),

    /**
     * Alert/notification resource
     */
    ALERT("alert"),

    /**
     * User profile resource
     */
    PROFILE("profile");

    private final String typeName;

    ResourceType(String typeName) {
        this.typeName = typeName;
    }

    /**
     * Get the string representation for use in FGA tuples.
     *
     * @return the type name
     */
    public String getTypeName() {
        return typeName;
    }

    @Override
    public String toString() {
        return typeName;
    }
}
