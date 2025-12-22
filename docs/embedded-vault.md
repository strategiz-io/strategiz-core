# Embedded Vault Setup

This document explains how the embedded Vault setup works and how to use it for secret management.

## Overview

The application now runs HashiCorp Vault embedded within the same container as the Spring Boot application. This approach provides:

- **Cost-effective**: No additional Cloud Run service costs
- **Simplified deployment**: Single container deployment  
- **Automatic secret management**: Vault starts automatically with the application
- **Development consistency**: Same secret management in dev and production

## How It Works

### Container Setup

1. **Vault Installation**: The Dockerfile installs Vault binary alongside the Java runtime
2. **Startup Process**: The `start.sh` script handles the startup sequence:
   - Starts Vault server in background
   - Waits for Vault to be ready
   - Initializes Vault if needed (first run)
   - Unseals Vault automatically
   - Populates initial secrets from environment variables
   - Starts the Spring Boot application

### Secret Management

The application uses a hybrid approach:

1. **Vault Integration**: When Vault is available, OAuth credentials are loaded from Vault
2. **Fallback**: If secrets aren't in Vault, falls back to environment variables
3. **Automatic Population**: Environment variables are automatically stored in Vault on first run

## Configuration

### Vault Configuration

Vault is configured with:
- **Storage**: File-based storage in `/app/vault/data`
- **Address**: `http://localhost:8200`
- **Authentication**: Root token (generated on first run)
- **Secrets Engine**: KV v2 at `secret/strategiz/`

### Spring Boot Configuration

```properties
# Vault connection
spring.cloud.vault.uri=http://localhost:8200
spring.cloud.vault.token=root-token

# Vault settings
strategiz.vault.enabled=true
strategiz.vault.secrets-path=secret/strategiz
```

## Secret Structure

OAuth credentials are stored in Vault with this structure:

```
secret/strategiz/
├── oauth/
│   ├── google/
│   │   ├── client_id
│   │   └── client_secret
│   └── facebook/
│       ├── client_id
│       └── client_secret
```

## Development Workflow

### Local Development

1. **Environment Variables**: Set OAuth credentials as environment variables:
   ```bash
   export AUTH_GOOGLE_CLIENT_ID=your_google_client_id
   export AUTH_GOOGLE_CLIENT_SECRET=your_google_client_secret
   export AUTH_FACEBOOK_CLIENT_ID=your_facebook_client_id
   export AUTH_FACEBOOK_CLIENT_SECRET=your_facebook_client_secret
   ```

2. **Run Application**: Start the application normally:
   ```bash
   mvn spring-boot:run
   ```

3. **Automatic Setup**: On first run, Vault will:
   - Initialize itself
   - Store environment variables as secrets
   - Use secrets for subsequent runs

### Production Deployment

1. **Set Environment Variables** in Cloud Run:
   ```bash
   gcloud run deploy strategiz-core \
     --set-env-vars="AUTH_GOOGLE_CLIENT_ID=your_google_client_id" \
     --set-env-vars="AUTH_GOOGLE_CLIENT_SECRET=your_google_client_secret" \
     --set-env-vars="AUTH_FACEBOOK_CLIENT_ID=your_facebook_client_id" \
     --set-env-vars="AUTH_FACEBOOK_CLIENT_SECRET=your_facebook_client_secret"
   ```

2. **Vault Persistence**: Vault data persists in the container's file system. For production, consider:
   - Using persistent volumes for Cloud Run when available
   - Regular backups of Vault data
   - Monitoring Vault health

## Managing Secrets

### Adding New Secrets

You can add secrets programmatically using the `SecretManager` interface:

```java
@Autowired
private SecretManager secretManager;

// Store a new secret
secretManager.storeSecret("oauth.twitter.client-id", "your_twitter_client_id");
secretManager.storeSecret("oauth.twitter.client-secret", "your_twitter_client_secret");
```

### Retrieving Secrets

```java
// Get a secret with fallback
String clientId = secretManager.getSecret("oauth.google.client-id", "fallback_value");

// Check if secret exists
boolean exists = secretManager.hasSecret("oauth.google.client-id");
```

### Vault CLI Access

For debugging, you can access Vault CLI within the container:

```bash
# Get container ID
docker ps

# Access container
docker exec -it <container_id> /bin/bash

# Set Vault address and token
export VAULT_ADDR=http://localhost:8200
export VAULT_TOKEN=root-token

# List secrets
vault kv list secret/strategiz/oauth/

# Get a secret
vault kv get secret/strategiz/oauth/google
```

## Troubleshooting

### Common Issues

1. **Vault Not Starting**: Check container logs for Vault initialization errors
2. **Secrets Not Loading**: Verify environment variables are set correctly
3. **Authentication Issues**: Ensure OAuth credentials are valid

### Log Analysis

The application logs will show:
- Vault startup process
- Secret loading from Vault vs environment variables
- Any errors in secret retrieval

### Health Checks

The application includes health checks for:
- Vault connectivity
- Secret availability
- OAuth provider configuration

## Security Considerations

### Development
- Vault runs in dev mode for simplicity
- Root token is used for authentication
- TLS is disabled for local development

### Production
- Vault data is stored in container file system
- Consider implementing proper Vault authentication
- Monitor access logs
- Regular security updates

## Migration from Environment Variables

The system automatically migrates from environment variables to Vault:

1. **First Run**: Environment variables are detected and stored in Vault
2. **Subsequent Runs**: Secrets are loaded from Vault
3. **Fallback**: If Vault is unavailable, falls back to environment variables

This ensures a smooth transition and fallback capabilities. 