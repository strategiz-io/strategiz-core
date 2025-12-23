# Multi-stage build with dependency caching optimization
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

# Step 1: Copy only pom.xml files (changes rarely)
COPY pom.xml .
COPY framework/pom.xml framework/pom.xml
COPY data/pom.xml data/pom.xml
COPY client/pom.xml client/pom.xml
COPY business/pom.xml business/pom.xml
COPY service/pom.xml service/pom.xml
COPY application-api/pom.xml application-api/pom.xml
COPY batch/pom.xml batch/pom.xml

# Step 2: Download dependencies (cached unless pom.xml changes)
RUN mvn dependency:go-offline -DskipTests

# Step 3: Copy source code (changes frequently)
COPY framework/ framework/
COPY data/ data/
COPY client/ client/
COPY business/ business/
COPY service/ service/
COPY application-api/ application-api/
COPY batch/ batch/

# Step 4: Build application (only runs if source changed)
RUN mvn clean install -DskipTests -o

# Runtime image (unchanged)
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/application-api/target/application-api-1.0-SNAPSHOT.jar app.jar
ENV PORT=8080
ENV SPRING_PROFILES_ACTIVE=prod
CMD ["java", "-jar", "app.jar"]
