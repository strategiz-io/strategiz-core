# Data Preferences Module

The data-preferences module provides repository interfaces and entity definitions for the user preferences subcollection in Firebase Firestore. This module manages all user-configurable settings including themes, notifications, trading preferences, and other personalization options.

## Architecture

This module follows the Strategiz data layer architecture:
- **Entity Layer**: `entity/` package with `*Entity.java` naming convention
- **Repository Layer**: Repository interfaces defining data access contracts
- **Base Entity**: All entities extend `BaseEntity` for consistent audit field management

## Collection Structure

### Subcollection: `users/{userId}/preferences`
The preferences subcollection stores user settings organized by category:

```
users/
└── {userId}/
    └── preferences/                   # Subcollection
        ├── theme/                     # Theme preferences document
        │   ├── category: "theme"
        │   ├── settings: {
        │   │   ├── mode: "dark"
        │   │   ├── primaryColor: "#00ff00"
        │   │   └── fontSize: "medium"
        │   │ }
        │   └── [audit fields]
        │
        ├── notifications/             # Notification preferences document
        │   ├── category: "notifications"
        │   ├── settings: {
        │   │   ├── email: true
        │   │   ├── push: true
        │   │   ├── sms: false
        │   │   ├── tradingAlerts: true
        │   │   └── marketUpdates: false
        │   │ }
        │   └── [audit fields]
        │
        ├── trading/                   # Trading preferences document
        │   ├── category: "trading"
        │   ├── settings: {
        │   │   ├── defaultMode: "demo"
        │   │   ├── confirmOrders: true
        │   │   ├── defaultOrderType: "limit"
        │   │   └── riskLevel: "moderate"
        │   │ }
        │   └── [audit fields]
        │
        └── display/                   # Display preferences document
            ├── category: "display"
            ├── settings: {
            │   ├── chartType: "candlestick"
            │   ├── timeframe: "1D"
            │   ├── indicators: ["SMA", "RSI"]
            │   └── gridLayout: "2x2"
            │ }
            └── [audit fields]
```

## Entity

### UserPreferenceEntity
**File**: `entity/UserPreferenceEntity.java`
**Collection**: `users/{userId}/preferences`
**Purpose**: Represents a category of user preferences with flexible settings

#### Key Fields
- `preferenceId` (String): Document ID (typically same as category)
- `category` (String): Preference category name
- `settings` (Map<String, Object>): Category-specific settings
- Inherits audit fields from BaseEntity: `createdAt`, `updatedAt`, `createdBy`, `updatedBy`

#### Validation
- Category is required (`@NotBlank`)
- Settings map can contain any serializable values
- Document ID matches category for easy lookup

#### Usage
```java
// Create theme preferences
Map<String, Object> themeSettings = new HashMap<>();
themeSettings.put("mode", "dark");
themeSettings.put("primaryColor", "#00ff00");
themeSettings.put("fontSize", "medium");

UserPreferenceEntity themePrefs = new UserPreferenceEntity("theme", themeSettings);
```

## Repository Interface

### UserPreferenceRepository
**File**: `repository/UserPreferenceRepository.java`
**Purpose**: Data access contract for preference management operations

#### Core CRUD Operations
```java
// Save preference
UserPreferenceEntity savePreference(String userId, UserPreferenceEntity preference);

// Get all preferences
List<UserPreferenceEntity> findByUserId(String userId);

// Get specific preference category
Optional<UserPreferenceEntity> findByUserIdAndCategory(String userId, String category);

// Update preference
UserPreferenceEntity updatePreference(String userId, String category, 
                                    UserPreferenceEntity preference);

// Delete preference category
void deletePreference(String userId, String category);
```

#### Setting Management
```java
// Update single setting
void updateSetting(String userId, String category, String settingKey, Object value);

// Update multiple settings
void updateSettings(String userId, String category, Map<String, Object> settings);

// Get specific setting value
Optional<Object> getSetting(String userId, String category, String settingKey);

// Remove specific setting
void removeSetting(String userId, String category, String settingKey);
```

#### Category-Specific Methods
```java
// Theme preferences
Optional<UserPreferenceEntity> getThemePreference(String userId);
void setTheme(String userId, String theme);
void setThemeMode(String userId, String mode);

// Notification preferences
Optional<UserPreferenceEntity> getNotificationPreference(String userId);
void setNotificationSetting(String userId, String notificationType, boolean enabled);
Map<String, Boolean> getAllNotificationSettings(String userId);

// Trading preferences
Optional<UserPreferenceEntity> getTradingPreference(String userId);
void setTradingMode(String userId, String tradingMode);
void setDefaultOrderType(String userId, String orderType);

// Display preferences
Optional<UserPreferenceEntity> getDisplayPreference(String userId);
void setChartType(String userId, String chartType);
void setTimeframe(String userId, String timeframe);
```

