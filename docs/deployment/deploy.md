# Strategiz Core API Deployment Guide

This guide explains how to deploy the Strategiz Java backend to Firebase using Cloud Run.

## Prerequisites

1. **Google Cloud SDK** installed and configured with the Strategiz project
2. **Firebase CLI** installed and logged in
3. **Docker** installed (for local testing)
4. **Maven** installed
5. **Java 11** installed

## One-Time Setup

1. **Enable required Google Cloud APIs**:
   ```bash
   gcloud services enable cloudbuild.googleapis.com run.googleapis.com artifactregistry.googleapis.com
   ```

2. **Configure Firebase project**:
   ```bash
   firebase use strategiz-io
   ```

3. **Create Firebase hosting target for API** (separate from the main frontend):
   ```bash
   firebase target:apply hosting api api-strategiz-io
   ```

4. **Set up service account for Firebase Admin SDK**:
   - Ensure the `firebase-service-account.json` file is in the project root
   - This file should NEVER be committed to GitHub
   - The application reads this file for Firebase authentication

## Deployment Process

1. **Build and package the Spring Boot application**:
   - This compiles your Java code and creates a JAR file
   - Includes all dependencies required to run your application

2. **Build and submit Docker image to Google Container Registry**:
   - Containerizes your application for deployment
   - Makes it available for Cloud Run to deploy

3. **Deploy the container to Cloud Run**:
   - Sets up the serverless environment for your Java backend
   - Configures it to be publicly accessible

4. **Configure Firebase Hosting for API**:
   - Routes requests from your API subdomain to the Cloud Run service
   - Keeps this separate from your main frontend at strategiz.io

All these steps are automated in the `deploy.bat` script.

## Integration with Frontend

The Java backend API will be deployed to `https://api-strategiz-io.web.app` while your frontend remains at `strategiz.io`. To integrate them:

1. Configure your frontend to make API calls to the backend at `https://api-strategiz-io.web.app`
2. Ensure CORS is properly configured in your Spring Boot application to allow requests from `strategiz.io`
3. Consider setting up a custom domain like `api.strategiz.io` for a more cohesive experience

## Security Considerations

- **API Credentials**: Stored in Firebase Firestore under the `api_credentials` subcollection for each user document
- **Environment Variables**: Sensitive configuration is stored in environment variables in Cloud Run
- **Service Account**: The Firebase Admin SDK service account file is required for authentication
- **CORS**: Configured to only allow requests from trusted domains

## Running the Deployment

Simply run the `deploy.bat` script:

```bash
.\deploy.bat
```

The script will:
1. Create the Firebase hosting target (if not already created)
2. Build your Java application with Maven
3. Build and push a Docker image to Google Container Registry
4. Deploy the image to Cloud Run
5. Configure Firebase Hosting to route to your Cloud Run service

## Accessing the Deployed API

After deployment, your API will be available at:
- Primary URL: `https://api-strategiz-io.web.app`
- Direct Cloud Run URL: `https://strategiz-core-[hash].run.app`

## Troubleshooting

- **Build Failures**: Check Maven errors in console output
- **Deployment Failures**: Verify Google Cloud and Firebase CLI authentication
- **Runtime Errors**: Check Cloud Run logs via Google Cloud Console

## Local Testing

To test locally before deployment:

1. Build the Docker image:
   ```bash
   docker build -t strategiz-core .
   ```

2. Run the container:
   ```bash
   docker run -p 8080:8080 strategiz-core
   ```

3. Access at `http://localhost:8080`
