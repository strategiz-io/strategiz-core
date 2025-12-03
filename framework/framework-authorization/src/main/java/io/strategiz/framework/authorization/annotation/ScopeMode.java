package io.strategiz.framework.authorization.annotation;

/**
 * Mode for evaluating multiple scopes in {@link RequireScope}.
 */
public enum ScopeMode {
    /**
     * User must have at least one of the specified scopes.
     */
    ANY,

    /**
     * User must have all of the specified scopes.
     */
    ALL
}
