package io.strategiz.framework.authorization.context;

import io.strategiz.framework.authorization.error.AuthorizationErrorDetails;
import io.strategiz.framework.exception.StrategizException;
import java.util.Optional;

/**
 * Thread-local holder for the security context. Provides static methods to access the current
 * authenticated user.
 *
 * <p>Usage example:
 *
 * <pre>
 * // Get current user (throws if not authenticated)
 * AuthenticatedUser user = SecurityContextHolder.requireAuthenticatedUser();
 *
 * // Get current user ID (throws if not authenticated)
 * String userId = SecurityContextHolder.requireUserId();
 *
 * // Check if authenticated (doesn't throw)
 * Optional&lt;AuthenticatedUser&gt; maybeUser = SecurityContextHolder.getAuthenticatedUser();
 * </pre>
 */
public final class SecurityContextHolder {

  private static final String MODULE_NAME = "authorization";

  private static final ThreadLocal<SecurityContext> contextHolder =
      new InheritableThreadLocal<SecurityContext>() {
        @Override
        protected SecurityContext initialValue() {
          return new SecurityContext();
        }
      };

  private SecurityContextHolder() {
    // Static utility class
  }

  /**
   * Get the current security context.
   *
   * @return the security context (never null)
   */
  public static SecurityContext getContext() {
    SecurityContext context = contextHolder.get();
    if (context == null) {
      context = new SecurityContext();
      contextHolder.set(context);
    }
    return context;
  }

  /**
   * Set the security context for the current thread.
   *
   * @param context the security context
   */
  public static void setContext(SecurityContext context) {
    if (context == null) {
      contextHolder.remove();
    } else {
      contextHolder.set(context);
    }
  }

  /**
   * Clear the security context for the current thread. Should be called at the end of request
   * processing.
   */
  public static void clearContext() {
    contextHolder.remove();
  }

  /**
   * Get the authenticated user if present.
   *
   * @return Optional containing the authenticated user
   */
  public static Optional<AuthenticatedUser> getAuthenticatedUser() {
    return getContext().getAuthenticatedUser();
  }

  /**
   * Get the authenticated user, throwing if not authenticated.
   *
   * @return the authenticated user
   * @throws StrategizException with NOT_AUTHENTICATED if no user is authenticated
   */
  public static AuthenticatedUser requireAuthenticatedUser() {
    return getAuthenticatedUser()
        .orElseThrow(
            () -> new StrategizException(AuthorizationErrorDetails.NOT_AUTHENTICATED, MODULE_NAME));
  }

  /**
   * Get the current user ID, throwing if not authenticated.
   *
   * @return the user ID
   * @throws StrategizException with NOT_AUTHENTICATED if no user is authenticated
   */
  public static String requireUserId() {
    return requireAuthenticatedUser().getUserId();
  }

  /**
   * Check if a user is currently authenticated.
   *
   * @return true if authenticated
   */
  public static boolean isAuthenticated() {
    return getContext().isAuthenticated();
  }
}
