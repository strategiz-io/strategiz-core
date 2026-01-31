package io.strategiz.framework.secrets.client;

import java.util.Map;

/**
 * Interface for Vault HTTP operations. Abstracts the HTTP communication with Vault
 * server.
 */
public interface VaultClient {

	/**
	 * Read data from Vault at the specified path.
	 * @param path The Vault path to read from
	 * @return The data at the path, or null if not found
	 */
	Map<String, Object> read(String path);

	/**
	 * Write data to Vault at the specified path.
	 * @param path The Vault path to write to
	 * @param data The data to write
	 */
	void write(String path, Map<String, Object> data);

	/**
	 * Delete data from Vault at the specified path.
	 * @param path The Vault path to delete
	 */
	void delete(String path);

	/**
	 * Check if Vault is healthy and accessible.
	 * @return true if Vault is accessible, false otherwise
	 */
	boolean isHealthy();

}
