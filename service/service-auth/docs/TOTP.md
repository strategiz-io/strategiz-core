# TOTP (Time-based One-Time Password) Authentication

> **API Documentation**: [Swagger UI](http://localhost:8080/swagger-ui.html)  
> **Controllers**: `TotpRegistrationController`, `TotpAuthenticationController`  
> **Service Module**: `service-auth`

## Overview
TOTP (Time-based One-Time Password) is a secure, two-factor authentication method supported by Strategiz. It allows users to register an authenticator app (such as Google Authenticator or Authy) and use time-based codes for sign-in and sensitive actions.

## Detailed Flow Diagram

📊 **[View Interactive Diagram](./TOTP-Flow-Diagram.drawio)** - Open with [draw.io](https://app.diagrams.net)

### Overview
The detailed flow diagram shows the complete TOTP authentication system including:

**Registration Flow (Setup):**
1. **User Interaction**: User clicks "Setup Authenticator" 
2. **API Call**: Frontend → `POST /setup/initialize`
3. **Backend Processing**: Controller → Service → Database validation
4. **Secret Generation**: Create Base32 secret + QR code URI
5. **User Scanning**: User scans QR code with authenticator app
6. **Completion**: User enters first TOTP code via `POST /setup/complete`
7. **Verification**: Backend verifies code and saves `TotpAuthenticationMethod`
8. **Database Storage**: Encrypted secret stored in user's `authenticationMethods` array

**Authentication Flow (Sign-In):**
1. **User Input**: User enters email + TOTP code from app
2. **API Call**: Frontend → `POST /authenticate`
3. **Backend Verification**: Controller → Service → Database lookup
4. **Code Validation**: Verify 6-digit TOTP code against stored secret
5. **Session Creation**: Update `lastVerifiedAt` and generate user session
6. **Authentication Success**: User logged in and redirected to dashboard

**Error Handling:**
- User Not Found
- Invalid TOTP Code  
- Expired Session Token
- TOTP Already Enabled

### Component Interactions

| Layer | Components | Responsibilities |
|-------|------------|------------------|
| **Frontend** | React Components, Services | User interface, API calls |
| **Controller** | `TotpRegistrationController`, `TotpAuthenticationController` | Request handling, validation |
| **Service** | `TotpRegistrationService`, `TotpAuthenticationService`, `BaseTotpService` | Business logic, TOTP operations |
| **Repository** | `UserRepository` | Database operations |
| **Database** | User documents with `authenticationMethods` | Persistent storage |

### Working with the Flow Diagram

**Viewing the Diagram:**
1. Download the `TOTP-Flow-Diagram.drawio` file
2. Open [draw.io](https://app.diagrams.net) in your browser
3. Import the `.drawio` file
4. View the interactive, color-coded flow diagram

**Editing the Diagram:**
- **Green boxes**: User interactions and actions
- **Blue boxes**: Frontend components and API calls  
- **Yellow boxes**: Backend controllers
- **Red boxes**: Service layer and business logic
- **Purple cylinders**: Database operations
- **Red boxes (Error section)**: Error handling scenarios

**Color Coding Legend:**
- 🟢 **User Layer**: User actions and responses
- 🔵 **Frontend Layer**: React components, API clients
- 🟡 **Controller Layer**: Spring Boot REST controllers
- 🔴 **Service Layer**: Business logic and TOTP operations
- 🟣 **Data Layer**: Repository and database operations
- ❌ **Error Handling**: Exception scenarios and error responses

## Backend Controller Endpoints

| Endpoint                                 | Method | Controller                | Description                                 |
|------------------------------------------|--------|---------------------------|---------------------------------------------|
| `/auth/totp/setup/initialize`           | POST   | TotpRegistrationController | Initialize TOTP setup and generate QR code  |
| `/auth/totp/setup/complete`             | POST   | TotpRegistrationController | Complete TOTP setup by verifying first code |
| `/auth/totp/setup/status/{userId}`      | GET    | TotpRegistrationController | Check if TOTP is enabled for a user         |
| `/auth/totp/setup/disable/{userId}`     | POST   | TotpRegistrationController | Disable TOTP for a user                     |
| `/auth/totp/authenticate`               | POST   | TotpAuthenticationController | Authenticate user with TOTP code           |
| `/auth/totp/verify`                     | POST   | TotpAuthenticationController | Verify TOTP code (MFA scenarios)           |

## User Flow

### 1. Registration (Setup)
- User requests TOTP setup (`/setup/initialize`)
- Backend generates a secret and returns a QR code URI
- User scans QR code with authenticator app
- User submits first TOTP code to `/setup/complete`
- Backend verifies and activates TOTP for the user

### 2. Authentication (Sign-In)
- User enters email and TOTP code
- Frontend calls `/auth/totp/authenticate`
- Backend verifies code and returns user session

### 3. Status Check
- Frontend or backend can check if TOTP is enabled for a user via `/setup/status/{userId}`

## HTTP API Specifications

### 1. Initialize TOTP Setup
**Controller**: `TotpRegistrationController.initializeSetup()`

```http
POST /auth/totp/setup/initialize HTTP/1.1
Content-Type: application/json
Accept: application/json
Host: localhost:8080
```

**Request Body**:
```json
{
  "userId": "alice@example.com"
}
```

**Response**: `200 OK`
```json
{
  "success": true,
  "qrCodeUri": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA...",
  "userId": "alice@example.com",
  "message": "TOTP setup initialized successfully"
}
```

### 2. Complete TOTP Setup
**Controller**: `TotpRegistrationController.completeRegistration()`

```http
POST /auth/totp/setup/complete HTTP/1.1
Content-Type: application/json
Accept: application/json
Host: localhost:8080
```

**Request Body**:
```json
{
  "userId": "alice@example.com",
  "sessionToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "code": "123456"
}
```

**Response**: `200 OK`
```json
{
  "completed": true,
  "userId": "alice@example.com",
  "message": "TOTP enabled successfully"
}
```

### 3. Authenticate with TOTP
**Controller**: `TotpAuthenticationController.authenticate()`

```http
POST /auth/totp/authenticate HTTP/1.1
Content-Type: application/json
Accept: application/json
Host: localhost:8080
```

**Request Body**:
```json
{
  "userId": "alice@example.com",
  "code": "654321"
}
```

**Response**: `200 OK`
```json
{
  "success": true,
  "message": "TOTP authentication successful",
  "userId": "alice@example.com"
}
```

### 4. Check TOTP Status
**Controller**: `TotpRegistrationController.checkStatus()`

```http
GET /auth/totp/setup/status/alice@example.com HTTP/1.1
Accept: application/json
Host: localhost:8080
```

**Response**: `200 OK`
```json
{
  "enabled": true,
  "userId": "alice@example.com"
}
```

### 5. Verify TOTP Code (MFA)
**Controller**: `TotpAuthenticationController.verify()`

```http
POST /auth/totp/verify HTTP/1.1
Content-Type: application/json
Accept: application/json
Host: localhost:8080
```

**Request Body**:
```json
{
  "userId": "alice@example.com",
  "code": "789012"
}
```

**Response**: `200 OK`
```json
{
  "verified": true,
  "userId": "alice@example.com"
}
```

### 6. Disable TOTP
**Controller**: `TotpRegistrationController.disableTotp()`

```http
POST /auth/totp/setup/disable/alice@example.com HTTP/1.1
Accept: application/json
Host: localhost:8080
```

**Response**: `200 OK`
```json
{
  "disabled": true,
  "userId": "alice@example.com",
  "message": "TOTP disabled successfully"
}
```

## Error Responses

All endpoints follow the global error handling pattern:

**4xx Client Errors**:
```json
{
  "code": "VALIDATION_FAILED",
  "message": "Invalid TOTP code",
  "developerMessage": "The provided TOTP code is invalid or expired",
  "moreInfo": "https://docs.strategiz.io/errors/validation_failed"
}
```

**5xx Server Errors**:
```json
{
  "code": "INTERNAL_ERROR",
  "message": "An unexpected error occurred. Please contact support with the trace ID from response headers.",
  "developerMessage": "Exception: ...",
  "moreInfo": "https://docs.strategiz.io/errors/server/internal-error"
}
```

## Technical Implementation

### Data Model Structure
```java
// User document in database
public class User {
    private String userId;
    private UserProfile profile;
    private List<AuthenticationMethod> authenticationMethods; // ← TOTP stored here
    // ... audit fields
}

// TOTP authentication method
public class TotpAuthenticationMethod extends AuthenticationMethod {
    private String secret; // Encrypted TOTP secret (Base32)
    // Inherits: id, type="TOTP", name, lastVerifiedAt, audit fields
}
```

### Service Architecture
- **BaseTotpService**: Shared TOTP logic (QR generation, code verification)
- **TotpRegistrationService**: Handles setup and configuration
- **TotpAuthenticationService**: Handles authentication and verification
- **UserRepository**: Database operations for user and auth method persistence

### Request/Response Models
- **TotpRegistrationRequest**: `record(String userId)`
- **TotpAuthenticationRequest**: `record(String userId, String code)`

## Security & Compliance

### Encryption
- **TOTP secrets are encrypted at rest** using application-level encryption
- **QR codes contain temporary display data** and are not persisted
- **Session tokens** are used for setup completion workflow

### Audit Trail
All TOTP operations are tracked with:
- `createdBy`, `createdAt`: Initial setup
- `modifiedBy`, `modifiedAt`: Configuration changes  
- `lastVerifiedAt`: Last successful authentication
- `version`: Optimistic locking for concurrent updates

### Rate Limiting
- Implement rate limiting at API Gateway level
- Backend services include brute-force protection logic
- TOTP codes have natural time-based expiration (30-second windows)

## Development & Extension

### Adding New Authentication Methods
1. Extend `AuthenticationMethod` model
2. Create dedicated service classes extending base patterns
3. Implement corresponding controller endpoints
4. Update frontend clients and services

### Configuration
- TOTP settings: 6-digit codes, 30-second intervals, SHA1 algorithm
- QR code generation uses ZXing library
- Compatible with Google Authenticator, Authy, and RFC 6238 compliant apps

---

*For more details, see the service code and data models in the repository.* 