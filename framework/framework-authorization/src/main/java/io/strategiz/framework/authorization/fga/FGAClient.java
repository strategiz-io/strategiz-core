package io.strategiz.framework.authorization.fga;

import java.util.List;

/**
 * Fine-Grained Authorization client interface.
 *
 * <p>This interface abstracts the FGA (OpenFGA) operations for relationship-based
 * authorization. The current implementation is {@link FGAMockClient} which uses
 * an in-memory store. A future {@code client-openfga} module will provide a
 * production implementation using the OpenFGA SDK.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * // Grant access when creating a resource
 * fgaClient.write("user:123", "owner", "portfolio:456");
 *
 * // Check access before performing an action
 * if (fgaClient.check("user:123", "viewer", "portfolio:456")) {
 *     // User can view the portfolio
 * }
 *
 * // Revoke access when deleting a resource
 * fgaClient.delete("user:123", "owner", "portfolio:456");
 * </pre>
 */
public interface FGAClient {

    /**
     * Check if a user has a specific relation to an object.
     *
     * @param user the user identifier (e.g., "user:123")
     * @param relation the relation to check (e.g., "owner", "editor", "viewer")
     * @param object the object identifier (e.g., "portfolio:456")
     * @return true if the user has the relation to the object
     */
    boolean check(String user, String relation, String object);

    /**
     * Write (grant) a tuple - establish a relationship between user and object.
     *
     * @param user the user identifier
     * @param relation the relation to grant
     * @param object the object identifier
     */
    void write(String user, String relation, String object);

    /**
     * Delete (revoke) a tuple - remove a relationship between user and object.
     *
     * @param user the user identifier
     * @param relation the relation to revoke
     * @param object the object identifier
     */
    void delete(String user, String relation, String object);

    /**
     * List all objects of a given type that a user has a specific relation to.
     *
     * @param user the user identifier
     * @param relation the relation to filter by
     * @param type the object type to filter by (e.g., "portfolio")
     * @return list of object identifiers
     */
    List<String> listObjects(String user, String relation, String type);
}
