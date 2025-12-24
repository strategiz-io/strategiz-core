# Data User Module

The data-user module provides repository interfaces and entity definitions for the main users collection in Firebase Firestore. This module handles CRUD operations for user documents and provides aggregation capabilities to fetch user data along with their subcollections.

## Architecture

This module follows the Strategiz data layer architecture:
- **Entity Layer**: `entity/` package with `*Entity.java` naming convention
- **Repository Layer**: Repository interfaces defining data access contracts  
- **Base Entity**: All entities extend `BaseEntity` for consistent audit field management

## Collections Structure

### Primary Collection: `users`
The users collection contains the main user documents with embedded profile information:

```
users/
├── {userId}/                          # Document ID (Firebase auto-generated)
│   ├── profile/                       # Embedded UserProfileEntity
│   └── subcollections/
│       ├── watchlist/                 # User's market watchlist (data-watchlist module)
│       ├── providers/                 # Connected trading providers (data-provider module)
│       ├── devices/                   # Registered devices (data-device module)
│       └── preferences/               # User preferences (data-preferences module)
```

**Note**: This module focuses solely on the main user document. Subcollections are handled by dedicated modules as shown above.

## Entities

### UserEntity
**File**: `entity/UserEntity.java`
**Collection**: `users`
**Purpose**: Root document representing a user account

#### Key Fields
- `userId` (String): Unique user identifier (document ID)
- `profile` (UserProfileEntity): Embedded user profile information
- Inherits audit fields from BaseEntity: `createdAt`, `updatedAt`, `createdBy`, `updatedBy`

#### Usage
```java
UserEntity user = new UserEntity();
user.setUserId("user123");
user.setProfile(new UserProfileEntity("John", "Doe", "john@example.com"));
```

### UserProfileEntity
**File**: `entity/UserProfileEntity.java` 
**Purpose**: Embedded object within UserEntity containing profile information

#### Key Fields
- `firstName` (String): User's first name
- `lastName` (String): User's last name  
- `email` (String): User's email address
- `phoneNumber` (String): User's phone number
- `dateOfBirth` (LocalDate): User's date of birth
- `timezone` (String): User's preferred timezone
- `avatarUrl` (String): URL to user's profile picture

#### Validation
- Email format validation using `@Email` annotation
- Required fields marked with `@NotBlank`
- Phone number format validation

#### Usage
```java
UserProfileEntity profile = new UserProfileEntity();
profile.setFirstName("John");
profile.setLastName("Doe");
profile.setEmail("john@example.com");
profile.setPhoneNumber("+1234567890");
```

### UserAggregateData
**File**: `dto/UserAggregateData.java`
**Purpose**: Aggregate object containing user data and all subcollection data

#### Key Fields
- `user` (UserEntity): Main user document
- `watchlistItems` (List<WatchlistItemEntity>): User's watchlist
- `connectedProviders` (List<ConnectedProviderEntity>): Connected providers
- `devices` (List<UserDeviceEntity>): Registered devices  
- `preferences` (List<UserPreferenceEntity>): User preferences

#### Usage
```java
UserAggregateData aggregateData = userRepository.getUserWithAllData("user123");
UserEntity user = aggregateData.getUser();
List<WatchlistItemEntity> watchlist = aggregateData.getWatchlistItems();
```

## Repository Interface

### UserRepository
**File**: `repository/UserRepository.java`
**Purpose**: Data access contract for user operations

#### Core CRUD Operations
```java
// Create new user
UserEntity createUser(UserEntity user);

// Find user by ID
Optional<UserEntity> findById(String userId);

// Update user
UserEntity updateUser(UserEntity user);

// Delete user
void deleteUser(String userId);

// Check if user exists
boolean existsById(String userId);
```

#### Profile Operations
```java
// Update user profile
UserEntity updateProfile(String userId, UserProfileEntity profile);

// Get user profile only
Optional<UserProfileEntity> getProfile(String userId);
```

#### Aggregation Operations
```java
// Get user with all subcollection data
UserAggregateData getUserWithAllData(String userId);

// Get user with specific subcollections
UserAggregateData getUserWithData(String userId, boolean includeWatchlist, 
                                  boolean includeProviders, boolean includeDevices, 
                                  boolean includePreferences);
```

#### Search Operations
```java
// Find users by email
Optional<UserEntity> findByEmail(String email);

// Find users by phone number
Optional<UserEntity> findByPhoneNumber(String phoneNumber);

// Search users by name
List<UserEntity> searchByName(String nameQuery);
```

#### Statistics Operations
```java
// Count total users
long countUsers();

// Count users created since date
long countUsersCreatedSince(Instant since);
```

## Integration with Subcollections

The data-user module coordinates with other data modules to provide complete user data:

### Related Modules
- **data-watchlist**: `users/{userId}/watchlist` subcollection
- **data-provider**: `users/{userId}/providers` subcollection
- **data-device**: `users/{userId}/devices` subcollection
- **data-preferences**: `users/{userId}/preferences` subcollection

