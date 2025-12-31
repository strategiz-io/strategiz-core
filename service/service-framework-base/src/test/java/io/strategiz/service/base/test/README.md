# Service Integration Testing Framework

This directory contains the base framework for integration testing of service module REST API endpoints.

## Overview

Integration tests verify that REST API endpoints work correctly by:
- Testing actual controller methods (not mocked)
- Validating HTTP request/response handling
- Checking error responses and status codes
- Verifying request validation logic
- Testing endpoint routing and mapping

**Note**: Integration tests focus on the HTTP layer, not business logic. Business logic should be tested in service-layer unit tests.

## Architecture

```
service-framework-base/src/test/
├── java/io/strategiz/service/base/test/
│   ├── BaseIntegrationTest.java      # Base class for all integration tests
│   ├── TestConfig.java                # Test configuration beans
│   └── README.md                      # This file
└── resources/
    └── application-test.properties    # Test profile configuration
```

## Key Components

### 1. BaseIntegrationTest

Abstract base class that all integration tests extend.

**Features:**
- Spring Boot test setup with MockMvc
- HTTP request helpers (GET, POST, PUT, DELETE, PATCH)
- Authenticated request helpers (with access tokens)
- Response assertion helpers
- Test data generation utilities
- Structured logging for test steps

**Usage:**
```java
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
public class MyControllerIntegrationTest extends BaseIntegrationTest {

    @Test
    public void shouldReturnUserProfile() throws Exception {
        performGet("/v1/users/profile")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").exists());
    }
}
```

### 2. TestConfig

Provides test-specific bean configurations.

**Use cases:**
- Mock external service clients (Vault, Firebase, etc.)
- Override production beans for testing
- Configure test-specific dependencies

**Usage:**
```java
@SpringBootTest
@Import(TestConfig.class)
public class MyIntegrationTest extends BaseIntegrationTest {
    // Tests
}
```

### 3. application-test.properties

Test profile configuration loaded when `@ActiveProfiles("test")` is used.

**Features:**
- Disables external services (Vault, OAuth providers)
- Enables debug logging for tests
- Configures test database settings
- Disables security features for easier testing

## Writing Integration Tests

### Step 1: Create Test Class

Create test class in the service module under `src/test/java`:

```
service/service-auth/src/test/java/io/strategiz/service/auth/controller/
└── oauth/
    └── OAuthProviderControllerIntegrationTest.java
```

### Step 2: Extend BaseIntegrationTest

```java
package io.strategiz.service.auth.controller.oauth;

import io.strategiz.service.base.test.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = io.strategiz.application.Application.class)
@ActiveProfiles("test")
public class OAuthProviderControllerIntegrationTest extends BaseIntegrationTest {

    @Test
    public void shouldReturnAuthorizationUrl() throws Exception {
        performGet("/v1/auth/oauth/google/signup/authorization-url")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.url").exists())
            .andExpect(jsonPath("$.state").exists());
    }
}
```

### Step 3: Test HTTP Methods

**GET Requests:**
```java
// Simple GET
performGet("/v1/users/123")
    .andExpect(status().isOk());

// GET with query parameters
performGet("/v1/users", Map.of("page", "1", "size", "10"))
    .andExpect(status().isOk());

// Authenticated GET
performAuthenticatedGet("/v1/profile", accessToken)
    .andExpect(status().isOk());
```

**POST Requests:**
```java
// POST with JSON body
Map<String, Object> request = Map.of(
    "email", "test@example.com",
    "name", "Test User"
);

performPost("/v1/users", request)
    .andExpect(status().isCreated())
    .andExpect(jsonPath("$.userId").exists());

// Authenticated POST
performAuthenticatedPost("/v1/profile", accessToken, request)
    .andExpect(status().isOk());
```

**PUT/PATCH/DELETE:**
```java
// PUT request
performPut("/v1/users/123", updateRequest)
    .andExpect(status().isOk());

// PATCH request
performPatch("/v1/users/123", patchRequest)
    .andExpect(status().isOk());

// DELETE request
performDelete("/v1/users/123")
    .andExpect(status().isNoContent());

// Authenticated DELETE
performAuthenticatedDelete("/v1/users/123", accessToken)
    .andExpect(status().isNoContent());
```

