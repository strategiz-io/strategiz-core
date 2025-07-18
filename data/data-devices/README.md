# Data Devices Module

The data-devices module provides repository interfaces and entity definitions for the devices subcollection in Firebase Firestore. This module manages registered devices that users use to access the Strategiz platform, supporting device trust, security monitoring, and push notifications.

## Architecture

This module follows the Strategiz data layer architecture:
- **Entity Layer**: `entity/` package with `*Entity.java` naming convention
- **Repository Layer**: Repository interfaces defining data access contracts
- **Base Entity**: All entities extend `BaseEntity` for consistent audit field management

## Collection Structure

### Subcollection: `users/{userId}/devices`
The devices subcollection contains documents representing user devices:

```
users/
└── {userId}/
    └── devices/                       # Subcollection
        └── {deviceId}/                # Document ID (auto-generated)
            ├── deviceName             # User-friendly name
            ├── agentId                # Unique device identifier
            ├── platform               # Platform information
            │   ├── type               # mobile, desktop, tablet
            │   ├── os                 # iOS, Android, Windows, etc.
            │   ├── osVersion          # OS version
            │   ├── browser            # Browser name
            │   ├── browserVersion     # Browser version
            │   └── userAgent          # Full user agent string
            ├── lastLoginAt            # Last login timestamp
            ├── ipAddress              # Last known IP
            ├── location               # Approximate location
            ├── trusted                # Trust status
            ├── pushToken              # Push notification token
            └── [audit fields]         # From BaseEntity
```

## Entities

### UserDeviceEntity
**File**: `entity/UserDeviceEntity.java`
**Collection**: `users/{userId}/devices`
**Purpose**: Represents a device used to access the user's account

#### Key Fields
- `deviceId` (String): Unique identifier (document ID)
- `deviceName` (String): User-friendly device name
- `agentId` (String): Unique device fingerprint/identifier
- `platform` (DevicePlatform): Detailed platform information
- `lastLoginAt` (Instant): Last successful login from device
- `ipAddress` (String): Last known IP address
- `location` (String): Approximate geographic location
- `trusted` (Boolean): Whether device is trusted for reduced auth
- `pushToken` (String): Token for push notifications
- Inherits audit fields from BaseEntity: `createdAt`, `updatedAt`, `createdBy`, `updatedBy`

#### Validation
- Device name and agent ID are required (`@NotBlank`)
- Platform information required for new devices
- Trust status defaults to false

#### Usage
```java
UserDeviceEntity device = new UserDeviceEntity();
device.setDeviceName("John's iPhone");
device.setAgentId("agent_abc123xyz");
device.setPlatform(new DevicePlatform("mobile", "iOS", "17.0"));
device.setTrusted(false);
device.setLastLoginAt(Instant.now());
```

### DevicePlatform
**File**: `entity/DevicePlatform.java`
**Purpose**: Embedded object containing device platform details

#### Key Fields
- `type` (String): Device type - mobile, desktop, tablet, wearable
- `os` (String): Operating system - iOS, Android, Windows, macOS, Linux
- `osVersion` (String): OS version number
- `browser` (String): Browser name if applicable
- `browserVersion` (String): Browser version
- `userAgent` (String): Full user agent string
- `brand` (String): Device manufacturer (Apple, Samsung, etc.)
- `model` (String): Device model (iPhone 15 Pro, Galaxy S24, etc.)

#### Usage
```java
DevicePlatform platform = new DevicePlatform();
platform.setType("mobile");
platform.setOs("iOS");
platform.setOsVersion("17.0");
platform.setBrand("Apple");
platform.setModel("iPhone 15 Pro");
platform.setUserAgent("Mozilla/5.0 (iPhone; CPU iPhone OS 17_0...)");
```

## Repository Interface

### UserDeviceRepository
**File**: `repository/UserDeviceRepository.java`
**Purpose**: Data access contract for device management operations

