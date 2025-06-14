# Strategiz Core Build Scripts

This directory contains scripts for building and deploying the Strategiz Core platform.

## Script Organization

### Production Scripts (Root Directory)
Production deployment scripts are located in the root `scripts/` directory:
- **TBD** - Production deployment scripts will be added here
- These scripts are designed for production environments and CI/CD pipelines

### Local Development Scripts (`local/` Directory)
Local development scripts are organized in the `scripts/local/` subdirectory for development and testing:

#### Windows Scripts (.bat)
- **build.bat** - Builds all modules in the correct dependency order
- **deploy.bat** - Deploys a previously built application locally
- **build-and-deploy.bat** - Builds and then deploys the application locally

#### Linux/macOS Scripts (.sh)
- **build.sh** - Builds all modules in the correct dependency order
- **deploy.sh** - Deploys a previously built application locally  
- **build-and-deploy.sh** - Builds and then deploys the application locally

## Usage Instructions

### Local Development

Navigate to the local scripts directory:
```bash
cd scripts/local
```

#### For Windows Users

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

#### For Linux/macOS Users

First, make the scripts executable:
```bash
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

## Build Process

The build process follows the correct Maven dependency order:

1. **Framework modules** - Core framework and API documentation
2. **Data modules** - Database models and repositories
3. **Client modules** - External API clients (exchanges, data providers)  
4. **Business modules** - Business logic and domain models
5. **Service modules** - Application services and business services
6. **API modules** - REST API controllers and endpoints
7. **Application module** - Main Spring Boot application

## Deployment

The deployment scripts:
- Start the Spring Boot application with development profile
- Use port 8080 by default
- Include all necessary classpath dependencies
- Enable hot reloading for development

## Notes

- **Local scripts** are intended for development and testing environments
- **Production scripts** (when added) will handle production deployments with appropriate security and configuration
- All scripts skip tests by default for faster builds during development
- Use `Ctrl+C` to stop the locally deployed application
