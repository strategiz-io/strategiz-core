FROM openjdk:21-slim
WORKDIR /app

# Copy the pre-built JAR file
COPY target/*.jar app.jar

# Ensure the service listens on the port provided by Cloud Run
ENV PORT=8080

# Environment variables for Spring Boot application
ENV SPRING_PROFILES_ACTIVE=prod

# Copy Firebase credentials if available
COPY firebase-credentials.json* /app/firebase-credentials.json*

# The command that will be executed when the container starts
CMD ["java", "-jar", "app.jar", "--server.port=${PORT}"]
