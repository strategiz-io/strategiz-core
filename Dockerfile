# Multi-stage build - simpler approach for multi-module project
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

# Copy everything (pom.xml files nested deeply in modules)
COPY pom.xml .
COPY framework/ framework/
COPY data/ data/
COPY client/ client/
COPY business/ business/
COPY service/ service/
COPY application-api/ application-api/
COPY batch/ batch/

# Build the application (exclude service-labs due to dependency issues)
RUN mvn clean install -DskipTests -pl '!service/service-labs'

# Runtime image
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/application-api/target/application-api-1.0-SNAPSHOT.jar app.jar
ENV PORT=8080
ENV SPRING_PROFILES_ACTIVE=prod
CMD ["java", "-jar", "app.jar"]
