# Strategiz Deployment Guide

This directory contains all deployment configurations and scripts for the Strategiz platform.

## Deployment Options

### 1. Local Development
**Location:** `../scripts/local/`

**Quick Start:**
```bash
# Build and run locally
cd scripts/local
./build-and-deploy.sh    # Linux/Mac
build-and-deploy.bat     # Windows
```

**Files:**
- `build.sh/bat` - Builds the Maven project
- `deploy.sh/bat` - Runs the JAR locally
- `build-and-deploy.sh/bat` - Does both

### 2. Google Cloud Run (Production)
**Files:** `deploy-to-cloud-run.ps1`

**Prerequisites:**
```powershell
# Set OAuth credentials
$env:AUTH_GOOGLE_CLIENT_ID = "your-google-client-id"
$env:AUTH_GOOGLE_CLIENT_SECRET = "your-google-client-secret"
$env:AUTH_FACEBOOK_CLIENT_ID = "your-facebook-client-id"
$env:AUTH_FACEBOOK_CLIENT_SECRET = "your-facebook-client-secret"
```

**Deploy:**
```powershell
./deploy-to-cloud-run.ps1
```

**What it does:**
1. Builds Docker image
2. Pushes to Google Container Registry
3. Deploys to Cloud Run with environment variables
4. Configures Firebase Hosting routing

### 3. Google Cloud Build (CI/CD)
**Files:** `cloudbuild.yaml`, `cloudbuild-simple.yaml`

**Usage:**
```bash
# Trigger build manually
gcloud builds submit --config cloudbuild.yaml

# Or use the simple version
gcloud builds submit --config cloudbuild-simple.yaml
```

### 4. Google App Engine (Alternative)
**Files:** `app.yaml`

**Deploy:**
```bash
gcloud app deploy
```

## Configuration Files

### Firebase
- `firebase.json` - Firebase hosting configuration
- Routes API calls to Cloud Run service

### Docker
- `../Dockerfile` - Main application container
- `../firebase.Dockerfile` - Firebase-specific container

### Cloud Build
- `cloudbuild.yaml` - Full CI/CD pipeline
- `cloudbuild-simple.yaml` - Simple build and deploy

## Environment Variables Required

### Authentication (OAuth)
- `AUTH_GOOGLE_CLIENT_ID` - Google OAuth client ID
- `AUTH_GOOGLE_CLIENT_SECRET` - Google OAuth client secret
- `AUTH_FACEBOOK_CLIENT_ID` - Facebook OAuth client ID
- `AUTH_FACEBOOK_CLIENT_SECRET` - Facebook OAuth client secret

### Firebase
- `GOOGLE_APPLICATION_CREDENTIALS` - Firebase service account JSON

### Spring Boot
- `SPRING_PROFILES_ACTIVE` - Set to `prod` for production

## Project Structure

```
strategiz-core/
├── deployment/              # ← All deployment files here
│   ├── app.yaml
│   ├── cloudbuild*.yaml
│   ├── deploy-to-cloud-run.ps1
│   ├── firebase.json
│   └── README.md           # ← This file
├── scripts/local/          # ← Local development
│   ├── build.sh/bat
│   ├── deploy.sh/bat
│   └── build-and-deploy.sh/bat
├── docs/                   # ← All documentation
└── service/service-monitoring/ # ← Monitoring & observability configs
```

## Quick Commands

```bash
# Local development
cd scripts/local && ./build-and-deploy.sh

# Production deployment
cd deployment && ./deploy-to-cloud-run.ps1

# CI/CD
gcloud builds submit --config deployment/cloudbuild.yaml
```

## Troubleshooting

**OAuth Errors:**
- Ensure environment variables are set
- Check Google/Facebook OAuth app configurations
- Verify redirect URIs match

**Build Failures:**
- Check Maven build logs
- Ensure Java 21 is installed
- Verify all dependencies are available

**Deployment Failures:**
- Check Google Cloud authentication
- Verify project ID and permissions
- Review Cloud Run logs

## Architecture

**Production Flow:**
1. **User** → `strategiz.io` (Frontend)
2. **API Calls** → `api-strategiz-io.web.app` (Firebase Hosting)
3. **Firebase** → Cloud Run service (Backend)
4. **Backend** → Firestore (Database)

**Local Development:**
1. **Frontend** → `localhost:3000`
2. **Backend** → `localhost:8080`
3. **Database** → Firebase Firestore (shared) 