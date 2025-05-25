# Strategiz Application Module

This module serves as the main entry point for the Strategiz Core application. It contains the application bootstrap class that initializes and starts the Spring Boot application.

## Key Components

- `Application.java`: The main class with the entry point that bootstraps the Spring Boot application
- `application.properties`: Configuration properties for the application

## Running the Application

To run the application:

```bash
mvn spring-boot:run -pl application
```

This will start the application on port 8080 as configured in the application.properties file.

## Dependencies

This module depends on all other modules in the Strategiz Core project:
- API modules
- Client modules
- Data modules
- Service modules

These dependencies are managed in the module's pom.xml file.
