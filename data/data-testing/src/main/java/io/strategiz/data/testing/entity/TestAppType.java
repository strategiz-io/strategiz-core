package io.strategiz.data.testing.entity;

/**
 * Types of applications in the test hierarchy
 */
public enum TestAppType {
    /**
     * Frontend application (React/TypeScript)
     * Examples: web, console, auth apps
     */
    FRONTEND,

    /**
     * Backend application (Spring Boot/Java)
     * Examples: application-api, application-console
     */
    BACKEND,

    /**
     * Microservice (Python, Node.js, etc.)
     * Examples: application-strategy-execution (Python gRPC)
     */
    MICROSERVICE
}
