package io.strategiz.framework.authorization.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to inject the authenticated user into a controller method parameter.
 *
 * <p>Usage example:</p>
 * <pre>
 * &#64;GetMapping("/profile")
 * &#64;RequireAuth
 * public ResponseEntity&lt;?&gt; getProfile(&#64;AuthUser AuthenticatedUser user) {
 *     String userId = user.getUserId();
 *     // ...
 * }
 *
 * // Optional - returns null if not authenticated
 * &#64;GetMapping("/public")
 * public ResponseEntity&lt;?&gt; publicEndpoint(&#64;AuthUser(required = false) AuthenticatedUser user) {
 *     if (user != null) {
 *         // Personalized response
 *     }
 *     // ...
 * }
 * </pre>
 *
 * @see io.strategiz.framework.authorization.resolver.AuthUserArgumentResolver
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuthUser {

    /**
     * Whether authentication is required.
     * If true (default) and no user is authenticated, throws 401.
     * If false, the parameter will be null when not authenticated.
     *
     * @return true if authentication is required
     */
    boolean required() default true;
}