#### Core CRUD Operations
```java
// Register new device
UserDeviceEntity registerDevice(String userId, UserDeviceEntity device);

// Get all user devices
List<UserDeviceEntity> findByUserId(String userId);

// Get specific device
Optional<UserDeviceEntity> findByUserIdAndDeviceId(String userId, String deviceId);

// Update device
UserDeviceEntity updateDevice(String userId, String deviceId, 
                            UserDeviceEntity device);

// Remove device
void removeDevice(String userId, String deviceId);
```

#### Device Identification
```java
// Find device by agent ID
Optional<UserDeviceEntity> findByUserIdAndAgentId(String userId, String agentId);

// Check if device exists
boolean existsByUserIdAndAgentId(String userId, String agentId);

// Get or create device
UserDeviceEntity getOrCreateDevice(String userId, String agentId, 
                                 UserDeviceEntity deviceDetails);
```

#### Trust Management
```java
// Mark device as trusted
void markAsTrusted(String userId, String deviceId);

// Mark device as untrusted
void markAsUntrusted(String userId, String deviceId);

// Get trusted devices
List<UserDeviceEntity> findTrustedDevices(String userId);

// Check if device is trusted
boolean isDeviceTrusted(String userId, String deviceId);

// Trust device by agent ID
void trustDeviceByAgentId(String userId, String agentId);
```

#### Activity Tracking
```java
// Update last login
void updateLastLogin(String userId, String deviceId, Instant lastLoginAt);

// Update location and IP
void updateLocationAndIp(String userId, String deviceId, 
                        String location, String ipAddress);

// Get recent devices (last 30 days)
List<UserDeviceEntity> findRecentDevices(String userId, Instant since);

// Get inactive devices
List<UserDeviceEntity> findInactiveDevices(String userId, Duration inactivityPeriod);
```

#### Platform Filtering
```java
// Get devices by platform type
List<UserDeviceEntity> findByUserIdAndPlatformType(String userId, String platformType);

// Get mobile devices
List<UserDeviceEntity> findMobileDevices(String userId);

// Get desktop devices
List<UserDeviceEntity> findDesktopDevices(String userId);

// Get devices by OS
List<UserDeviceEntity> findByUserIdAndOs(String userId, String os);
```

#### Push Notification Management
```java
// Update push token
void updatePushToken(String userId, String deviceId, String pushToken);

// Get devices with push tokens
List<UserDeviceEntity> findDevicesWithPushTokens(String userId);

// Clear push token
void clearPushToken(String userId, String deviceId);

// Find device by push token
Optional<UserDeviceEntity> findByUserIdAndPushToken(String userId, String pushToken);
```

#### Utility Operations
```java
// Count user devices
long countByUserId(String userId);

// Count by platform type
long countByUserIdAndPlatformType(String userId, String platformType);

// Remove old devices
void removeInactiveDevices(String userId, Duration inactivityPeriod);

// Remove untrusted devices
void removeUntrustedDevices(String userId);
```

## Usage Examples

### Device Registration
```java
@Autowired
private UserDeviceRepository deviceRepository;

// Register new device on login
DevicePlatform platform = new DevicePlatform();
platform.setType("mobile");
platform.setOs("iOS");
platform.setOsVersion("17.0");
platform.setBrand("Apple");
platform.setModel("iPhone 15 Pro");
platform.setUserAgent(request.getHeader("User-Agent"));

UserDeviceEntity device = new UserDeviceEntity();
device.setDeviceName("iPhone 15 Pro");
device.setAgentId(generateDeviceAgentId(request));
device.setPlatform(platform);
device.setLastLoginAt(Instant.now());
device.setIpAddress(request.getRemoteAddr());
device.setLocation("San Francisco, CA");

UserDeviceEntity registered = deviceRepository.registerDevice("user123", device);
```

### Trust Management
```java
// Mark device as trusted after MFA verification
deviceRepository.markAsTrusted("user123", "deviceId456");

// Get all trusted devices for security settings
List<UserDeviceEntity> trustedDevices = 
    deviceRepository.findTrustedDevices("user123");

// Remove trust from all devices (security reset)
List<UserDeviceEntity> allDevices = deviceRepository.findByUserId("user123");
for (UserDeviceEntity device : allDevices) {
    deviceRepository.markAsUntrusted("user123", device.getDeviceId());
}
```

