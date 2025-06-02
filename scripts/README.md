# Strategiz Core Build Scripts

This directory contains scripts for building and deploying the Strategiz Core platform.

## Script Organization

### Windows Scripts (.bat)
- **build.bat** - Builds all modules in the correct dependency order
- **deploy.bat** - Deploys a previously built application
- **build-and-deploy.bat** - Builds and then deploys the application

### Linux/macOS Scripts (.sh)
- **build.sh** - Builds all modules in the correct dependency order
- **deploy.sh** - Deploys a previously built application
- **build-and-deploy.sh** - Builds and then deploys the application

## Usage Instructions

### For Windows Users

1. **Build only:**
   ```
   .\build.bat
   ```

2. **Deploy only** (requires previous build):
   ```
   .\deploy.bat
   ```

3. **Build and deploy:**
   ```
   .\build-and-deploy.bat
   ```

### For Linux/macOS Users

First, make the scripts executable:
```
chmod +x *.sh
```

Then:

1. **Build only:**
   ```
   ./build.sh
   ```

2. **Deploy only** (requires previous build):
   ```
   ./deploy.sh
   ```

3. **Build and deploy:**
   ```
   ./build-and-deploy.sh
   ```

## Build Order

The build scripts follow the correct module dependency order:
1. Framework modules
2. Data modules
3. Client modules
4. Service modules
5. API modules
6. Application module

This ensures all dependencies are properly resolved during the build process.
