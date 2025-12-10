FROM eclipse-temurin:21-jre-jammy

# Set working directory
WORKDIR /app

# Copy the JAR file
COPY target/application-web-1.0-SNAPSHOT.jar app.jar

# Expose port
EXPOSE 8080

# Set production profile and Vault configuration
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xmx768m -Xms256m"

# Run the application with PORT binding
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --server.port=${PORT:-8080}"]