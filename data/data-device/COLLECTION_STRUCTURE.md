# Device Collection Structure

## Firestore Collections

### Anonymous Devices
**Collection Path:** `/devices`
- Stored at root level
- Used for landing page visitors
- No user association
- Tracked by visitor_id from FingerprintJS

### Authenticated Devices  
**Collection Path:** `/users/{userId}/devices`
- Stored as subcollection under each user
- Created during sign-up step 2 or sign-in
- Associated with specific user
- Contains trust indicators and session data

## Repository Methods Mapping

### CreateDeviceRepository
- `createAnonymousDevice()` → `/devices`
- `createAuthenticatedDevice()` → `/users/{userId}/devices`

### ReadDeviceRepository
- `findAnonymousDevice()` → `/devices/{deviceId}`
- `findAuthenticatedDevice()` → `/users/{userId}/devices/{deviceId}`
- `findAllByUserId()` → `/users/{userId}/devices`

### UpdateDeviceRepository
- `updateAnonymousDevice()` → `/devices/{deviceId}`
- `updateAuthenticatedDevice()` → `/users/{userId}/devices/{deviceId}`

### DeleteDeviceRepository
- `deleteAnonymousDevice()` → `/devices/{deviceId}`
- `deleteAuthenticatedDevice()` → `/users/{userId}/devices/{deviceId}`

## Device ID in Authentication Token

During sign-in or sign-up, the device ID should be included in the authentication token (PASETO or JWT) to track device-based sessions. This allows for:

1. Device-specific session management
2. Detection of suspicious login attempts from new devices
3. Device trust scoring over time
4. User ability to manage trusted devices