### Step 4: Assert Responses

**Status Codes:**
```java
.andExpect(status().isOk())                    // 200
.andExpect(status().isCreated())               // 201
.andExpect(status().isNoContent())             // 204
.andExpect(status().isBadRequest())            // 400
.andExpect(status().isUnauthorized())          // 401
.andExpect(status().isForbidden())             // 403
.andExpect(status().isNotFound())              // 404
.andExpect(status().is3xxRedirection())        // 3xx
.andExpect(status().is4xxClientError())        // 4xx
.andExpect(status().is5xxServerError())        // 5xx
```

**JSON Path Assertions:**
```java
// Field exists
.andExpect(jsonPath("$.userId").exists())

// Field has value
.andExpect(jsonPath("$.email").value("test@example.com"))

// Field is string/boolean/number
.andExpect(jsonPath("$.name").isString())
.andExpect(jsonPath("$.active").isBoolean())
.andExpect(jsonPath("$.age").isNumber())

// Field is not empty
.andExpect(jsonPath("$.name").isNotEmpty())

// Nested fields
.andExpect(jsonPath("$.user.profile.name").exists())

// Array operations
.andExpect(jsonPath("$.users").isArray())
.andExpect(jsonPath("$.users.length()").value(5))
.andExpect(jsonPath("$.users[0].id").exists())
```

**Headers:**
```java
.andExpect(header().exists("Location"))
.andExpect(header().string("Content-Type", "application/json"))
.andExpect(content().contentType(MediaType.APPLICATION_JSON))
```

**Error Responses:**
```java
// Using helper method
andExpectError(resultActions, "INVALID_TOKEN");

// Using jsonPath
.andExpect(jsonPath("$.errorCode").value("INVALID_TOKEN"))
.andExpect(jsonPath("$.message").exists())
.andExpect(jsonPath("$.detailsUrl").exists())
```

### Step 5: Extract and Validate Response

```java
// Extract response object
MvcResult result = performGet("/v1/users/123")
    .andExpect(status().isOk())
    .andReturn();

UserResponse user = extractResponse(result, UserResponse.class);
assertEquals("test@example.com", user.getEmail());

// Extract error response
MvcResult errorResult = performGet("/v1/users/invalid")
    .andExpect(status().isNotFound())
    .andReturn();

StandardErrorResponse error = extractErrorResponse(errorResult);
assertEquals("USER_NOT_FOUND", error.getErrorCode());
```

## Test Organization with JUnit 5

Use `@Nested` classes to organize related tests:

```java
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
@DisplayName("OAuth Provider Controller Integration Tests")
public class OAuthProviderControllerIntegrationTest extends BaseIntegrationTest {

    @Nested
    @DisplayName("GET /v1/auth/oauth/{provider}/signup/authorization-url")
    class GetSignupAuthorizationUrl {

        @Test
        @DisplayName("should return authorization URL for Google")
        void shouldReturnAuthUrlForGoogle() throws Exception {
            performGet("/v1/auth/oauth/google/signup/authorization-url")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").exists());
        }

        @Test
        @DisplayName("should return 400 for invalid provider")
        void shouldReturn400ForInvalidProvider() throws Exception {
            performGet("/v1/auth/oauth/invalid/signup/authorization-url")
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /v1/auth/oauth/{provider}/signup/callback")
    class HandleSignupCallback {

        @Test
        @DisplayName("should handle missing code parameter")
        void shouldHandleMissingCode() throws Exception {
            Map<String, String> request = Map.of("state", "test");

            performPost("/v1/auth/oauth/google/signup/callback", request)
                .andExpect(status().isBadRequest());
        }
    }
}
```

## Test Data Helpers

BaseIntegrationTest provides test data generation utilities:

```java
// Generate unique test email
String email = generateTestEmail();
// Result: test-1640000000000@test.strategiz.io

// Generate unique test phone
String phone = generateTestPhone();
// Result: +15551234567

// Generate unique test string
String userId = generateTestString("user");
// Result: user-1640000000000
```

## Logging Test Steps

Use logging helpers for better test output:

```java
@Test
void shouldCreateUser() throws Exception {
    logTestStep("Create new user");

    Map<String, Object> request = Map.of(
        "email", generateTestEmail(),
        "name", "Test User"
    );

    performPost("/v1/users", request)
        .andExpect(status().isCreated());

    logAssertion("User created successfully");
}
```

