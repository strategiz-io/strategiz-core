# Local Build and Deploy Scripts

Modern, efficient build and deploy scripts for Strategiz Core local development.

## Available Scripts

### 🚀 Main Scripts

#### `clean-build-deploy.sh` / `clean-build-deploy.bat`
**Complete clean build and deploy for production-ready builds**

- Uses Maven reactor for optimal dependency resolution
- Includes pre-build environment checks
- Provides colored output and progress indicators
- Builds all modules in correct dependency order
- Verifies build artifacts
- Optional smoke testing
- Optimized JVM settings for runtime

**Usage:**
```bash
# Unix/macOS
./clean-build-deploy.sh

# Windows
clean-build-deploy.bat
```

#### `quick-build-deploy.sh`
**Fast incremental build for development iteration**

- Skips clean phase for faster builds
- Parallel compilation
- Minimal output for speed
- Ideal for quick testing during development

**Usage:**
```bash
./quick-build-deploy.sh
```

### 📋 Legacy Scripts (Still Available)

- `build-and-deploy.sh` - Original comprehensive build script
- `build.sh` - Step-by-step module building
- `deploy.sh` - Application deployment only

## Key Features

### ✨ Modern Improvements

1. **Maven Reactor Usage**: Leverages Maven's dependency resolution instead of manual module ordering
2. **Parallel Builds**: Uses `-T 1C` for parallel compilation
3. **Colored Output**: Better visual feedback with status indicators
4. **Environment Checks**: Validates Java, Maven, and Vault availability
5. **JVM Optimization**: Tuned memory settings for better performance
6. **Error Handling**: Proper exit codes and error messages

### 🔧 Environment Setup

#### Required
- Java 21+
- Maven 3.8+

#### Optional but Recommended
- HashiCorp Vault (for secrets management)
  ```bash
  vault server -dev
  export VAULT_TOKEN=hvs.your-dev-token
  ```

### 📊 Performance Comparison

| Script | Clean Build | Incremental | Parallel | Best For |
|--------|-------------|-------------|----------|----------|
| `clean-build-deploy.sh` | ✅ | ❌ | ✅ | Production builds, CI/CD |
| `quick-build-deploy.sh` | ❌ | ✅ | ✅ | Development iteration |
| Legacy `build.sh` | ✅ | ❌ | ❌ | Debugging specific modules |

### 🎯 Usage Recommendations

#### First Time Setup
```bash
./clean-build-deploy.sh
```

#### Development Workflow
```bash
# Make code changes
./quick-build-deploy.sh

# After major changes or dependency updates
./clean-build-deploy.sh
```

#### Troubleshooting Builds
```bash
# If quick build fails, run clean build
./clean-build-deploy.sh

# For module-specific issues, use legacy
./build.sh
```

### 🚨 Common Issues

#### Build Failures
1. **OutOfMemoryError**: Increase `MAVEN_OPTS` memory settings
2. **Module not found**: Run clean build to ensure proper dependency resolution
3. **Vault connection**: Ensure Vault is running if using secret management

#### Runtime Issues
1. **Port conflicts**: Check if port 8080 is available
2. **Database connections**: Verify Firebase/Firestore configuration
3. **Missing secrets**: Ensure Vault is configured with required secrets

### 🔗 Integration

These scripts work seamlessly with:
- **Frontend Development**: Start backend with these scripts, frontend separately
- **IDE Integration**: Can be run from IDE terminals
- **Docker Development**: Use as base for containerized builds
- **CI/CD Pipelines**: Adapt for automated deployment scenarios

### 📝 Environment Variables

| Variable | Purpose | Example |
|----------|---------|---------|
| `VAULT_TOKEN` | Vault authentication | `hvs.AbCdEf123456` |
| `MAVEN_OPTS` | Maven JVM settings | `-Xmx2g -XX:MaxMetaspaceSize=512m` |
| `SPRING_PROFILES_ACTIVE` | Spring profile | `dev`, `test`, `prod` |

### 🛠 Customization

To customize these scripts for your environment:

1. **Memory Settings**: Adjust `-Xmx` values based on available RAM
2. **Profiles**: Change `spring.profiles.active` for different environments  
3. **Build Options**: Add/remove Maven flags as needed
4. **Logging**: Modify logging levels for debugging

### 📚 Related Documentation

- [Architecture Overview](../../../docs/architecture/overview.md)
- [Development Guide](../../../docs/development/developer-guide.md)
- [Deployment Guide](../../../docs/deployment/deployment.md)
- [Secret Management](../../../docs/security/secrets-management.md)