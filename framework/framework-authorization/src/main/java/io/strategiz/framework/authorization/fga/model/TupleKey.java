package io.strategiz.framework.authorization.fga.model;

import java.util.Objects;

/**
 * Represents an authorization tuple in the FGA model. A tuple defines a relationship
 * between a user and a resource.
 *
 * <p>
 * Example: {@code TupleKey("user:123", "owner", "portfolio:456")}
 *
 * <p>
 * Means: "user 123 is an owner of portfolio 456"
 *
 * @param user the user identifier (e.g., "user:123")
 * @param relation the relationship type (e.g., "owner", "editor", "viewer")
 * @param object the resource identifier (e.g., "portfolio:456")
 */
public record TupleKey(String user, String relation, String object) {

	/** Creates a new TupleKey with validation. */
	public TupleKey {
		Objects.requireNonNull(user, "user must not be null");
		Objects.requireNonNull(relation, "relation must not be null");
		Objects.requireNonNull(object, "object must not be null");
	}

	/**
	 * Create a tuple key for a user-resource relationship.
	 * @param userId the user ID
	 * @param relation the relation type
	 * @param resourceType the resource type
	 * @param resourceId the resource ID
	 * @return the tuple key
	 */
	public static TupleKey of(String userId, String relation, String resourceType, String resourceId) {
		return new TupleKey("user:" + userId, relation, resourceType + ":" + resourceId);
	}
}
