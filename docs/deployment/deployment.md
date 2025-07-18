# Backend Deployment Guide - Google Cloud Run

This guide provides comprehensive instructions for deploying the Strategiz Core backend to Google Cloud Run.

## Prerequisites

### Required Tools
- **Java 21** or higher
- **Maven 3.8+**
- **Docker** installed and running
- **Google Cloud SDK** authenticated (`gcloud auth login`)
- **Firebase CLI** authenticated (`firebase login`)
- **HashiCorp Vault** (for secret management)

### Google Cloud Setup
1. Set your project:
   ```bash
   gcloud config set project strategiz-io
   ```

2. Enable required APIs:
   ```bash
   gcloud services enable cloudbuild.googleapis.com run.googleapis.com artifactregistry.googleapis.com
   ```

3. Configure Firebase project:
   ```bash
   firebase use strategiz-io
   ```

## Environment Configuration

### Vault Configuration
The application uses HashiCorp Vault for secret management. Ensure Vault is properly configured with:

- Firebase service account credentials
- OAuth provider secrets
- API keys for external services
- JWT secrets and encryption keys

### Application Properties
The `application-prod.properties` file contains production-specific configurations:
- Secure cookie settings
- Vault integration endpoints
- Production logging levels
- Performance optimizations
- CORS configuration for production domains

## Building the Application

### Local Build
```bash
# Clean build with all tests
mvn clean install

# Build without tests (faster)
mvn clean package -DskipTests

# Build with production profile
mvn clean package -P prod
```

### Docker Image Build
The application includes a Dockerfile optimized for production:

```bash
# Build Docker image
docker build -t strategiz-core .

# Test locally
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e VAULT_TOKEN=<your-token> \
  strategiz-core
```

## Deployment Process

### Quick Deployment
Use the provided deployment script:

```bash
./scripts/deployment/local/deploy.sh
```

### Manual Deployment Steps

1. **Build and push Docker image to Google Container Registry**:
   ```bash
   # Build and tag image
   docker build -t gcr.io/strategiz-io/strategiz-api .
   
   # Push to GCR
   docker push gcr.io/strategiz-io/strategiz-api
   ```

2. **Deploy to Cloud Run**:
   ```bash
   gcloud run deploy strategiz-api \
     --image gcr.io/strategiz-io/strategiz-api \
     --platform managed \
     --region us-central1 \
     --allow-unauthenticated \
     --port 8080 \
     --memory 512Mi \
     --cpu 1 \
     --set-env-vars SPRING_PROFILES_ACTIVE=prod
   ```

3. **Configure environment variables** (if using Vault):
   ```bash
   gcloud run services update strategiz-api \
     --set-env-vars VAULT_ADDR=<vault-address> \
     --set-env-vars VAULT_TOKEN=<vault-token> \
     --region us-central1
   ```

## Production Configuration

### CORS Settings
The backend is configured to accept requests from:
- `https://*.strategiz.io`
- `https://*.web.app`
- `https://*.firebaseapp.com`
- `https://*.run.app`
- `http://localhost:3000` (for local development)

### Security Configuration
- JWT tokens for authentication
- Secure cookie settings in production
- Role-based access control (USER, ADMIN)
- OAuth provider integration

### Performance Optimization
- Connection pooling for database
- Caching configuration
- Minimum instance settings to reduce cold starts
- Resource limits optimized for typical load

## Post-Deployment

### Verify Deployment
```bash
# Check service status
gcloud run services describe strategiz-api --region us-central1

# Get service URL
gcloud run services list --platform managed
```

### Access URLs
After deployment, your API will be available at:
- **Cloud Run URL**: `https://strategiz-api-[hash].us-central1.run.app`
- **Health Check**: `https://strategiz-api-[hash].us-central1.run.app/actuator/health`

## Monitoring and Logging

### View Logs
```bash
# Recent logs
gcloud logging read "resource.type=cloud_run_revision AND resource.labels.service_name=strategiz-api" \
  --limit 50 \
  --format json

# Stream logs
gcloud alpha logging tail "resource.type=cloud_run_revision AND resource.labels.service_name=strategiz-api"
```

### Metrics
Access metrics through Google Cloud Console:
- CPU utilization
- Memory usage
- Request count and latency
- Error rates

## Troubleshooting

### Common Issues

1. **Build Failures**
   - Verify Java 21 is being used: `java -version`
   - Clean Maven cache: `mvn clean`
   - Check for dependency conflicts

2. **Deployment Failures**
   - Verify Google Cloud authentication: `gcloud auth list`
   - Check project settings: `gcloud config list`
   - Ensure Docker daemon is running

3. **Runtime Errors**
   - Check Cloud Run logs for specific errors
   - Verify Vault connectivity and credentials
   - Ensure all required environment variables are set

4. **CORS Issues**
   - Verify allowed origins in `ApplicationWebConfig`
   - Check that frontend is using correct API URL
   - Ensure OPTIONS requests are handled

5. **Cold Start Issues**
   - Consider setting minimum instances:
     ```bash
     gcloud run services update strategiz-api \
       --min-instances 1 \
       --region us-central1
     ```

### Health Checks
The application exposes health endpoints:
- `/actuator/health` - Overall health status
- `/actuator/info` - Application information
- `/actuator/metrics` - Application metrics

## Rollback Procedure

If deployment issues occur:

1. **List previous revisions**:
   ```bash
   gcloud run revisions list --service strategiz-api --region us-central1
   ```

2. **Rollback to previous revision**:
   ```bash
   gcloud run services update-traffic strategiz-api \
     --to-revisions <previous-revision>=100 \
     --region us-central1
   ```

## Cost Optimization

- Cloud Run charges only for actual usage
- Configure appropriate CPU and memory limits
- Use minimum instances sparingly
- Monitor usage patterns and adjust accordingly

## Security Best Practices

1. **Never expose secrets** in environment variables
2. **Use Vault** for all sensitive configuration
3. **Regularly rotate** API keys and secrets
4. **Monitor** access logs for suspicious activity
5. **Keep dependencies** updated for security patches

## Integration with Frontend

The deployed backend integrates with the frontend by:
1. Accepting CORS requests from configured domains
2. Providing RESTful API endpoints
3. Managing authentication via JWT tokens
4. Handling WebSocket connections for real-time features

Frontend configuration should point to the Cloud Run URL:
```javascript
// In frontend environment configuration
REACT_APP_API_URL=https://strategiz-api-[hash].us-central1.run.app
```