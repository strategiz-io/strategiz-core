# Multi-stage build for Strategiz Console API
# Build stage: Maven compile
# Runtime stage: JRE with OpenTelemetry agent

FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

# Copy all source (multi-module project - Maven needs parent POM with all modules)
COPY pom.xml .
COPY proto/ proto/
COPY framework/ framework/
COPY data/ data/
COPY client/ client/
COPY business/ business/
COPY service/ service/
COPY batch/ batch/
COPY application-api/ application-api/
COPY application-console/ application-console/

# Build the console application (targeted build for console module and dependencies)
RUN mvn clean install -Pdocker -pl application-console -am -Dmaven.test.skip=true -Dcheckstyle.skip=true -Dspotbugs.skip=true -Dpmd.skip=true && \
    rm -rf /root/.m2/repository

# Runtime image
FROM eclipse-temurin:21-jre
WORKDIR /app

# Download OpenTelemetry Java agent for automatic instrumentation
ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.11.0/opentelemetry-javaagent.jar /app/opentelemetry-javaagent.jar
RUN chmod 644 /app/opentelemetry-javaagent.jar

# Copy the JAR from builder stage
COPY --from=builder /app/application-console/target/application-console-1.0-SNAPSHOT.jar app.jar

# Environment configuration
ENV PORT=8080
# Enable both prod and scheduler profiles for batch job scheduling
ENV SPRING_PROFILES_ACTIVE=prod,scheduler
# Higher memory for batch job processing (3Gi heap for 4Gi container)
# CRITICAL: Set JVM timezone to UTC to prevent EST offset corruption in timestamps
ENV JAVA_OPTS="-Duser.timezone=UTC -Xmx3g -Xms512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Expose port
EXPOSE 8080

# Attach OpenTelemetry agent with -javaagent flag
CMD ["sh", "-c", "java $JAVA_OPTS -javaagent:/app/opentelemetry-javaagent.jar -jar app.jar"]
