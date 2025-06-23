package io.strategiz.data.device.constants;

/**
 * Constants for device identity module
 */
public final class DeviceConstants {

    private DeviceConstants() {
        // Utility class, no instantiation
    }
    
    /**
     * Collection names in Firestore
     */
    public static final class Collections {
        private Collections() {
            // Utility class, no instantiation
        }
        
        /**
         * Device identities collection
         */
        public static final String DEVICE_IDENTITIES = "device_identities";
    }
    
    /**
     * Field names in the device identities collection
     */
    public static final class Fields {
        private Fields() {
            // Utility class, no instantiation
        }
        
        public static final String ID = "id";
        public static final String DEVICE_ID = "deviceId";
        public static final String USER_ID = "userId";
        public static final String DEVICE_NAME = "deviceName";
        public static final String FIRST_SEEN = "firstSeen";
        public static final String LAST_SEEN = "lastSeen";
        public static final String TRUSTED = "trusted";
        public static final String PUBLIC_KEY = "publicKey";
        
        // Platform fields as individual columns
        public static final String PLATFORM_BRAND = "platformBrand";
        public static final String PLATFORM_MODEL = "platformModel";
        public static final String PLATFORM_OS = "platformOs";
        public static final String PLATFORM_TYPE = "platformType";
        public static final String USER_AGENT = "userAgent";
        public static final String PLATFORM_VERSION = "platformVersion";
    }
}