### Aggregation Strategy
The `getUserWithAllData()` method internally calls repositories from other modules to gather complete user information:

```java
UserAggregateData getUserWithAllData(String userId) {
    // 1. Fetch main user document
    UserEntity user = findById(userId);
    
    // 2. Fetch subcollection data
    List<WatchlistItemEntity> watchlist = watchlistRepo.findByUserId(userId);
    List<ConnectedProviderEntity> providers = providerRepo.findByUserId(userId);
    List<UserDeviceEntity> devices = deviceRepo.findByUserId(userId);
    List<UserPreferenceEntity> preferences = preferenceRepo.findByUserId(userId);
    
    // 3. Return aggregate object
    return new UserAggregateData(user, watchlist, providers, devices, preferences);
}
```

## Usage Examples

### Creating a New User
```java
@Autowired
private UserRepository userRepository;

// Create user profile
UserProfileEntity profile = new UserProfileEntity();
profile.setFirstName("John");
profile.setLastName("Doe");
profile.setEmail("john@example.com");

// Create user entity
UserEntity user = new UserEntity();
user.setProfile(profile);

// Save to database
UserEntity savedUser = userRepository.createUser(user);
String userId = savedUser.getUserId();
```

### Fetching Complete User Data
```java
// Get user with all subcollection data
UserAggregateData userData = userRepository.getUserWithAllData("user123");

// Access different data types
UserEntity user = userData.getUser();
String email = user.getProfile().getEmail();
List<WatchlistItemEntity> watchlist = userData.getWatchlistItems();
List<ConnectedProviderEntity> providers = userData.getConnectedProviders();
```

### Updating User Profile
```java
// Update profile information
UserProfileEntity updatedProfile = new UserProfileEntity();
updatedProfile.setFirstName("Jane");
updatedProfile.setLastName("Smith");
updatedProfile.setEmail("jane@example.com");

UserEntity updatedUser = userRepository.updateProfile("user123", updatedProfile);
```

### Search Operations
```java
// Find user by email
Optional<UserEntity> user = userRepository.findByEmail("john@example.com");

// Search users by name
List<UserEntity> users = userRepository.searchByName("John");
```

## Implementation Notes

### Firebase Firestore Integration
- Uses Firebase Admin SDK for server-side operations
- Documents stored in `users` collection
- Subcollections are handled by separate modules
- Auto-generated document IDs used as `userId`

### Audit Trail
- All entities extend `BaseEntity` for automatic audit field management
- `createdAt` and `updatedAt` timestamps automatically maintained
- `createdBy` and `updatedBy` fields track user actions

### Validation
- Bean validation annotations used for input validation
- Email format validation on profile entity
- Required field validation enforced
- Phone number format validation included

### Error Handling
- Repository methods return `Optional<T>` for single results
- Null-safe operations throughout
- Validation exceptions propagated to service layer

## Dependencies

### Maven Dependencies
```xml
<dependencies>
    <!-- Framework modules -->
    <dependency>
        <groupId>io.strategiz</groupId>
        <artifactId>framework-common</artifactId>
    </dependency>
    
    <!-- Data modules -->
    <dependency>
        <groupId>io.strategiz</groupId>
        <artifactId>data-base</artifactId>
    </dependency>
    
    <!-- Firebase -->
    <dependency>
        <groupId>com.google.firebase</groupId>
        <artifactId>firebase-admin</artifactId>
    </dependency>
    
    <!-- Validation -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
</dependencies>
```

### Module Dependencies
For aggregation operations, this module depends on:
- `data-watchlist`
- `data-provider`
- `data-device`
- `data-preferences`

These dependencies are injected at runtime through the implementation layer.

## Testing

### Unit Tests
Repository interface testing should focus on:
- CRUD operation contracts
- Validation behavior
- Search functionality
- Aggregation logic

### Integration Tests
Firebase integration testing should verify:
- Document creation and retrieval
- Subcollection aggregation
- Transaction handling
- Error scenarios

## Security Considerations

### Data Access
- Repository operates with service account privileges
- No user-level security at data layer
- Security enforced at business/service layers

### Data Privacy
- Email and phone number fields contain PII
- Profile information should be handled according to privacy policies
- Audit trail maintains user action history

### Validation
- Input validation prevents malformed data
- Email format validation prevents invalid addresses
- Required field validation ensures data integrity

## Performance Considerations

### Firestore Operations
- Single document reads are efficient
- Aggregation operations require multiple subcollection queries
- Consider caching for frequently accessed user data
- Batch operations for bulk user operations

### Indexing
- Email field should be indexed for efficient lookups
- Phone number field should be indexed if used for lookup
- Consider composite indexes for complex queries

## Future Enhancements

### Potential Improvements
- Implement soft delete functionality
- Add user role and permission management
- Implement user status tracking (active, suspended, etc.)
- Add user activity logging
- Implement data archival for inactive users

### Scalability Considerations
- Consider sharding strategy for large user bases
- Implement caching layer for frequently accessed data
- Monitor and optimize query performance
- Consider read replicas for improved read performance
