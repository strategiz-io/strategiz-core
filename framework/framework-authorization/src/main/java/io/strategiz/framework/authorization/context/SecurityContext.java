package io.strategiz.framework.authorization.context;

import java.util.Optional;

/**
 * Security context holding the authenticated user for the current request. This context is
 * thread-local and managed by {@link SecurityContextHolder}.
 */
public final class SecurityContext {

  private AuthenticatedUser authenticatedUser;

  public SecurityContext() {
    this.authenticatedUser = null;
  }

  /**
   * Get the authenticated user, if present.
   *
   * @return Optional containing the authenticated user
   */
  public Optional<AuthenticatedUser> getAuthenticatedUser() {
    return Optional.ofNullable(authenticatedUser);
  }

  /**
   * Set the authenticated user for this context.
   *
   * @param authenticatedUser the authenticated user
   */
  public void setAuthenticatedUser(AuthenticatedUser authenticatedUser) {
    this.authenticatedUser = authenticatedUser;
  }

  /**
   * Check if a user is authenticated.
   *
   * @return true if an authenticated user is present
   */
  public boolean isAuthenticated() {
    return authenticatedUser != null;
  }

  /** Clear the authentication from this context. */
  public void clearAuthentication() {
    this.authenticatedUser = null;
  }
}