#### Utility Operations
```java
// Check if preference exists
boolean hasPreference(String userId, String category);

// Get preference categories
List<String> getUserPreferenceCategories(String userId);

// Reset category to defaults
void resetToDefaults(String userId, String category);

// Clear all preferences
void clearAllPreferences(String userId);

// Copy preferences from another user
void copyPreferences(String fromUserId, String toUserId);
```

#### Bulk Operations
```java
// Save multiple preferences
List<UserPreferenceEntity> saveMultiplePreferences(String userId, 
                                                  List<UserPreferenceEntity> preferences);

// Get preferences by categories
List<UserPreferenceEntity> findByUserIdAndCategories(String userId, 
                                                    List<String> categories);

// Update multiple categories
void updateMultiplePreferences(String userId, 
                             Map<String, UserPreferenceEntity> preferencesByCategory);
```

## Preference Categories

### Theme Preferences
**Category**: `theme`
**Purpose**: Visual appearance settings

```java
Map<String, Object> themeSettings = new HashMap<>();
themeSettings.put("mode", "dark");              // dark, light, system
themeSettings.put("primaryColor", "#00ff00");   // Hex color
themeSettings.put("accentColor", "#0066ff");    // Hex color
themeSettings.put("fontSize", "medium");        // small, medium, large
themeSettings.put("fontFamily", "system");      // system, mono, sans
themeSettings.put("density", "comfortable");    // compact, comfortable, spacious
```

### Notification Preferences
**Category**: `notifications`
**Purpose**: Communication and alert settings

```java
Map<String, Object> notificationSettings = new HashMap<>();
notificationSettings.put("email", true);
notificationSettings.put("push", true);
notificationSettings.put("sms", false);
notificationSettings.put("tradingAlerts", true);
notificationSettings.put("priceAlerts", true);
notificationSettings.put("marketUpdates", false);
notificationSettings.put("newsAlerts", true);
notificationSettings.put("accountAlerts", true);
notificationSettings.put("frequency", "realtime"); // realtime, hourly, daily
```

### Trading Preferences
**Category**: `trading`
**Purpose**: Trading behavior and risk settings

```java
Map<String, Object> tradingSettings = new HashMap<>();
tradingSettings.put("defaultMode", "demo");          // demo, real
tradingSettings.put("confirmOrders", true);
tradingSettings.put("defaultOrderType", "limit");    // market, limit, stop
tradingSettings.put("defaultTimeInForce", "GTC");    // GTC, IOC, FOK
tradingSettings.put("riskLevel", "moderate");        // conservative, moderate, aggressive
tradingSettings.put("maxPositionSize", 1000);
tradingSettings.put("stopLossEnabled", true);
tradingSettings.put("stopLossPercentage", 5.0);
```

### Display Preferences
**Category**: `display`
**Purpose**: Chart and dashboard display settings

```java
Map<String, Object> displaySettings = new HashMap<>();
displaySettings.put("chartType", "candlestick");     // candlestick, line, bar
displaySettings.put("timeframe", "1D");              // 1M, 5M, 1H, 1D, 1W
displaySettings.put("indicators", Arrays.asList("SMA", "RSI", "MACD"));
displaySettings.put("showVolume", true);
displaySettings.put("gridLayout", "2x2");            // 1x1, 2x2, 3x3
displaySettings.put("showOrderBook", true);
displaySettings.put("showTradeHistory", true);
```

### Privacy Preferences
**Category**: `privacy`
**Purpose**: Data sharing and privacy controls

```java
Map<String, Object> privacySettings = new HashMap<>();
privacySettings.put("shareAnalytics", true);
privacySettings.put("sharePerformance", false);
privacySettings.put("publicProfile", false);
privacySettings.put("showOnlineStatus", true);
privacySettings.put("allowMessages", "friends");     // all, friends, none
```

## Usage Examples

### Setting Theme Preferences
```java
@Autowired
private UserPreferenceRepository preferenceRepository;

// Set dark theme
preferenceRepository.setTheme("user123", "dark");

// Or set complete theme preferences
Map<String, Object> themeSettings = new HashMap<>();
themeSettings.put("mode", "dark");
themeSettings.put("primaryColor", "#00ff00");
themeSettings.put("fontSize", "large");

UserPreferenceEntity theme = new UserPreferenceEntity("theme", themeSettings);
preferenceRepository.savePreference("user123", theme);
```

