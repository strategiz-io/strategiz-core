package io.strategiz.framework.authorization.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to require authentication and optionally specify minimum authentication
 * level.
 *
 * <p>
 * When applied to a controller method or class, the request will be rejected with 401/403
 * if the user is not authenticated or doesn't meet the requirements.
 *
 * <p>
 * Usage examples:
 *
 * <pre>
 * // Require any authentication
 * &#64;RequireAuth
 * public ResponseEntity&lt;?&gt; getProfile() { ... }
 *
 * // Require MFA (ACR level 2+)
 * &#64;RequireAuth(minAcr = "2")
 * public ResponseEntity&lt;?&gt; transferFunds() { ... }
 *
 * // Require strong MFA and deny demo mode
 * &#64;RequireAuth(minAcr = "3", allowDemoMode = false)
 * public ResponseEntity&lt;?&gt; executeTrade() { ... }
 * </pre>
 *
 * @see io.strategiz.framework.authorization.aspect.AuthenticationAspect
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireAuth {

	/**
	 * Minimum Authentication Context Reference (ACR) level required.
	 *
	 * <ul>
	 * <li>"0" - No authentication (partial/signup)
	 * <li>"1" - Single-factor authentication (default)
	 * <li>"2" - Multi-factor authentication
	 * <li>"3" - Strong multi-factor (hardware key + another factor)
	 * </ul>
	 * @return minimum ACR level
	 */
	String minAcr() default "1";

	/**
	 * Whether to allow demo mode users. Set to false for operations that require live
	 * trading access.
	 * @return true to allow demo mode users (default), false to deny
	 */
	boolean allowDemoMode() default true;

	/**
	 * Whether authentication is required for this endpoint. When set to false, the
	 * endpoint allows anonymous/public access. Useful for endpoints that can work with or
	 * without authentication.
	 * @return true if authentication is required (default), false if optional
	 */
	boolean required() default true;

}
