package io.strategiz.service.device.model.anonymous;

import io.strategiz.service.device.model.BaseDeviceFingerprint;

/**
 * Request for updating an anonymous device registration
 * Updates fingerprint data when device characteristics change
 * Stores in: /devices (root collection)
 */
public class UpdateAnonymousDeviceRequest extends BaseDeviceFingerprint {
    // Inherits all comprehensive fingerprint fields from BaseDeviceFingerprint
    // No additional fields needed for anonymous device updates
}