### Managing Notifications
```java
// Enable email notifications
preferenceRepository.setNotificationSetting("user123", "email", true);

// Get all notification settings
Optional<UserPreferenceEntity> notifPrefs = 
    preferenceRepository.getNotificationPreference("user123");

if (notifPrefs.isPresent()) {
    Map<String, Object> settings = notifPrefs.get().getSettings();
    boolean emailEnabled = (Boolean) settings.get("email");
    boolean pushEnabled = (Boolean) settings.get("push");
}

// Bulk update notification settings
Map<String, Object> notificationSettings = new HashMap<>();
notificationSettings.put("email", true);
notificationSettings.put("push", true);
notificationSettings.put("sms", false);
preferenceRepository.updateSettings("user123", "notifications", notificationSettings);
```

### Trading Configuration
```java
// Set trading mode
preferenceRepository.setTradingMode("user123", "real");

// Update risk settings
Map<String, Object> riskSettings = new HashMap<>();
riskSettings.put("riskLevel", "conservative");
riskSettings.put("maxPositionSize", 500);
riskSettings.put("stopLossEnabled", true);
riskSettings.put("stopLossPercentage", 3.0);

preferenceRepository.updateSettings("user123", "trading", riskSettings);
```

### Loading User Preferences
```java
// Load all preferences on app startup
List<UserPreferenceEntity> allPreferences = 
    preferenceRepository.findByUserId("user123");

Map<String, Map<String, Object>> preferenceMap = new HashMap<>();
for (UserPreferenceEntity pref : allPreferences) {
    preferenceMap.put(pref.getCategory(), pref.getSettings());
}

// Apply preferences to UI
applyTheme(preferenceMap.get("theme"));
configureNotifications(preferenceMap.get("notifications"));
setupTrading(preferenceMap.get("trading"));
```

### Default Preferences
```java
// Set default preferences for new users
public void setDefaultPreferences(String userId) {
    // Theme defaults
    Map<String, Object> themeDefaults = new HashMap<>();
    themeDefaults.put("mode", "system");
    themeDefaults.put("fontSize", "medium");
    
    // Notification defaults
    Map<String, Object> notificationDefaults = new HashMap<>();
    notificationDefaults.put("email", true);
    notificationDefaults.put("push", true);
    notificationDefaults.put("tradingAlerts", true);
    
    // Save defaults
    preferenceRepository.savePreference(userId, 
        new UserPreferenceEntity("theme", themeDefaults));
    preferenceRepository.savePreference(userId, 
        new UserPreferenceEntity("notifications", notificationDefaults));
}
```

## Implementation Notes

### Storage Strategy
- Each preference category is a separate document
- Settings stored as flexible map structure
- Allows addition of new settings without schema changes
- Document ID matches category name for efficiency

### Type Safety
- Repository provides type-safe convenience methods
- Generic get/set for flexibility
- Type casting handled at service layer
- Validation at business logic layer

### Performance Optimization
- Load all preferences in single query
- Cache preferences in application
- Update only changed settings
- Batch updates when possible

### Default Values
- Defaults handled at business layer
- Missing preferences return empty Optional
- Service layer provides fallback values
- New settings added with backward compatibility

## Integration with Other Modules

### Dependencies
- **data-base**: For BaseEntity and utilities
- **framework-common**: For constants and enums

### Used By
- **business-preference**: Business logic and validation
- **service-settings**: REST APIs for preferences
- **All UI modules**: For applying user preferences
- **data-user**: For user data aggregation

## Testing Guidelines

### Unit Tests
Focus on testing:
- Preference CRUD operations
- Setting updates and merges
- Type conversion logic
- Default value handling
- Category-specific methods

### Integration Tests
Verify with Firebase:
- Subcollection operations
- Concurrent updates
- Large settings maps
- Missing preference handling
- Migration scenarios

## Future Enhancements

### Planned Features
- Preference versioning
- Preference templates
- Import/export preferences
- Preference history/audit
- A/B testing preferences
- Device-specific preferences

### Advanced Settings
- Keyboard shortcuts
- API rate preferences
- Advanced charting options
- Custom indicators
- Workspace layouts
- Plugin configurations

### Performance Improvements
- Preference caching strategy
- Partial preference loading
- Preference compression
- Change detection optimization
- Real-time preference sync