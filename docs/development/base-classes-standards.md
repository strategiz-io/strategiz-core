# Development Standards: BaseController & BaseService Usage

## 🎯 **Overview**

This document outlines the development standards for using `BaseController`, `BaseAuthenticationController`, and `BaseService` classes in the Strategiz application. These base classes ensure consistency, error handling, and observability across all layers.

## 📋 **Core Principles**

### 1. **Consistent Exception Handling**
- ✅ **Use StrategizException with ErrorCode enums** for all business errors
- ✅ **Let exceptions bubble up** to GlobalExceptionHandler
- ❌ **Never catch and hide exceptions** without logging
- ❌ **Never return null** - throw exceptions instead

### 2. **Response Standardization**
- ✅ **Use createCleanResponse()** for all successful responses
- ✅ **Filter null/empty fields** automatically
- ✅ **Include request tracking** for observability
- ❌ **Never return raw objects** without ResponseEntity

### 3. **Input Validation**
- ✅ **Validate all inputs** at controller and service layers
- ✅ **Use provided validation methods** for consistency
- ✅ **Throw StrategizException** for validation failures
- ❌ **Never trust user input** without sanitization

---

## 🎮 **BaseController Usage**

### **When to Use:**
- General REST controllers without authentication requirements
- Public endpoints (health checks, documentation, etc.)
- Simple CRUD operations that don't need user context

### **Key Methods:**

```java
// Request tracking
initializeRequestTracking()
finalizeRequestTracking()

// Input validation
validateEmail(email, "userEmail")
validatePhoneNumber(phone, "contactPhone")
validateAlphanumeric(code, "verificationCode")
validateRequiredParam("userId", userId)
validateBusinessRule(condition, ErrorCode.BUSINESS_RULE_VIOLATION, "Invalid operation")

// Response creation
createCleanResponse(data)
createCleanResponseWithHeaders(data, headers)
createPaginatedResponse(data, page, size, totalElements)

// Utilities
extractClientIp(request)
sanitizeInput(userInput)
checkRateLimit(userId, operation)
```

### **Example Usage:**

```java
@RestController
@RequestMapping("/api/public")
public class PublicController extends BaseController {
    
    @PostMapping("/contact")
    public ResponseEntity<ContactResponse> submitContact(
            @RequestBody ContactRequest request,
            HttpServletRequest httpRequest) {
        
        initializeRequestTracking();
        try {
            // Validate inputs
            validateRequiredParam("name", request.getName());
            validateEmail(request.getEmail(), "email");
            validatePhoneNumber(request.getPhone(), "phone");
            
            // Sanitize input
            String cleanMessage = sanitizeInput(request.getMessage());
            
            // Business logic
            ContactResponse response = contactService.processContactRequest(
                request.getName(), request.getEmail(), request.getPhone(), cleanMessage);
            
            // Log and return
            logRequestSuccess("submit-contact", "anonymous", response);
            return createCleanResponse(response);
            
        } finally {
            finalizeRequestTracking();
        }
    }
}
```

---

## 🔐 **BaseAuthenticationController Usage**

### **When to Use:**
- Authentication and authorization controllers
- User management endpoints
- Any controller that needs user context or role checking

### **Key Methods:**

```java
// User extraction
extractUserId(principal)
extractUserIdWithRoleCheck(principal, "USER", "ADMIN")

// Authentication validation
validateAuthenticated(principal)
validateUserRoles(userId, "ADMIN")
validateResourceAccess(userId, "portfolio", portfolioId)
validateResourceOwnership(userId, resourceOwnerId)

// Authorization checks
isAdmin(userId)
requireAdmin(userId)

// Security logging
logAuthenticationEvent(userId, "login", "success", true)
logAuthorizationEvent(userId, "portfolio:123", "read", true)
```

### **Example Usage:**

```java
@RestController
@RequestMapping("/api/auth")
public class UserController extends BaseAuthenticationController {
    
    @GetMapping("/profile/{userId}")
    public ResponseEntity<UserProfile> getUserProfile(
            @PathVariable String userId,
            Principal principal) {
        
        initializeRequestTracking();
        try {
            // Extract and validate user
            String currentUserId = extractUserId(principal);
            
            // Check ownership or admin rights
            if (!currentUserId.equals(userId)) {
                requireAdmin(currentUserId);
            }
            
            // Business logic
            UserProfile profile = userService.getUserProfile(userId);
            
            // Log and return
            logAuthorizationEvent(currentUserId, "profile:" + userId, "read", true);
            return createCleanResponse(profile);
            
        } finally {
            finalizeRequestTracking();
        }
    }
}
```

---

## ⚙️ **BaseService Usage**

### **When to Use:**
- All service layer classes
- Business logic implementation
- Data processing and transformation

### **Key Methods:**

```java
// Resilience patterns
executeWithCircuitBreaker(operation, serviceCall, fallback)
executeWithRetry(operation, serviceCall, maxRetries)
executeWithCaching(operation, parameters, serviceCall)

// Validation
validateInput(request, "createUser")
validateRequired("email", user.getEmail())
validateBusinessRule(condition, "User already exists")
validateEntityExists(user, "User", userId)

// Performance & Monitoring
executeWithLogging(operation, context, serviceCall)
recordMetrics(operation, durationMs)
getServiceMetrics()

// Event publishing
publishEvent(new UserCreatedEvent(user))
logBusinessOperation(userId, "create", "User", newUserId, "New user registration")
```

### **Example Usage:**

