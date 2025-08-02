FROM openjdk:21-jdk-slim

# Set working directory
WORKDIR /app

# Copy the JAR file
COPY target/application-1.0-SNAPSHOT.jar app.jar

# Expose port
EXPOSE 8080

# Set production profile
ENV SPRING_PROFILES_ACTIVE=prod

# Run the application with PORT binding
ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=${PORT:-8080}"]