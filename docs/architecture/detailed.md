# Strategiz Architectural Guidelines

This document outlines the architectural principles, coding standards, naming conventions, and development philosophy for the Strategiz platform.

## Table of Contents
- [Architectural Principles](#architectural-principles)
- [Module Structure](#module-structure)
- [Naming Conventions](#naming-conventions)
- [Development Philosophy](#development-philosophy)
- [API Integration Standards](#api-integration-standards)
- [Security Guidelines](#security-guidelines)
- [Build Configuration](#build-configuration)

## Architectural Principles

The Strategiz platform follows a modular, clean architecture with clear separation of concerns. The diagram below illustrates our layered architecture and module dependencies:

![Strategiz Core Architecture](images/strategiz-architecture.png)

### Architecture Diagram Access

**View the diagram in the following ways:**

1. **Static Image**: The static PNG diagram is located at [docs/images/strategiz-architecture.png](images/strategiz-architecture.png)

2. **Interactive HTML Diagram**: When the application is running, you can access the interactive HTML architecture diagram at:
   - Direct URL: `http://localhost:8080/docs/strategiz_architecture_diagram.html`
   - API Endpoint: `http://localhost:8080/api/docs/architecture`

3. **Editable Source**: The editable source file is available at [docs/images/strategiz-architecture.drawio](images/strategiz-architecture.drawio)

The interactive HTML diagram provides a comprehensive visual representation of the system's components, their relationships, and the underlying architectural principles.

The following principles guide our development:

1. **Separation of Concerns**: Each module has a single, well-defined responsibility
2. **Dependency Rule**: Dependencies flow from high-level policies to low-level details, never the reverse
3. **SOLID Principles**: All code should follow the SOLID principles:
   - Single Responsibility
   - Open/Closed
   - Liskov Substitution
   - Interface Segregation
   - Dependency Inversion
4. **Real Data Only**: All implementations must use real API data and real user credentials, never mocks or test data
5. **Transparency**: Data shown to users should be as close as possible to what comes from the APIs

## Architecture Diagram

> **TODO**: Replace this placeholder with a proper architecture diagram.
> 
> Create a detailed architecture diagram using [draw.io](https://app.diagrams.net/) or another diagramming tool that shows:
> 1. The layered architecture (API, Service, Data, Client layers)
> 2. Module dependencies and relationships
> 3. External service integrations
> 4. Key design patterns implementation
>
> Save the diagram as both the source file (e.g., `strategiz-architecture.drawio`) and as an image file (PNG or SVG) in the `docs/images/` directory.
>
> Once created, update this section to reference the diagram:
>
> ```markdown
> ![Strategiz Architecture Diagram](./images/strategiz-architecture.png)
> ```

The Strategiz architecture follows a classic layered design with strict dependency rules:

1. **API Layer** (Controllers) - Handles HTTP requests and depends only on Service layer
2. **Service Layer** (Business Logic) - Implements application features and depends on Data/Client layers
3. **Data Layer** (Repositories) - Manages data persistence using Firestore
4. **Client Layer** (API Clients) - Integrates with external services like exchange APIs
5. **External Services** - Third-party systems like Firebase, Kraken, Binance, etc.

Dependencies flow downward only, with higher layers depending on lower layers, never the reverse.

## Technology Stack

### API Layer
- **Spring MVC**: For REST controllers and request handling
- **Spring Security**: For authentication and authorization
- **JSON Processing**: Jackson for serialization/deserialization
- **Validation**: Hibernate Validator for input validation
- **Documentation**: SpringDoc OpenAPI for API documentation

### Service Layer
- **Spring Core**: For dependency injection and application context
- **Spring Boot**: For configuration and application bootstrapping
- **Transaction Management**: Spring Transaction for transaction boundaries
- **Caching**: Spring Cache for caching service results

### Data Layer
- **Firestore**: For document database storage
- **Spring Data**: For repository abstractions when applicable
- **Custom Repositories**: For direct Firestore API interaction
- **Entity Mapping**: For converting between domain and persistence models

### Client Layer
- **HTTP Client**: Apache HttpClient 5 for API interactions
- **Firebase Admin SDK**: For Firebase operations
- **JSON Processing**: Jackson for parsing API responses
- **Circuit Breaker**: For handling external service failures

### Cross-Cutting Concerns
- **Logging**: SLF4J with Logback
- **Configuration**: Spring Boot properties and profiles
- **Error Handling**: Global exception handlers
- **Metrics**: Micrometer for application metrics

## Module Structure

The codebase is organized into a hierarchy of modules:

### API Modules (`api-*`)
- Contain only REST controllers and API-specific code
- Handle HTTP requests, authentication, and input validation
- Depend only on their corresponding service modules
- Located in the `api/` directory
- Package structure:
  - `io.strategiz.api.<module-name>.controller`: API controllers
  - `io.strategiz.api.<module-name>.config`: API configuration
  - `io.strategiz.api.<module-name>.model`: API models and DTOs

### Service Modules (`service-*`)
- Contain business logic and service implementations
- Implement the core functionality of the application
- May depend on data modules and client modules
- Located in the `service/` directory
- Package structure:
  - `io.strategiz.service.<module-name>`: Service implementations
  - `io.strategiz.service.<module-name>.model`: Service-specific models

### Data Modules (`data-*`)
- Handle data persistence and database interactions
- Contain repositories, entities, and data access objects
- Located in the `data/` directory
- Package structure:
  - `io.strategiz.data.<module-name>`: Repositories and data access objects
  - `io.strategiz.data.<module-name>.model`: Entity models

### Client Modules (`client-*`)
- Provide integration with external services and APIs
- Implement HTTP clients and API wrappers
- Located in the `client/` directory
- Package structure:
  - `io.strategiz.client.<module-name>`: Client implementations
  - `io.strategiz.client.<module-name>.model`: Client-specific models

### Dependency Flow
The dependency flow follows a strict hierarchy:
```
API Modules → Service Modules → Data Modules → Client Modules
```

## Design Patterns Implementation

The Strategiz Core application implements several Gang of Four (GoF) design patterns to ensure maintainability, flexibility, and adherence to SOLID principles:

### Creational Patterns

#### Factory Method
- **Implementation**: `RepositoryFactory` provides methods to create appropriate repository instances
- **Purpose**: Abstracts repository creation and allows for different implementations
- **Example Usage**: Authentication repositories that may have different implementations (Firestore, JDBC, etc.)
- **SOLID Principles**: Supports Open/Closed principle by allowing new repository types without modifying existing code

#### Builder
- **Implementation**: `ResponseBuilder` classes for constructing complex API responses
- **Purpose**: Provides a step-by-step way to construct complex objects
- **Example Usage**: `RiskAnalysisResponseBuilder`, `MarketSentimentResponseBuilder`
- **SOLID Principles**: Enforces Single Responsibility by separating object construction from its representation

### Structural Patterns

#### Adapter
- **Implementation**: Exchange API client adapters that convert between different API formats
- **Purpose**: Converts the interface of external APIs to a common interface used in our system
- **Example Usage**: `KrakenApiAdapter`, `BinanceApiAdapter`, `CoinbaseApiAdapter`
- **SOLID Principles**: Supports Interface Segregation by providing specific interfaces tailored to needs

#### Facade
- **Implementation**: Service layer acts as a facade over more complex subsystems
- **Purpose**: Provides a simplified interface to a complex subsystem
- **Example Usage**: `PortfolioService` providing a unified interface over multiple exchange APIs
- **SOLID Principles**: Reduces coupling, following Dependency Inversion principle

#### Proxy
- **Implementation**: Security proxy for controlling access to sensitive resources
- **Purpose**: Controls access to the original object, adding security checks
- **Example Usage**: `SecuredDocumentStorage` providing access control over `DocumentStorageService`
- **SOLID Principles**: Adds functionality without changing the original object (Open/Closed)

### Behavioral Patterns

#### Strategy
- **Implementation**: Different strategies for authenticating users
- **Purpose**: Defines a family of algorithms, encapsulates each one, and makes them interchangeable
- **Example Usage**: `FirebaseAuthStrategy`, `PasskeyAuthStrategy`
- **SOLID Principles**: Follows Open/Closed by allowing new strategies without modifying existing code

#### Observer
- **Implementation**: Event-based communication between components
- **Purpose**: Defines a one-to-many dependency between objects
- **Example Usage**: `UserEventListener` reacting to user authentication events
- **SOLID Principles**: Reduces coupling between components

#### Command
- **Implementation**: Encapsulates requests as objects
- **Purpose**: Parameterizes clients with different requests and supports operations like undo
- **Example Usage**: `OrderCommand` for executing exchange orders
- **SOLID Principles**: Separates the object that invokes the operation from the one that knows how to perform it

## SOLID Principles Implementation

### Single Responsibility Principle
- **Implementation**: Each module, class, and method has one reason to change
- **Example**: Separating API controllers from service logic and data access
- **Benefits**: Easier maintenance, testing, and debugging

### Open/Closed Principle
- **Implementation**: Classes are open for extension but closed for modification
- **Example**: Using interfaces for repositories allows different implementations without changing dependent code
- **Benefits**: Reduces risk when adding new functionality

### Liskov Substitution Principle
- **Implementation**: Subtypes must be substitutable for their base types
- **Example**: All repository implementations can be used interchangeably through their interfaces
- **Benefits**: Ensures consistent behavior and promotes interface-based programming

### Interface Segregation Principle
- **Implementation**: Clients should not be forced to depend on interfaces they do not use
- **Example**: Specific repository interfaces for different entity types rather than one general interface
- **Benefits**: Prevents bloated interfaces and reduces coupling

### Dependency Inversion Principle
- **Implementation**: High-level modules should not depend on low-level modules; both should depend on abstractions
- **Example**: Service classes depend on repository interfaces, not concrete implementations
- **Benefits**: Decouples components and enables easier testing through mocking

## Naming Conventions

### Class Naming Conventions

- **Repository Classes**: Must end with `Repository`
  - Example: `PortfolioCredentialsRepository`, `FirestoreSessionRepository`
  
- **Service Classes**: Must end with `Service`
  - Example: `DeviceIdentityService`, `PasskeyService`
  
- **Controller Classes**: Must end with `Controller`
  - Example: `AuthController`, `DeviceIdentityController`
  
- **Builder Classes**: Use `Builder` suffix for classes that construct complex objects
  - Example: `UserBuilder`, `ResponseBuilder`
  
- **Mapper Classes**: Use `Mapper` suffix for classes that transform between object types
  - Example: `EntityDtoMapper`, `UserProfileMapper`
  
- **Provider Classes**: Use `Provider` suffix for classes that supply/provide objects
  - Example: `AuthenticationProvider`, `ExchangeDataProvider`
  
- **Factory Classes**: Use `Factory` suffix for classes that create different types of objects
  - Example: `RepositoryFactory`, `ServiceFactory`

### Package Naming Conventions

- All packages follow the `io.strategiz.<module-type>.<module-name>` pattern
- Sub-packages should be organized by function (controller, service, repository, etc.)
- Test packages should mirror the main package structure with a `.test` suffix

### Method Naming Conventions

- **Repository Methods**:
  - `findBy*`: For retrieval operations
  - `save*`: For persistence operations
  - `delete*`: For removal operations
  
- **Service Methods**:
  - Use verb-noun naming (e.g., `createUser`, `validateCredentials`)
  - Should express business actions
  
- **Controller Methods**:
  - Should align with HTTP methods (`get*`, `create*`, `update*`, `delete*`)
  - Should clearly indicate the resource being acted upon

## Development Philosophy

1. **Real Data Over Mocks**: Always use real API data and real user credentials
   - NEVER use mock responses, test data, dummy data, or simulated data
   - All implementations must connect to actual APIs using real credentials
   - Show raw API data for transparency and debugging purposes

2. **API Transparency**: Maintain transparency in API responses
   - Provide access to raw API data through dedicated endpoints
   - Implement minimal transformations when necessary
   - Document any data transformations that do occur

3. **Scalability**: Design for scalability and future expansion
   - Design modular APIs that can evolve independently
   - Create flexible data models that can accommodate new fields
   - Implement versioned APIs to maintain backward compatibility

4. **Modularity**: Maintain strict module boundaries
   - Higher-level modules should not be affected by changes in lower-level modules
   - Business logic should be isolated from presentation and data access concerns
   - Each module should have a single responsibility

5. **Clean API Design**: Prioritize clean, consistent API design
   - Follow RESTful principles consistently
   - Use descriptive resource naming
   - Provide comprehensive API documentation

## API Integration Standards

### API Endpoint Design

1. **RESTful Design**: All APIs should follow RESTful principles
   - Use appropriate HTTP methods (GET, POST, PUT, DELETE)
   - Design resource-oriented URLs
   - Use consistent response formats

2. **Response Format**: All API responses should use a consistent format
   - Use a standard `ApiResponse<T>` wrapper for all responses
   - Include status, message, and data fields
   - Use proper HTTP status codes

3. **Error Handling**: Implement consistent error handling
   - Use appropriate HTTP status codes
   - Provide clear error messages
   - Include helpful details when appropriate

### API Integration Requirements

1. **Real Data Only**: All API integrations must use real data
   - Connect to actual exchange APIs
   - Use real user credentials
   - Do not use mocks or simulated responses

2. **Raw Data Access**: Provide access to raw, unmodified API responses
   - Create admin endpoints for viewing raw data
   - Preserve all fields from the original API response
   - Do not transform data unnecessarily

3. **Security**: Implement secure API integration
   - Store API keys securely
   - Use server-side API requests
   - Implement rate limiting and other protection measures

## Security Guidelines

1. **Credential Storage**: Store credentials securely
   - Use encryption for sensitive data
   - Store API keys in Firebase Firestore with encryption
   - Never expose credentials to the client side

2. **Authentication**: Implement secure authentication
   - Use Firebase Authentication
   - Implement proper session management
   - Use secure protocols (HTTPS, OAuth)

3. **Authorization**: Implement proper authorization
   - Restrict admin pages to users with admin privileges
   - Implement role-based access control
   - Validate permissions for all sensitive operations

## Build Configuration

### Maven Configuration

1. **Module Dependencies**: Configure module dependencies properly
   - Each module should declare its own dependencies
   - Avoid transitive dependencies when possible
   - Use dependency management for version control

2. **Plugin Configuration**: Configure plugins appropriately
   - Library modules should skip Spring Boot repackaging:
   ```xml
   <build>
       <plugins>
           <plugin>
               <groupId>org.springframework.boot</groupId>
               <artifactId>spring-boot-maven-plugin</artifactId>
               <configuration>
                   <skip>true</skip>
               </configuration>
           </plugin>
       </plugins>
   </build>
   ```

3. **Version Management**: Use consistent version management
   - Define versions in the parent POM
   - Use Maven properties for version numbers
   - Maintain compatible versions across modules
