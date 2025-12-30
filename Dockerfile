# Multi-stage build - simpler approach for multi-module project
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

# Copy everything (pom.xml files nested deeply in modules)
COPY pom.xml .
COPY proto/ proto/
COPY framework/ framework/
COPY data/ data/
COPY client/ client/
COPY business/ business/
COPY service/ service/
COPY application-api/ application-api/
COPY batch/ batch/

# Build the application and clean up in one layer to reduce snapshot size
RUN mvn clean install -Dmaven.test.skip=true -Dcheckstyle.skip=true -Dspotbugs.skip=true -Dpmd.skip=true && \
    rm -rf /root/.m2/repository

# Runtime image
FROM eclipse-temurin:21-jre
WORKDIR /app

# Download OpenTelemetry Java agent for automatic instrumentation
ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.11.0/opentelemetry-javaagent.jar /app/opentelemetry-javaagent.jar
RUN chmod 644 /app/opentelemetry-javaagent.jar

COPY --from=builder /app/application-api/target/application-api-1.0-SNAPSHOT.jar app.jar
ENV PORT=8080
ENV SPRING_PROFILES_ACTIVE=prod

# Attach OpenTelemetry agent with -javaagent flag
CMD ["java", "-javaagent:/app/opentelemetry-javaagent.jar", "-jar", "app.jar"]
