package io.strategiz.service.device.model.anonymous;

import io.strategiz.service.device.model.BaseDeviceFingerprint;

/**
 * Request for creating an anonymous device registration
 * Captures FingerprintJS data and Web Crypto API public key for landing page visitors
 * Stores in: /devices (root collection)
 * 
 * No additional fields needed - anonymous devices only need the base fingerprint data
 */
public class CreateAnonymousDeviceRequest extends BaseDeviceFingerprint {
    // Inherits all fingerprint fields from BaseDeviceFingerprint
    // No additional fields needed for anonymous devices
}
