# Gradle Migration Guide

This project now supports both Maven and Gradle build systems running in parallel. This allows for a gradual migration from Maven to Gradle.

## Current Status

- ✅ All modules have Gradle build files generated
- ✅ Gradle wrapper installed (version 8.7)
- ✅ Root build configuration complete
- ✅ Parallel build structure established

## Building with Gradle

```bash
# Build entire project
./gradlew clean build

# Build without tests
./gradlew clean build -x test

# Build specific module
./gradlew :service:service-auth:build

# Run application
./gradlew :application:bootRun
```

## Building with Maven (existing)

```bash
# Build entire project
./mvnw clean install

# Build without tests
./mvnw clean install -DskipTests

# Run application
mvn spring-boot:run -pl application
```

## Comparing Builds

Use the comparison script to verify both build systems produce identical artifacts:

```bash
./compare-builds.sh
```

## Migration Notes

1. **Dependencies**: All dependencies have been automatically converted from pom.xml files. Review and adjust as needed.

2. **Spring Boot**: Modules with Spring Boot plugins have been configured appropriately.

3. **Module Structure**: The Gradle structure mirrors the Maven multi-module layout exactly.

4. **Gradual Migration**: You can continue using Maven while testing Gradle builds. Once confident, switch to Gradle exclusively.

## Next Steps

1. Review generated build.gradle.kts files for accuracy
2. Test individual module builds
3. Verify application runs correctly with Gradle
4. Update CI/CD pipelines to support Gradle
5. Eventually remove Maven files once migration is complete