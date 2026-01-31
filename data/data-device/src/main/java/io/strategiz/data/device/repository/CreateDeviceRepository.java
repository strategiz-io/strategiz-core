package io.strategiz.data.device.repository;

import io.strategiz.data.device.model.DeviceIdentity;

/**
 * Repository interface exclusively for device creation operations Single Responsibility:
 * Device creation/persistence
 *
 * Collection Paths: - Anonymous: /devices (root collection) - Authenticated:
 * /users/{userId}/devices (subcollection)
 */
public interface CreateDeviceRepository {

	/**
	 * Create a new authenticated device Stores in: /users/{userId}/devices
	 * @param device The device entity to create
	 * @param userId The user ID (required for subcollection path)
	 * @return The created device with generated ID
	 */
	DeviceIdentity createAuthenticatedDevice(DeviceIdentity device, String userId);

	/**
	 * Create a new anonymous device Stores in: /devices (root collection)
	 * @param device The device entity to create
	 * @return The created device with generated ID
	 */
	DeviceIdentity createAnonymousDevice(DeviceIdentity device);

	/**
	 * Bulk create authenticated devices For migration or batch operations
	 * @param devices Array of devices to create
	 * @param userId The user ID
	 * @return Array of created devices
	 */
	DeviceIdentity[] bulkCreateAuthenticatedDevices(DeviceIdentity[] devices, String userId);

	/**
	 * Bulk create anonymous devices For batch operations
	 * @param devices Array of devices to create
	 * @return Array of created devices
	 */
	DeviceIdentity[] bulkCreateAnonymousDevices(DeviceIdentity[] devices);

}