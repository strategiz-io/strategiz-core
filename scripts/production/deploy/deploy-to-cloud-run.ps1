#!/usr/bin/env pwsh

# Script to deploy strategiz-core to Google Cloud Run
$PROJECT_ID = "strategiz-io"
$REGION = "us-central1"
$SERVICE_NAME = "strategiz-core"
$IMAGE_NAME = "gcr.io/$PROJECT_ID/$SERVICE_NAME"

Write-Host "===== Deploying strategiz-core to Google Cloud Run ====="
Write-Host "Project: $PROJECT_ID"
Write-Host "Region: $REGION"
Write-Host "Service: $SERVICE_NAME"
Write-Host "Image: $IMAGE_NAME"

# Check if Firebase credentials exist
if (-not (Test-Path "firebase-credentials.json")) {
    Write-Host "ERROR: firebase-credentials.json not found" -ForegroundColor Red
    Write-Host "Please download your Firebase Admin SDK credentials from:" -ForegroundColor Yellow
    Write-Host "https://console.firebase.google.com/project/$PROJECT_ID/settings/serviceaccounts/adminsdk" -ForegroundColor Yellow
    Write-Host "Save the file as firebase-credentials.json in this directory." -ForegroundColor Yellow
    exit 1
}

# 1. Build the Docker image
Write-Host "`n[1/4] Building Docker image..." -ForegroundColor Cyan
docker build -t $IMAGE_NAME .

# 2. Configure Docker to use gcloud credentials
Write-Host "`n[2/4] Configuring Docker to use gcloud credentials..." -ForegroundColor Cyan
gcloud auth configure-docker -q

# 3. Push the image to Container Registry
Write-Host "`n[3/4] Pushing image to Container Registry..." -ForegroundColor Cyan
docker push $IMAGE_NAME

# 4. Deploy to Cloud Run with OAuth environment variables
Write-Host "`n[4/4] Deploying to Cloud Run..." -ForegroundColor Cyan
gcloud run deploy $SERVICE_NAME `
    --image $IMAGE_NAME `
    --platform managed `
    --region $REGION `
    --allow-unauthenticated `
    --memory 512Mi `
    --cpu 1 `
    --min-instances 0 `
    --max-instances 1 `
    --set-env-vars "GOOGLE_APPLICATION_CREDENTIALS=/app/firebase-credentials.json,SPRING_PROFILES_ACTIVE=prod,AUTH_GOOGLE_CLIENT_ID=$env:AUTH_GOOGLE_CLIENT_ID,AUTH_GOOGLE_CLIENT_SECRET=$env:AUTH_GOOGLE_CLIENT_SECRET,AUTH_FACEBOOK_CLIENT_ID=$env:AUTH_FACEBOOK_CLIENT_ID,AUTH_FACEBOOK_CLIENT_SECRET=$env:AUTH_FACEBOOK_CLIENT_SECRET"

Write-Host "`n===== Deployment Complete =====" -ForegroundColor Green
Write-Host "Your service is now available at the URL shown above."
Write-Host "This is running with min-instances=0, so the first request may be slow."
Write-Host "Subsequent requests will be much faster."
Write-Host "`nIMPORTANT: Make sure to set OAuth environment variables before running this script:" -ForegroundColor Yellow
Write-Host "  `$env:AUTH_GOOGLE_CLIENT_ID = 'your-google-client-id'" -ForegroundColor Yellow
Write-Host "  `$env:AUTH_GOOGLE_CLIENT_SECRET = 'your-google-client-secret'" -ForegroundColor Yellow
Write-Host "  `$env:AUTH_FACEBOOK_CLIENT_ID = 'your-facebook-client-id'" -ForegroundColor Yellow
Write-Host "  `$env:AUTH_FACEBOOK_CLIENT_SECRET = 'your-facebook-client-secret'" -ForegroundColor Yellow
