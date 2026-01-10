package io.strategiz.framework.authorization.context;

import io.strategiz.framework.authorization.error.AuthorizationErrorDetails;
import io.strategiz.framework.exception.StrategizException;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Immutable representation of an authenticated user, extracted from a PASETO token. This class
 * provides convenience methods for checking permissions and authentication levels.
 */
public final class AuthenticatedUser {

  private final String userId;
  private final Set<String> scopes;
  private final String acr;
  private final List<Integer> amr;
  private final boolean demoMode;
  private final Instant authTime;
  private final Instant tokenExpiry;

  private AuthenticatedUser(Builder builder) {
    this.userId = builder.userId;
    this.scopes = builder.scopes != null ? Set.copyOf(builder.scopes) : Set.of();
    this.acr = builder.acr != null ? builder.acr : "0";
    this.amr = builder.amr != null ? List.copyOf(builder.amr) : List.of();
    this.demoMode = builder.demoMode;
    this.authTime = builder.authTime;
    this.tokenExpiry = builder.tokenExpiry;
  }

  public static Builder builder() {
    return new Builder();
  }

  // Getters

  public String getUserId() {
    return userId;
  }

  public Set<String> getScopes() {
    return scopes;
  }

  public String getAcr() {
    return acr;
  }

  public List<Integer> getAmr() {
    return amr;
  }

  public boolean isDemoMode() {
    return demoMode;
  }

  public Instant getAuthTime() {
    return authTime;
  }

  public Instant getTokenExpiry() {
    return tokenExpiry;
  }

  // Convenience methods

  /**
   * Check if the user has a specific scope. Also returns true if user has wildcard scope (*).
   *
   * @param scope the scope to check
   * @return true if the user has the scope
   */
  public boolean hasScope(String scope) {
    if (scopes.contains("*")) {
      return true;
    }
    return scopes.contains(scope);
  }

  /**
   * Check if the user has any of the specified scopes.
   *
   * @param requiredScopes the scopes to check
   * @return true if the user has at least one of the scopes
   */
  public boolean hasAnyScope(String... requiredScopes) {
    if (scopes.contains("*")) {
      return true;
    }
    for (String scope : requiredScopes) {
      if (scopes.contains(scope)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Check if the user has all of the specified scopes.
   *
   * @param requiredScopes the scopes to check
   * @return true if the user has all of the scopes
   */
  public boolean hasAllScopes(String... requiredScopes) {
    if (scopes.contains("*")) {
      return true;
    }
    for (String scope : requiredScopes) {
      if (!scopes.contains(scope)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Check if the user meets a minimum ACR level. ACR levels: "0" (none) < "1" (single-factor) < "2"
   * (MFA) < "3" (strong MFA)
   *
   * @param minAcr the minimum ACR level required
   * @return true if the user's ACR meets or exceeds the minimum
   */
  public boolean meetsMinAcr(String minAcr) {
    try {
      int userAcr = Integer.parseInt(this.acr);
      int requiredAcr = Integer.parseInt(minAcr);
      return userAcr >= requiredAcr;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  /**
   * Check if the user authenticated with multi-factor authentication (ACR >= 2).
   *
   * @return true if MFA was used
   */
  public boolean isMultiFactor() {
    return meetsMinAcr("2");
  }

  /**
   * Check if the token has expired.
   *
   * @return true if the token is expired
   */
  public boolean isExpired() {
    if (tokenExpiry == null) {
      return true;
    }
    return Instant.now().isAfter(tokenExpiry);
  }

  @Override
  public String toString() {
    return "AuthenticatedUser{"
        + "userId='"
        + userId
        + '\''
        + ", scopes="
        + scopes
        + ", acr='"
        + acr
        + '\''
        + ", demoMode="
        + demoMode
        + '}';
  }

  public static final class Builder {
    private String userId;
    private Set<String> scopes;
    private String acr;
    private List<Integer> amr;
    private boolean demoMode = true;
    private Instant authTime;
    private Instant tokenExpiry;

    private Builder() {}

    public Builder userId(String userId) {
      this.userId = userId;
      return this;
    }

    public Builder scopes(Set<String> scopes) {
      this.scopes = scopes;
      return this;
    }

    public Builder acr(String acr) {
      this.acr = acr;
      return this;
    }

    public Builder amr(List<Integer> amr) {
      this.amr = amr;
      return this;
    }

    public Builder demoMode(boolean demoMode) {
      this.demoMode = demoMode;
      return this;
    }

    public Builder authTime(Instant authTime) {
      this.authTime = authTime;
      return this;
    }

    public Builder tokenExpiry(Instant tokenExpiry) {
      this.tokenExpiry = tokenExpiry;
      return this;
    }

    public AuthenticatedUser build() {
      if (userId == null || userId.isBlank()) {
        throw new StrategizException(
            AuthorizationErrorDetails.INVALID_ARGUMENT, "AuthenticatedUser", "userId is required");
      }
      return new AuthenticatedUser(this);
    }
  }
}
