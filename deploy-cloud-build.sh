#!/bin/bash

# Cloud Build Deployment Script for Strategiz
# This script uses Google Cloud Build to deploy without local Docker

set -e  # Exit on error

echo "===== Strategiz Cloud Build Deployment ====="
echo ""

# Configuration
PROJECT_ID="strategiz-io"
REGION="us-central1"
SERVICE_NAME="strategiz-api"
FRONTEND_DIR="../strategiz-ui"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Step 1: Check prerequisites
echo -e "${BLUE}[1/5] Checking prerequisites...${NC}"

# Check if gcloud is installed
if ! command -v gcloud &> /dev/null; then
    echo -e "${RED}Error: gcloud CLI is not installed${NC}"
    echo "Please install Google Cloud SDK: https://cloud.google.com/sdk/docs/install"
    exit 1
fi

# Check if firebase is installed
if ! command -v firebase &> /dev/null; then
    echo -e "${RED}Error: firebase CLI is not installed${NC}"
    echo "Please install Firebase CLI: npm install -g firebase-tools"
    exit 1
fi

# Check if frontend directory exists
if [ ! -d "$FRONTEND_DIR" ]; then
    echo -e "${RED}Error: Frontend directory not found at $FRONTEND_DIR${NC}"
    exit 1
fi

# Set the project
echo -e "${BLUE}Setting Google Cloud project...${NC}"
gcloud config set project $PROJECT_ID

# Step 2: Enable required APIs
echo -e "${BLUE}[2/5] Enabling required Google Cloud APIs...${NC}"
gcloud services enable cloudbuild.googleapis.com
gcloud services enable run.googleapis.com
gcloud services enable containerregistry.googleapis.com

# Step 3: Submit build to Cloud Build
echo -e "${BLUE}[3/5] Submitting build to Google Cloud Build...${NC}"
echo "This will build the Docker image in the cloud..."

gcloud builds submit --config cloudbuild.yaml .

# Step 4: Get the backend URL
echo -e "${BLUE}[4/5] Getting backend URL...${NC}"
BACKEND_URL=$(gcloud run services describe $SERVICE_NAME --region $REGION --format 'value(status.url)')
echo -e "${GREEN}Backend deployed to: $BACKEND_URL${NC}"

# Step 5: Deploy frontend to Firebase
echo -e "${BLUE}[5/5] Deploying frontend to Firebase...${NC}"

# Navigate to frontend directory
cd $FRONTEND_DIR

# Build frontend with production API URL
echo "Building frontend with API URL: $BACKEND_URL"
REACT_APP_API_URL=$BACKEND_URL npm run build

# Deploy to Firebase
firebase deploy --only hosting --project $PROJECT_ID

# Return to original directory
cd -

echo ""
echo -e "${GREEN}===== Deployment Complete! =====${NC}"
echo -e "Backend URL: ${YELLOW}$BACKEND_URL${NC}"
echo -e "Frontend URL: ${YELLOW}https://$PROJECT_ID.web.app${NC}"
echo ""
echo -e "${YELLOW}Important Notes:${NC}"
echo "1. Make sure Firebase service account is configured in the backend"
echo "2. Update OAuth redirect URIs in Google/Facebook console to include $BACKEND_URL"
echo "3. Monitor Cloud Run logs: gcloud logging read \"resource.type=cloud_run_revision\""
echo "4. The first request may be slow due to cold start (min-instances=0)"