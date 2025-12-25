# SonarQube Deployment for Strategiz

This directory contains the configuration for deploying SonarQube Community Edition to Google Cloud Run.

## Architecture

- **Platform**: Google Cloud Run (serverless)
- **Image**: `sonarqube:community-latest`
- **Database**: Embedded H2 (for simplicity, can upgrade to Cloud SQL PostgreSQL later)
- **Storage**: Persistent disk via Cloud Run volumes (configured separately)
- **Region**: us-east1
- **Cost**: ~$0-5/month (scales to zero when not in use)

## Initial Deployment

### 1. Build and Deploy SonarQube

```bash
# From strategiz-core root directory
gcloud builds submit \
  --config=deployment/sonarqube/cloudbuild-sonarqube.yaml \
  --project=strategiz-io
```

This will:
- Build the SonarQube Docker image
- Push to Google Container Registry
- Deploy to Cloud Run at `https://strategiz-sonarqube-<hash>.run.app`

### 2. Configure SonarQube

After deployment, access the SonarQube web UI:

```bash
# Get the Cloud Run URL
gcloud run services describe strategiz-sonarqube \
  --region=us-east1 \
  --format='value(status.url)'
```

**Initial login:**
- Username: `admin`
- Password: `admin`
- **IMPORTANT**: Change the password immediately!

### 3. Generate API Token

1. Log in to SonarQube web UI
2. Go to **My Account** → **Security**
3. Generate a new token (name: `strategiz-api`)
4. Copy the token (you won't see it again!)

### 4. Store Secrets in Vault

```bash
# Set Vault address and authenticate
export VAULT_ADDR=https://strategiz-vault-43628135674.us-east1.run.app
export VAULT_TOKEN=<your-vault-token>

# Get SonarQube URL
SONARQUBE_URL=$(gcloud run services describe strategiz-sonarqube \
  --region=us-east1 \
  --format='value(status.url)')

# Store SonarQube configuration in Vault
vault kv put secret/strategiz/sonarqube \
  url="$SONARQUBE_URL" \
  token="<paste-your-token-here>" \
  project-key="strategiz-io_strategiz-core"
```

### 5. Create SonarQube Project

In the SonarQube web UI:
1. Click **Create Project** → **Manually**
2. Project key: `strategiz-io_strategiz-core`
3. Display name: `Strategiz Core`
4. Main branch: `main`

### 6. Run Initial Analysis

```bash
# From strategiz-core root directory
mvn clean verify sonar:sonar \
  -Dsonar.projectKey=strategiz-io_strategiz-core \
  -Dsonar.host.url=$SONARQUBE_URL \
  -Dsonar.login=<your-sonarqube-token>
```

This will analyze the codebase and upload results to SonarQube.

## Configuration Files

- **Dockerfile**: SonarQube container configuration for Cloud Run
- **cloudbuild-sonarqube.yaml**: Cloud Build configuration for deployment
- **README.md**: This file

## Upgrading to Cloud SQL PostgreSQL (Optional)

For production with persistent data across deployments:

1. Create Cloud SQL PostgreSQL instance
2. Update Cloud Run environment variables:
   ```bash
   gcloud run services update strategiz-sonarqube \
     --region=us-east1 \
     --set-env-vars="SONAR_JDBC_URL=jdbc:postgresql://<cloud-sql-ip>:5432/sonarqube" \
     --set-env-vars="SONAR_JDBC_USERNAME=sonarqube" \
     --set-env-vars="SONAR_JDBC_PASSWORD=<password>"
   ```

## Monitoring

Check SonarQube logs:
```bash
gcloud run services logs read strategiz-sonarqube \
  --region=us-east1 \
  --limit=50
```

Check service status:
```bash
gcloud run services describe strategiz-sonarqube \
  --region=us-east1
```

## Cost Optimization

SonarQube on Cloud Run scales to zero when not in use:
- **Active usage**: ~$0.10/hour (2 vCPU, 2GB RAM)
- **Idle**: $0 (scales to zero after 15 minutes of inactivity)
- **Storage**: ~$0.10/GB/month (H2 database file)

Typical monthly cost: **$0-5** (depending on analysis frequency)

## Troubleshooting

### SonarQube won't start
- Check memory: SonarQube requires at least 2GB RAM
- Check logs for Java heap errors
- Increase memory in cloudbuild-sonarqube.yaml if needed

### Analysis fails
- Verify token is valid
- Check project key matches SonarQube project
- Ensure Maven build succeeds before running analysis

### Data loss after redeployment
- H2 embedded database is ephemeral
- Upgrade to Cloud SQL PostgreSQL for persistence
- Or use Cloud Run volumes (requires additional configuration)
