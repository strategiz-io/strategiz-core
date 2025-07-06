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
COPY application/ application/

# Build the application
RUN mvn clean install -DskipTests

# Second stage - runtime image
FROM eclipse-temurin:21-jre
WORKDIR /app

# Install necessary tools and Vault
RUN apt-get update && apt-get install -y \
    curl \
    bash \
    jq \
    unzip \
    && rm -rf /var/lib/apt/lists/*

# Download and install Vault
RUN curl -fsSL https://releases.hashicorp.com/vault/1.15.2/vault_1.15.2_linux_amd64.zip -o vault.zip && \
    unzip vault.zip && \
    mv vault /usr/local/bin/ && \
    rm vault.zip

# Create vault data directory
RUN mkdir -p /app/vault/data

# Copy the built JAR file from the builder stage
COPY --from=builder /app/application/target/application-1.0-SNAPSHOT.jar app.jar

# Copy Vault configuration
COPY deployment/vault-config.hcl /app/vault-config.hcl

# Copy startup script
COPY deployment/start.sh /app/start.sh
RUN chmod +x /app/start.sh

# Ensure the service listens on the port provided by Cloud Run
ENV PORT=8080

# Environment variables for Spring Boot application
ENV SPRING_PROFILES_ACTIVE=prod

# Vault configuration
ENV VAULT_ADDR=http://localhost:8200
ENV VAULT_TOKEN=root-token

# The command that will be executed when the container starts
CMD ["/app/start.sh"]