```java
@Service
public class UserService extends BaseService {
    
    public UserProfile createUser(CreateUserRequest request) {
        return executeWithLogging("create-user", request.getEmail(), () -> {
            // Validate input
            validateInput(request, "create-user");
            validateRequired("email", request.getEmail());
            
            // Business rule validation
            boolean emailExists = userRepository.existsByEmail(request.getEmail());
            validateBusinessRule(!emailExists, "Email already registered");
            
            // Create user with circuit breaker
            User user = executeWithCircuitBreaker(
                "create-user-db",
                () -> userRepository.save(buildUser(request)),
                () -> { throw new StrategizException(ErrorCode.SERVICE_UNAVAILABLE, "Database unavailable"); }
            );
            
            // Publish event
            publishEvent(new UserCreatedEvent(user));
            
            // Business logging
            logBusinessOperation(user.getId(), "create", "User", user.getId(), "User registration completed");
            
            return mapToProfile(user);
        });
    }
    
    public UserProfile getUserWithRetry(String userId) {
        return executeWithRetry("get-user", () -> {
            User user = userRepository.findById(userId).orElse(null);
            return validateEntityExists(user, "User", userId);
        }, 3);
    }
}
```

---

## 🚨 **Error Handling Standards**

### **Use StrategizException for ALL business errors:**

```java
// ✅ CORRECT
throw new StrategizException(ErrorCode.VALIDATION_ERROR, "Invalid email format");
throw new StrategizException(ErrorCode.RESOURCE_NOT_FOUND, "User not found");
throw new StrategizException(ErrorCode.BUSINESS_RULE_VIOLATION, "Insufficient balance");

// ❌ INCORRECT
throw new IllegalArgumentException("Invalid email");
throw new RuntimeException("User not found");
return null; // Never return null
```

### **Available ErrorCodes:**
- `VALIDATION_ERROR` - Input validation failures
- `AUTHENTICATION_ERROR` - Authentication failures
- `AUTHORIZATION_ERROR` - Authorization/permission failures
- `RESOURCE_NOT_FOUND` - Entity not found
- `BUSINESS_RULE_VIOLATION` - Business logic violations
- `TOO_MANY_REQUESTS` - Rate limiting
- `SERVICE_UNAVAILABLE` - External service issues
- `INTERNAL_ERROR` - Unexpected internal errors

---

## 📊 **Logging Standards**

### **Request Logging Pattern:**
```java
// Start tracking
initializeRequestTracking();

// Log request
logRequest("operation-name", userId, context);

// Log success
logRequestSuccess("operation-name", userId, result);

// Log sensitive operations
logSensitiveOperation(userId, "password-change", "Success");

// Clean up
finalizeRequestTracking();
```

### **Service Logging Pattern:**
```java
// Business operations
logBusinessOperation(userId, "create", "Portfolio", portfolioId, "Initial portfolio");

// Service operations with metrics
executeWithLogging("process-data", context, () -> {
    // Your business logic here
    return result;
});
```

---

## 🔧 **Migration Guide**

### **Updating Existing Controllers:**

1. **Change extends clause:**
   ```java
   // Before
   public class MyController {
   
   // After - General controller
   public class MyController extends BaseController {
   
   // After - Auth controller
   public class MyAuthController extends BaseAuthenticationController {
   ```

2. **Update exception handling:**
   ```java
   // Before
   if (email == null) {
       throw new IllegalArgumentException("Email required");
   }
   
   // After
   validateRequiredParam("email", email);
   ```

3. **Update response creation:**
   ```java
   // Before
   return ResponseEntity.ok(data);
   
   // After
   return createCleanResponse(data);
   ```

### **Updating Existing Services:**

1. **Change extends clause:**
   ```java
   // Before
   @Service
   public class MyService {
   
   // After
   @Service
   public class MyService extends BaseService {
   ```

2. **Update validation:**
   ```java
   // Before
   if (request == null) {
       throw new IllegalArgumentException("Request required");
   }
   
   // After
   validateRequired("request", request);
   validateInput(request, "my-operation");
   ```

3. **Add resilience patterns:**
   ```java
   // Before
   return expensiveOperation();
   
   // After
   return executeWithCircuitBreaker("expensive-op", 
       () -> expensiveOperation(),
       () -> getFallbackResult());
   ```

---

## ✅ **Best Practices Checklist**

### **Controllers:**
- [ ] Extends appropriate base class (BaseController vs BaseAuthenticationController)
- [ ] Uses initializeRequestTracking() / finalizeRequestTracking()
- [ ] Validates all inputs using provided methods
- [ ] Uses createCleanResponse() for all returns
- [ ] Throws StrategizException for errors
- [ ] Logs sensitive operations appropriately

### **Services:**
- [ ] Extends BaseService
- [ ] Uses validateInput() for request validation
- [ ] Uses validateBusinessRule() for business logic
- [ ] Uses executeWithLogging() for operation tracking
- [ ] Publishes domain events appropriately
- [ ] Implements circuit breaker patterns for external calls

### **General:**
- [ ] No null returns (throw exceptions instead)
- [ ] Consistent error codes using ErrorCode enum
- [ ] Proper logging at appropriate levels
- [ ] Business operations logged for audit trail
- [ ] Performance metrics collected for monitoring

---

## 🎯 **Summary**

By following these standards, you ensure:

1. **Consistency** across all controllers and services
2. **Proper error handling** with meaningful error codes
3. **Observability** through logging and metrics
4. **Resilience** through circuit breakers and retry logic
5. **Security** through input validation and sanitization
6. **Maintainability** through standardized patterns

Remember: **When in doubt, throw a StrategizException with a clear error message!** 🚀 