package io.strategiz.framework.authorization.annotation;

import io.strategiz.framework.authorization.model.ResourceType;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to require fine-grained authorization via FGA (OpenFGA).
 *
 * <p>
 * This provides Layer 2 authorization - relationship-based access control. It checks if
 * the authenticated user has a specific relation to a resource.
 *
 * <p>
 * The resourceId supports SpEL expressions to extract values from method parameters:
 *
 * <ul>
 * <li>{@code #paramName} - Reference a method parameter by name
 * <li>{@code #request.id} - Reference a property of a parameter
 * </ul>
 *
 * <p>
 * Usage examples:
 *
 * <pre>
 * // Check if user is owner of the portfolio
 * &#64;Authorize(relation = "owner", resource = ResourceType.PORTFOLIO,
 *     resourceId = "#portfolioId")
 * public ResponseEntity&lt;?&gt; deletePortfolio(&#64;PathVariable String portfolioId) {
 *     // ...
 * }
 *
 * // Check if user can view a strategy (owner, editor, or viewer)
 * &#64;Authorize(relation = "viewer", resource = ResourceType.STRATEGY,
 *     resourceId = "#request.strategyId")
 * public ResponseEntity&lt;?&gt; viewStrategy(&#64;RequestBody ViewRequest request) {
 *     // ...
 * }
 * </pre>
 *
 * @see io.strategiz.framework.authorization.aspect.FGAAuthorizationAspect
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Authorize {

	/**
	 * The relation to check (e.g., "owner", "editor", "viewer").
	 * @return the relation name
	 */
	String relation();

	/**
	 * The type of resource being accessed.
	 * @return the resource type
	 */
	ResourceType resource();

	/**
	 * SpEL expression to extract the resource ID from method parameters. Examples:
	 * "#portfolioId", "#request.id"
	 * @return the SpEL expression for the resource ID
	 */
	String resourceId();

}