### Activity Monitoring
```java
// Update device on each login
deviceRepository.updateLastLogin("user123", "deviceId456", Instant.now());
deviceRepository.updateLocationAndIp("user123", "deviceId456", 
                                   "New York, NY", "192.168.1.1");

// Get recently active devices
Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);
List<UserDeviceEntity> recentDevices = 
    deviceRepository.findRecentDevices("user123", thirtyDaysAgo);

// Clean up old devices
Duration sixMonths = Duration.ofDays(180);
deviceRepository.removeInactiveDevices("user123", sixMonths);
```

### Push Notifications
```java
// Update push token when app launches
String fcmToken = "fcm_token_from_client";
deviceRepository.updatePushToken("user123", "deviceId456", fcmToken);

// Get all devices for push notification
List<UserDeviceEntity> pushDevices = 
    deviceRepository.findDevicesWithPushTokens("user123");

// Send push notification to all devices
for (UserDeviceEntity device : pushDevices) {
    sendPushNotification(device.getPushToken(), message);
}
```

### Platform Analytics
```java
// Count devices by platform
long mobileCount = deviceRepository.countByUserIdAndPlatformType("user123", "mobile");
long desktopCount = deviceRepository.countByUserIdAndPlatformType("user123", "desktop");

// Get iOS devices for app update notification
List<UserDeviceEntity> iosDevices = 
    deviceRepository.findByUserIdAndOs("user123", "iOS");
```

## Security Features

### Device Trust System
- New devices start as untrusted
- Trust granted after additional authentication
- Trusted devices may skip certain auth steps
- Trust can be revoked at any time
- Trust expires after extended inactivity

### Device Fingerprinting
- Agent ID uniquely identifies devices
- Generated from multiple device attributes
- Helps detect device cloning/spoofing
- Used for anomaly detection

### Location Tracking
- Approximate location from IP geolocation
- Helps detect suspicious login attempts
- User can review login locations
- Triggers alerts for new locations

### Activity Monitoring
- Tracks last login time per device
- Identifies inactive devices
- Supports automatic cleanup policies
- Helps detect compromised devices

## Implementation Notes

### Firebase Firestore Structure
- Subcollection under each user document
- Auto-generated document IDs
- Supports real-time device status updates
- Efficient queries with proper indexing

### Device Identification Strategy
- Agent ID generated client-side
- Combines multiple device attributes
- Stored in secure client storage
- Regenerated if storage cleared

### Platform Detection
- User agent parsing for detailed info
- Client-side JavaScript for accuracy
- Fallback to server-side detection
- Regular updates for new devices

### Push Token Management
- Tokens expire and need refresh
- One token per device
- Cleared on app uninstall
- Platform-specific (FCM, APNS)

## Integration with Other Modules

### Dependencies
- **data-base**: For BaseEntity and utilities
- **framework-common**: For shared constants

### Used By
- **business-auth**: For device-based authentication
- **service-auth**: For device management APIs
- **business-notification**: For push notifications
- **data-user**: For user data aggregation

## Testing Guidelines

### Unit Tests
Focus on testing:
- Device registration flow
- Trust state transitions
- Activity update logic
- Platform parsing accuracy
- Token management

### Integration Tests
Verify with Firebase:
- Subcollection operations
- Query performance
- Real-time updates
- Cleanup operations
- Concurrent device updates

## Future Enhancements

### Planned Features
- Device-based session management
- Biometric authentication flags
- Device security scoring
- Automated trust policies
- Device grouping/families
- Cross-device sync settings

### Security Enhancements
- Advanced fingerprinting
- Behavioral analysis
- Risk-based trust levels
- Device attestation
- Jailbreak/root detection

### Analytics Features
- Device usage patterns
- Platform adoption metrics
- Login frequency analysis
- Geographic distribution
- Device lifecycle tracking