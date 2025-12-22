# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with the Strategiz platform.

## Project Structure

The Strategiz platform consists of two main projects:
- **strategiz-core**: Backend Java Spring Boot application (this directory)
- **strategiz-ui**: Frontend React TypeScript application (../strategiz-ui)

## Backend (strategiz-core)

### Common Development Commands

#### Build Commands
```bash
# Full clean build (recommended for first build or after major changes)
./clean-build.sh  # Unix/Mac
clean-build.bat   # Windows

# Normal build (faster, for incremental changes)
./build.sh        # Unix/Mac
build.bat         # Windows

# Build specific module
mvn clean install -pl module-name -am
```

#### Run Commands
```bash
# Start Vault (required for local development)
vault server -dev

# Export Vault token (in new terminal)
export VAULT_TOKEN=<your-dev-token>

# Run main application
mvn spring-boot:run -pl application-api

# Application runs on http://localhost:8080
```

#### Test Commands
```bash
# Run all tests
mvn test

# Run tests for specific module
mvn test -pl module-name

# Run specific test class
mvn test -Dtest=ClassName

# Run with specific profile
mvn test -Dspring.profiles.active=test
```

#### Code Quality
```bash
# Format code (uses Spring Java Format)
mvn spring-javaformat:apply

# Check formatting
mvn spring-javaformat:validate

# Run with skip tests
mvn clean install -DskipTests
```

### High-Level Architecture

#### Module Structure
The project follows a modular monorepo architecture with six types of modules:

1. **Framework Modules** (`framework-*`): Core utilities, exceptions, constants
   - `framework-firebase`: Firebase integration utilities
   - `framework-ai`: OpenAI integration and prompt management
   - `framework-common`: Shared utilities and base classes

2. **Data Modules** (`data-*`): Repository interfaces and database entities
   - Define repository contracts
   - No implementation - uses Firebase Firestore through converters

3. **Client Modules** (`client-*`): External service integrations
   - `client-firebase`: Firestore implementation of data repositories
   - `client-vault`: HashiCorp Vault integration
   - `client-*-oauth`: Various OAuth provider integrations

4. **Business Modules** (`business-*`): Core business logic
   - Service interfaces and implementations
   - Domain models and DTOs
   - Business rules and validations

5. **Service Modules** (`service-*`): REST API endpoints
   - Controllers extending BaseController
   - Swagger/OpenAPI documentation
   - Request/response DTOs
   - Note: Recent refactor consolidated API modules into service modules

6. **Application Module** (`application-api`): Main Spring Boot application
   - Application entry point (serves all REST endpoints)
   - Configuration and properties
   - Security configuration
   - Includes service-console for admin endpoints

#### Key Architectural Patterns

1. **Base Classes**: All controllers extend `BaseController`, services extend `BaseService`
2. **Repository Pattern**: Data access through repository interfaces with Firebase implementation
3. **Service Layer**: Business logic separated from controllers
4. **DTO Pattern**: Separate request/response objects from domain models
5. **Converter Pattern**: Entity-to-DTO conversions handled by dedicated converters

#### Database Architecture
- **Primary Database**: Firebase Firestore (NoSQL)
- **Collections**: Users, Teams, Tasks, Projects, Analytics
- **Document Structure**: Nested documents with subcollections
- **ID Strategy**: Firebase auto-generated IDs

#### Security & Authentication
- **Authentication**: Firebase Auth with custom OAuth providers
- **Authorization**: Role-based (USER, ADMIN roles)
- **Secret Management**: HashiCorp Vault for all sensitive configuration
- **OAuth Providers**: Google, Facebook, Microsoft, GitHub, LinkedIn, Twitter

#### Configuration Management
- **Application Properties**: Standard Spring Boot configuration
- **Vault Integration**: All secrets stored in Vault
- **Environment Profiles**: dev, test, prod
- **Firebase Config**: Service account credentials from Vault

### Important Context

#### Recent Refactoring (from git log)
- Backend framework consolidation completed
- Vault integration for secret management added
- OAuth authentication enhancements implemented
- API modules merged into service modules

#### Required Environment Setup
1. Java 21
2. Maven 3.8+
3. Spring Boot 3.5.7
4. HashiCorp Vault (for local development)
5. Firebase project with Firestore enabled
6. Service account credentials for Firebase

#### Module Dependencies
Build order matters due to Maven dependencies:
1. Framework modules first
2. Data modules
3. Client modules
4. Business modules
5. Service modules
6. Application module last

### Common Development Tasks

#### Adding a New API Endpoint
1. Create controller in appropriate service module extending BaseController
2. Define request/response DTOs
3. Implement service logic in business module
4. Add Swagger annotations for documentation
5. Write integration tests

#### Adding a New Firebase Collection
1. Define entity in appropriate data module
2. Create repository interface extending BaseRepository
3. Implement repository in client-firebase module
4. Create converter for entity-DTO mappings
5. Add service layer in business module

