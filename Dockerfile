FROM maven:3.8-openjdk-11 as build
WORKDIR /app
COPY pom.xml .
# Download dependencies separately to leverage Docker cache
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

FROM openjdk:11-jre-slim
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Add a health check
HEALTHCHECK --interval=30s --timeout=3s CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

# Ensure the service listens on the port provided by Cloud Run
ENV PORT=8080

# Environment variables for Spring Boot application
ENV SPRING_PROFILES_ACTIVE=prod

# Copy Firebase credentials if available
COPY firebase-credentials.json* /app/firebase-credentials.json*

# The command that will be executed when the container starts
CMD ["java", "-jar", "app.jar", "--server.port=${PORT}"]
