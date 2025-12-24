# Multi-stage Dockerfile for Strategy Execution Service

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

# Copy parent POM and all modules (needed for multi-module build)
COPY pom.xml .
COPY framework/ framework/
COPY data/ data/
COPY business/ business/
COPY service/ service/
COPY client/ client/
COPY application-execution/ application-execution/
COPY proto/ proto/

# Build only the execution service (skip tests for faster build)
RUN mvn clean package -pl application-execution -am -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-jammy

# Install Python for subprocess execution
RUN apt-get update && apt-get install -y \
    python3.11 \
    python3-pip \
    && rm -rf /var/lib/apt/lists/*

# Install Python dependencies for strategy execution
RUN pip3 install --no-cache-dir \
    pandas==2.1.4 \
    numpy==1.26.2 \
    pandas-ta==0.3.14b0 \
    TA-Lib==0.4.28

WORKDIR /app

# Copy JAR from build stage
COPY --from=build /build/application-execution/target/application-execution-*.jar app.jar

# Create non-root user
RUN useradd -m -u 1000 stratexec && chown -R stratexec:stratexec /app
USER stratexec

# Expose gRPC port
EXPOSE 50051

# Expose HTTP port for health checks
EXPOSE 8081

# Run the application
ENTRYPOINT ["java", \
    "-XX:+UseG1GC", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