#### Modifying OAuth Providers
1. OAuth implementations are in client-*-oauth modules
2. Each provider extends OAuthService interface
3. Configuration is loaded from Vault
4. Update OAuthFactory when adding new providers

### Troubleshooting

#### Build Failures
- Run clean-build script to ensure proper build order
- Check Maven settings for repository access
- Verify Java 21 is being used

#### Vault Connection Issues
- Ensure Vault is running (`vault server -dev`)
- Export VAULT_TOKEN environment variable
- Check Vault path configuration in application properties

#### Firebase Connection Issues
- Verify service account credentials in Vault
- Check Firebase project ID in configuration
- Ensure Firestore is enabled in Firebase console

#### Test Failures
- Tests may require specific Spring profiles
- Some tests need Vault running
- Check for required environment variables

## Frontend (strategiz-ui)

The Strategiz platform includes a React-based frontend application located at `../strategiz-ui` (relative to strategiz-core).

### Frontend Technology Stack

- **Core Framework**: React 19.1.0 with TypeScript
- **UI Framework**: Material-UI (MUI) v7.1.1 with Emotion for styling
- **State Management**: Redux Toolkit 2.8.2 with React-Redux 9.2.0
- **Routing**: React Router DOM 7.6.2
- **HTTP Client**: Axios 1.10.0
- **Build Tool**: Create React App 5.0.1 with CRACO
- **Testing**: Jest with React Testing Library

### Frontend Commands

```bash
# Navigate to frontend directory
cd ../strategiz-ui

# Install dependencies
npm install

# Start development server (runs on http://localhost:3000)
npm start

# Build for production
npm run build

# Build for local deployment
npm run build:local

# Run tests
npm test
```

### Frontend Architecture

The frontend follows a **feature-based architecture**:

```
src/
├── assets/                    # App-wide assets
├── components/                # Shared UI components
│   ├── layout/               # Layout components
│   └── ui/                   # Common UI utilities
├── features/                 # Feature modules
│   ├── auth/                 # Authentication
│   ├── dashboard/            # Dashboard
│   ├── portfolio/            # Portfolio management
│   ├── marketplace/          # Strategy marketplace
│   └── labs/                 # Experimental features
├── routes/                   # Application routing
├── store/                    # Redux store configuration
├── theme/                    # MUI theme configuration
└── utils/                    # Global utilities
```

### Frontend-Backend Integration

#### API Configuration
- **Development**: Proxies `/auth` requests to `http://localhost:8080`
- **Production**: Uses `REACT_APP_API_URL` environment variable
- **Base API URL**: `http://localhost:8080/api` (default)

#### Authentication Flow
The frontend supports all authentication methods provided by the backend:
1. Traditional email/password
2. TOTP (Google Authenticator, etc.)
3. SMS verification
4. Email OTP
5. Passkeys (WebAuthn)
6. OAuth providers (Google, Facebook)

### Full-Stack Development Workflow

#### Starting Both Applications
```bash
# Terminal 1: Start backend
cd strategiz-core
vault server -dev
export VAULT_TOKEN=<token>
mvn spring-boot:run -pl application-api

# Terminal 2: Start frontend
cd ../strategiz-ui
npm run dev:web
```

#### Environment Setup
1. Backend runs on http://localhost:8080
2. Frontend runs on http://localhost:3000
3. Frontend proxies API requests to backend
4. Both applications share authentication state via cookies

### Frontend Key Features

1. **Authentication System**
   - Complete multi-factor auth UI
   - Redux state management
   - Protected route guards
   - OAuth callback handling

2. **Dashboard Module**
   - Market watchlist
   - Portfolio visualization
   - TradingView widget integration
   - Real-time data updates

3. **Theme System**
   - Dark theme (default) with neon green accent
   - Light theme available
   - Responsive Material-UI components

4. **State Management**
   - Redux Toolkit with TypeScript
   - Feature-based slice organization
   - Persistent auth state
   - API integration middleware

### Deployment

#### Frontend Deployment (Firebase)
```bash
# Build for production
npm run build

# Deploy to Firebase
firebase deploy --only hosting
```

#### Backend Deployment (Google Cloud Run)
- Frontend production build points to Cloud Run URL
- Configured in `npm run build` script
- CORS and authentication handled by backend

### Common Full-Stack Tasks

#### Adding a New Feature
1. **Backend**: Create service module with controllers and business logic
2. **Frontend**: Create feature folder with components, hooks, and Redux slice
3. **Integration**: Update proxy config if new API paths needed
4. **Testing**: Write tests for both frontend and backend

#### Debugging Authentication Issues
1. Check backend logs for auth errors
2. Verify frontend Redux state in DevTools
3. Ensure cookies are being set correctly
4. Check CORS configuration for production

#### API Contract Changes
1. Update DTOs in backend service module
2. Update TypeScript types in frontend
3. Update API client in frontend services
4. Test integration thoroughly