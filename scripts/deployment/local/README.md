# Local Deployment Scripts

Modern build and deploy scripts for Strategiz Core with support for both Maven and Gradle build systems.

## 🚀 Quick Start

### Maven (Recommended for Current Setup)
```bash
# Full clean build and deploy
./maven-clean-build-deploy.sh

# Quick incremental build for development
./maven-quick-build-deploy.sh
```

### Gradle (For Performance-Focused Development)
```bash
# Full clean build and deploy
./gradle-clean-build-deploy.sh

# Quick incremental build for development  
./gradle-quick-build-deploy.sh
```

### Windows Users
```cmd
REM Maven
maven-clean-build-deploy.bat
maven-quick-build-deploy.bat

REM Gradle
gradle-clean-build-deploy.bat
gradle-quick-build-deploy.bat
```

## 📁 Script Overview

| Script | Purpose | Build Tool | Platform | Use Case |
|--------|---------|------------|----------|----------|
| `maven-clean-build-deploy.sh` | Complete build | Maven | Unix/macOS | Production builds, first setup |
| `maven-quick-build-deploy.sh` | Fast incremental | Maven | Unix/macOS | Development iteration |
| `gradle-clean-build-deploy.sh` | Complete build | Gradle | Unix/macOS | Performance-focused builds |
| `gradle-quick-build-deploy.sh` | Fast incremental | Gradle | Unix/macOS | Rapid development |
| `maven-clean-build-deploy.bat` | Complete build | Maven | Windows | Windows production builds |
| `gradle-clean-build-deploy.bat` | Complete build | Gradle | Windows | Windows performance builds |
| `clean-build-deploy.sh` | Legacy unified | Maven | Unix/macOS | Backward compatibility |
| `quick-build-deploy.sh` | Legacy quick | Maven | Unix/macOS | Backward compatibility |

## ⚡ Performance Comparison

| Build Type | Maven Time | Gradle Time | Speed Advantage |
|------------|------------|-------------|-----------------|
| **Clean Build** | 5-8 minutes | 3-5 minutes | 40-60% faster |
| **Incremental** | 2-3 minutes | 30-60 seconds | 70-80% faster |
| **Single File Change** | 30-60 seconds | 5-15 seconds | 75-85% faster |

## 🎯 Choosing the Right Script

### For Daily Development
- **Maven**: `maven-quick-build-deploy.sh` - Reliable, widely supported
- **Gradle**: `gradle-quick-build-deploy.sh` - Faster iteration, modern features

### For Production/CI
- **Maven**: `maven-clean-build-deploy.sh` - Stable, reproducible
- **Gradle**: `gradle-clean-build-deploy.sh` - Fast, cached builds

### For Team Onboarding
- **Start with**: `maven-clean-build-deploy.sh` - Most familiar to Java developers
- **Try later**: `gradle-clean-build-deploy.sh` - When speed becomes important

## 🔧 Environment Requirements

### Common Requirements
- ☕ Java 21+
- 🏛️ Vault server (recommended): `vault server -dev`

### Maven-Specific
- 🔨 Maven 3.8+ or Maven wrapper (`./mvnw`)
- 📦 Access to Maven Central repository

### Gradle-Specific  
- 🐘 Gradle 8.0+ or Gradle wrapper (`./gradlew`) - **Recommended**
- 🌐 Internet access for Gradle distribution and dependencies

## 📋 Pre-Build Checklist

1. **Environment Setup**
   ```bash
   # Check Java version
   java -version
   
   # Start Vault (if using secrets)
   vault server -dev
   export VAULT_TOKEN=hvs.your-dev-token
   ```

2. **Build Tool Verification**
   ```bash
   # Maven
   mvn --version
   
   # Gradle
   ./gradlew --version
   ```

3. **Memory Configuration** (if needed)
   ```bash
   # Maven
   export MAVEN_OPTS="-Xmx2g -XX:MaxMetaspaceSize=512m"
   
   # Gradle
   export GRADLE_OPTS="-Xmx2g -XX:MaxMetaspaceSize=512m"
   ```

## 🚨 Troubleshooting

### Common Issues

#### Build Fails with Memory Error
```bash
# Increase memory allocation
export MAVEN_OPTS="-Xmx4g -XX:MaxMetaspaceSize=1g"
export GRADLE_OPTS="-Xmx4g -XX:MaxMetaspaceSize=1g"
```

#### Port 8080 Already in Use
```bash
# Kill existing process
lsof -ti:8080 | xargs kill -9

# Or use different port
java -jar app.jar --server.port=8081
```

#### Vault Connection Issues
```bash
# Restart Vault
vault server -dev

# Set token
export VAULT_TOKEN=hvs.your-new-token
```

#### Dependencies Not Found
```bash
# Maven: Clear local repository
rm -rf ~/.m2/repository/io/strategiz

# Gradle: Clear cache
./gradlew clean --build-cache
```

### Build Tool Specific

#### Maven Issues
```bash
# Dependency conflicts
mvn dependency:tree -Dverbose

# Force update snapshots
mvn clean install -U

# Skip tests if needed
mvn clean install -DskipTests
```

#### Gradle Issues
```bash
# Dependency insights
./gradlew dependencyInsight --dependency spring-boot

# Refresh dependencies
./gradlew build --refresh-dependencies

# Build without cache
./gradlew build --no-build-cache
```

## 🔄 Migration Between Build Tools

### Trying Both Systems
```bash
# Build with Maven
./maven-clean-build-deploy.sh

# Compare with Gradle  
./gradle-clean-build-deploy.sh

# Use whichever works better for your workflow
```

### Performance Testing
```bash
# Time Maven build
time ./maven-clean-build-deploy.sh

# Time Gradle build  
time ./gradle-clean-build-deploy.sh
```

## 📊 Feature Matrix

| Feature | Maven Scripts | Gradle Scripts |
|---------|---------------|----------------|
| **Colored Output** | ✅ | ✅ |
| **Environment Checks** | ✅ | ✅ |
| **Build Verification** | ✅ | ✅ |
| **Error Handling** | ✅ | ✅ |
| **Cross-Platform** | ✅ | ✅ |
| **Parallel Builds** | ✅ | ✅ |
| **Build Cache** | ❌ | ✅ |
| **Incremental Compilation** | Basic | Advanced |
| **Dependency Insights** | ✅ | ✅ |
| **Build Scans** | ❌ | ✅ |

## 🎯 Best Practices

### Development Workflow
1. **First run**: Use clean build script
2. **Daily work**: Use quick build script  
3. **Before commits**: Run clean build
4. **CI/CD**: Use clean build with tests

### Team Collaboration
1. **Document choice**: Update team docs with preferred build tool
2. **Consistent environment**: Share MAVEN_OPTS/GRADLE_OPTS
3. **IDE integration**: Configure IDE to use same build tool
4. **CI alignment**: Match local and CI build tools

## 📚 Related Documentation

- [Build Tools Comparison](./BUILD-TOOLS-COMPARISON.md) - Detailed Maven vs Gradle analysis
- [Architecture Overview](../../../docs/architecture/overview.md) - Project structure
- [Development Guide](../../../docs/development/developer-guide.md) - Development workflow
- [Deployment Guide](../../../docs/deployment/deployment.md) - Production deployment