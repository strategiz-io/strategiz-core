# 🚀 Strategiz Deployment Hub

Centralized deployment scripts for **local development** and **production deployment** of the Strategiz Core backend.

## 📁 Directory Structure

```
scripts/deployment/
├── local/                          # 💻 LOCAL DEVELOPMENT
│   ├── build.sh/bat               # Maven build in dependency order
│   ├── deploy.sh/bat              # Start local JAR application
│   ├── build-and-deploy.sh/bat    # Full local build + run cycle
│   └── start-vault-local.sh       # Local Vault setup
├── production/                     # ☁️ PRODUCTION DEPLOYMENT
│   ├── deploy-to-cloud-run.ps1    # Google Cloud Run deployment
│   ├── cloudbuild.yaml            # Cloud Build configuration
│   ├── cloudbuild-simple.yaml     # Simplified Cloud Build
│   ├── app.yaml                   # App Engine configuration
│   ├── firebase.json              # Firebase hosting config
│   ├── start.sh                   # Production startup script
│   ├── vault-config.hcl           # Vault configuration
│   └── README*.md                 # Production deployment guides
└── README.md                      # This file
```

## 💻 Local Development

### Quick Start
```bash
# Full build and run cycle
cd scripts/deployment/local
./build-and-deploy.sh

# Or run individually
./build.sh     # Build all modules in correct order
./deploy.sh    # Run the application locally
```

### Windows Support
```cmd
REM Full build and run cycle
cd scripts\deployment\local
build-and-deploy.bat

REM Or run individually  
build.bat      REM Build all modules
deploy.bat     REM Run the application
```

### Local Scripts Details

**`build.sh/bat`** - Maven Build Pipeline
- Builds all modules in correct dependency order:
  1. Framework modules (exception, logging, secrets, api-docs)
  2. Data modules (data-base)
  3. Business modules (all business-* modules)
  4. Service modules (all service-* modules)  
  5. API modules (all api-* modules)
  6. Application module (final executable JAR)
- Uses `-DskipTests` for faster builds
- Stops on first build failure

**`deploy.sh/bat`** - Local Application Runner
- Checks for built application JAR
- Starts Spring Boot application with `dev` profile
- Runs on `http://localhost:8080`
- Press Ctrl+C to stop

**`build-and-deploy.sh/bat`** - Complete Cycle
- Runs build script followed by deploy script
- One-command local development workflow

**`start-vault-local.sh`** - Vault Development Setup
- Starts local HashiCorp Vault instance
- Configures secrets for local development

## ☁️ Production Deployment

### Google Cloud Run (Recommended)
```powershell
cd scripts/deployment/production
./deploy-to-cloud-run.ps1
```

**Prerequisites:**
- Google Cloud SDK installed and authenticated
- Firebase Admin SDK credentials (`firebase-credentials.json`)
- Project ID: `strategiz-io`
- Region: `us-central1`

### Google Cloud Build
```bash
# From project root
gcloud builds submit --config scripts/deployment/production/cloudbuild.yaml

# Or simplified version
gcloud builds submit --config scripts/deployment/production/cloudbuild-simple.yaml
```

### Firebase Hosting (Frontend)
```bash
cd scripts/deployment/production
firebase deploy --config firebase.json
```

### Production Scripts Details

**`deploy-to-cloud-run.ps1`** - Cloud Run Deployment
- PowerShell script for complete Cloud Run deployment
- Builds Docker image and deploys to Google Cloud Run
- Configures environment variables and service settings
- Includes health checks and scaling configuration

**`cloudbuild.yaml`** - Complete Build Pipeline
- Multi-stage Docker build
- Runs tests and creates optimized production image
- Deploys to Cloud Run automatically

**`start.sh`** - Production Startup
- Production application startup script
- Configures production environment variables
- Used within Docker container

## 🏗️ Architecture & Environment Flow

### Local Development Flow
```
Developer → local/build.sh → Maven Build → local/deploy.sh → localhost:8080
```

### Production Deployment Flow  
```
Developer → production/deploy-to-cloud-run.ps1 → Docker Build → Cloud Run → api-strategiz-io.web.app
```

### Environment Configuration
- **Local**: Uses `application-dev.properties` and local Firebase
- **Production**: Uses environment variables and production Firebase

## 🔧 Configuration

### Local Environment Variables
```bash
# Set in your shell or IDE
export SPRING_PROFILES_ACTIVE=dev
export FIREBASE_PROJECT_ID=strategiz-io
```

### Production Environment Variables (Cloud Run)
- `SPRING_PROFILES_ACTIVE=prod`  
- `FIREBASE_PROJECT_ID=strategiz-io`
- `GOOGLE_APPLICATION_CREDENTIALS=/app/firebase-credentials.json`
- Database connection strings
- OAuth client secrets

## 🛠️ Troubleshooting

### Local Build Issues
```bash
# Clean all modules and rebuild
cd scripts/deployment/local
./build.sh

# Check for missing dependencies
mvn dependency:tree
```

### Local Runtime Issues
```bash
# Check application logs
tail -f logs/application.log

# Verify JAR exists
ls -la ../../../application/target/application-1.0-SNAPSHOT.jar
```

### Production Deployment Issues
```bash
# Check Cloud Run logs
gcloud logs read --service=strategiz-core --limit=50

# Check build logs
gcloud builds list --limit=10
```

### Common Issues
- **Build failures**: Check Java 21 is installed and JAVA_HOME is set
- **Missing JAR**: Run build script first
- **Port conflicts**: Check if port 8080 is already in use
- **Firebase auth**: Ensure `firebase-credentials.json` exists in production folder

## 📚 Additional Resources

- **Production Setup**: See `production/README.md` for detailed production setup
- **Vault Setup**: See `production/README-EMBEDDED-VAULT.md` for secrets management
- **API Documentation**: Access Swagger UI at `/swagger-ui/index.html` after starting
- **Health Checks**: Monitor application at `/actuator/health`

## 🎯 Quick Reference

| Task | Local Command | Production Command |
|------|---------------|-------------------|
| **Build Only** | `./local/build.sh` | `gcloud builds submit` |
| **Deploy Only** | `./local/deploy.sh` | `./production/deploy-to-cloud-run.ps1` |
| **Full Cycle** | `./local/build-and-deploy.sh` | CI/CD Pipeline |
| **Check Status** | `curl localhost:8080/actuator/health` | `gcloud run services list` |

---

**🎉 Happy Deploying!** 

For issues or questions, check the troubleshooting section above or refer to the individual script documentation.
