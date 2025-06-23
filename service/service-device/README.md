# Strategiz Device Service

This module provides device identity management and fingerprinting capabilities for the Strategiz platform. It implements CRUD operations for device identities, following SOLID principles, and provides APIs for both authenticated and anonymous device flows.

## Architecture

The service follows a layered architecture:

1. **Repository Layer** - Four separate CRUD repositories following Single Responsibility Principle
2. **Service Layer** - Business logic and coordination between repositories
3. **API Controllers** - REST endpoints for device registration and management
4. **Utilities** - Helper classes for device fingerprinting and identification

## Technologies & Frameworks

### Core Frameworks
- **Spring Boot** - Foundation framework for dependency injection, REST controllers, and web functionality
- **Spring Security** - Authentication and authorization for protected endpoints
- **Java Servlet API** - Access to HTTP request details for device fingerprinting

### Data Access
- **Spring Data JPA** - Repository pattern implementation for database access
- **Hibernate** - ORM for entity mapping and database operations

### JSON Processing
- **Jackson** - JSON serialization/deserialization for REST API responses

### Logging & Monitoring
- **SLF4J** - Logging facade
- **Logback** - Logging implementation

### Testing
- **JUnit** - Unit testing framework
- **Mockito** - Mocking framework for tests
- **Spring Test** - Integration testing utilities

## API Endpoints

### Anonymous Device Flow
- `POST /api/v1/device/anonymous/register` - Register a new anonymous device

### Authenticated Device Flow
- `POST /api/v1/device/authenticated/register` - Register an authenticated device
- `GET /api/v1/device/authenticated/list` - Get all devices for the authenticated user
- `DELETE /api/v1/device/authenticated/{deviceId}` - Delete a specific device
- `PUT /api/v1/device/authenticated/{deviceId}/trusted/{trusted}` - Update device trusted status

## Device Identification

The service captures various device information:

1. **HTTP Headers** - User-Agent and custom fingerprint headers
2. **User Agent Parsing** - Extracting OS, browser, and device details
3. **IP Address Tracking** - With proxy detection
4. **Client-side Fingerprinting** - Processed and stored with device identity

## Integration with Client-Side

This service is designed to work with the client-side device fingerprinting implementation that utilizes:

- **Web Crypto API** - For secure device key generation
- **Fingerprint.js** - For advanced device identification
- **Browser APIs** - For gathering device characteristics

## Usage

To use this service:

1. Ensure dependencies are configured in your Maven project
2. Set up appropriate security configuration for authenticated endpoints
3. Configure the database connection for device identity persistence
4. Implement client-side fingerprinting to send device information

## Development

To build and test the module:

```bash
# Navigate to the module directory
cd service/service-device

# Build the module
mvn clean install

# Run tests
mvn test
```
