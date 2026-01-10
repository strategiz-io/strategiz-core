package io.strategiz.framework.authorization.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to require specific scopes from the user's PASETO token.
 *
 * <p>This provides Layer 1 authorization - fast, local scope checking from the token. No external
 * calls are made.
 *
 * <p>Usage examples:
 *
 * <pre>
 * // Require a single scope
 * &#64;RequireScope("portfolio:read")
 * public ResponseEntity&lt;?&gt; getPortfolio() { ... }
 *
 * // Require any of multiple scopes
 * &#64;RequireScope(value = {"portfolio:read", "portfolio:write"}, mode = ScopeMode.ANY)
 * public ResponseEntity&lt;?&gt; viewOrEdit() { ... }
 *
 * // Require all scopes
 * &#64;RequireScope(value = {"provider:read", "provider:write"}, mode = ScopeMode.ALL)
 * public ResponseEntity&lt;?&gt; manageProvider() { ... }
 * </pre>
 *
 * @see io.strategiz.framework.authorization.aspect.ScopeAuthorizationAspect
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireScope {

  /**
   * The scope(s) required for access.
   *
   * @return array of required scopes
   */
  String[] value();

  /**
   * How to evaluate multiple scopes.
   *
   * <ul>
   *   <li>{@link ScopeMode#ANY} - User must have at least one scope (default)
   *   <li>{@link ScopeMode#ALL} - User must have all scopes
   * </ul>
   *
   * @return the scope mode
   */
  ScopeMode mode() default ScopeMode.ANY;
}