**Output:**
```
>>> TEST STEP: Create new user
    Asserting: User created successfully
```

## Running Tests

### Run All Integration Tests
```bash
mvn test
```

### Run Tests for Specific Module
```bash
mvn test -pl service-auth
```

### Run Specific Test Class
```bash
mvn test -Dtest=OAuthProviderControllerIntegrationTest
```

### Run Specific Test Method
```bash
mvn test -Dtest=OAuthProviderControllerIntegrationTest#shouldReturnAuthorizationUrl
```

### Run with Test Profile
```bash
mvn test -Dspring.profiles.active=test
```

## Best Practices

### 1. Test One Endpoint Per Test
```java
// GOOD: Focused test
@Test
void shouldReturnUser() throws Exception {
    performGet("/v1/users/123")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").value("123"));
}

// BAD: Testing multiple endpoints
@Test
void shouldWorkWithAllEndpoints() throws Exception {
    performGet("/v1/users/123").andExpect(status().isOk());
    performPost("/v1/users", request).andExpect(status().isCreated());
    performDelete("/v1/users/123").andExpect(status().isNoContent());
}
```

### 2. Use Descriptive Test Names
```java
// GOOD: Clear what's being tested
@Test
void shouldReturn404WhenUserNotFound() { }

// BAD: Unclear what's being tested
@Test
void testGetUser() { }
```

### 3. Test Error Cases
```java
@Test
void shouldReturn400WhenEmailIsInvalid() throws Exception {
    Map<String, Object> request = Map.of("email", "invalid-email");

    performPost("/v1/users", request)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("INVALID_EMAIL"));
}
```

### 4. Test Authentication/Authorization
```java
@Test
void shouldReturn401WhenNotAuthenticated() throws Exception {
    performGet("/v1/profile")
        .andExpect(status().isUnauthorized());
}

@Test
void shouldReturn403WhenNotAuthorized() throws Exception {
    String userToken = "user-access-token";

    performAuthenticatedGet("/v1/admin/users", userToken)
        .andExpect(status().isForbidden());
}
```

### 5. Use @Nested for Organization
Organize related tests by endpoint or feature:

```java
@Nested
@DisplayName("User CRUD Operations")
class UserCrudOperations {

    @Nested
    @DisplayName("Create User")
    class CreateUser {
        // Create user tests
    }

    @Nested
    @DisplayName("Update User")
    class UpdateUser {
        // Update user tests
    }

    @Nested
    @DisplayName("Delete User")
    class DeleteUser {
        // Delete user tests
    }
}
```

## Testing vs E2E Tests

| Integration Tests (Spring Boot) | E2E Tests (Playwright) |
|----------------------------------|------------------------|
| Test API endpoints directly | Test from browser/frontend |
| MockMvc (no HTTP server) | Real HTTP requests |
| Fast execution (~100ms/test) | Slower (~1-5s/test) |
| Backend repository | Frontend repository |
| Test HTTP layer | Test user journeys |
| No JavaScript/UI testing | Full UI/UX testing |
| JUnit 5 | Playwright + TypeScript |

**When to use Integration Tests:**
- Testing API contract (request/response structure)
- Testing HTTP status codes
- Testing request validation
- Testing error responses
- Testing controller logic

**When to use E2E Tests:**
- Testing user journeys (signup, login, etc.)
- Testing UI interactions
- Testing cross-app flows
- Testing browser-specific features
- Testing frontend state management

## Example: Complete Integration Test

See `service-auth/src/test/java/io/strategiz/service/auth/controller/oauth/OAuthProviderControllerIntegrationTest.java` for a comprehensive example.

## Next Steps

1. Create integration tests for each controller in your service module
2. Test all endpoints (GET, POST, PUT, DELETE, PATCH)
3. Test error cases (400, 401, 403, 404, 500)
4. Test authentication and authorization
5. Test request validation
6. Run tests as part of CI/CD pipeline

## Support

For questions or issues:
- Check existing tests in service-auth for examples
- Review BaseIntegrationTest source code for available helpers
- Refer to Spring Boot testing documentation: https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing
