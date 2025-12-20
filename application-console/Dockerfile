FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY target/application-console-1.0-SNAPSHOT.jar app.jar

EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=${PORT:-8080}"]
