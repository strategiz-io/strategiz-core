# Multi-stage build - first stage builds the application
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

# Copy pom.xml files and source code
COPY pom.xml .
COPY framework/ framework/
COPY data/ data/
COPY client/ client/
COPY business/ business/
COPY service/ service/
COPY application-core/ application-core/
COPY application-console/ application-console/
COPY batch/ batch/

# Build the application
RUN mvn clean install -DskipTests

# Second stage - runtime image
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the built JAR file from the builder stage (core API)
COPY --from=builder /app/application-core/target/application-core-1.0-SNAPSHOT.jar app.jar

# Ensure the service listens on the port provided by Cloud Run
ENV PORT=8080

# Environment variables for Spring Boot application
ENV SPRING_PROFILES_ACTIVE=prod

# The command that will be executed when the container starts
CMD ["java", "-jar", "app.jar"]
