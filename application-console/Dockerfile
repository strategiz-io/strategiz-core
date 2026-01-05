# Multi-stage build for Strategiz Console API
# This Dockerfile expects the JAR to be pre-built by Maven in Cloud Build

FROM eclipse-temurin:21-jre
WORKDIR /app

# Download OpenTelemetry Java agent for automatic instrumentation
ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.11.0/opentelemetry-javaagent.jar /app/opentelemetry-javaagent.jar
RUN chmod 644 /app/opentelemetry-javaagent.jar

# Copy the JAR file (built by Cloud Build Maven step)
COPY application-console/target/application-console-1.0-SNAPSHOT.jar app.jar

# Environment configuration
ENV PORT=8080
# Enable both prod and scheduler profiles for batch job scheduling
ENV SPRING_PROFILES_ACTIVE=prod,scheduler
# Higher memory for batch job processing
ENV JAVA_OPTS="-Xmx3g -Xms512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Expose port
EXPOSE 8080

# Attach OpenTelemetry agent with -javaagent flag
CMD ["sh", "-c", "java $JAVA_OPTS -javaagent:/app/opentelemetry-javaagent.jar -jar app.jar"]
