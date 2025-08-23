# Vault Setup for Strategiz Core

## Overview

Strategiz Core uses HashiCorp Vault for secure management of OAuth credentials and other secrets. This setup is **consistent across all environments** to minimize configuration drift and bugs.

## Required Environment Variables

**ALWAYS REQUIRED** (both development and production):
- `VAULT_TOKEN` - Your Vault authentication token
- `VAULT_ADDR` - Vault server address (defaults to http://localhost:8200)

## Local Development Setup

### 1. Start Vault Dev Server

```bash
# Start Vault in development mode
vault server -dev

# In a new terminal, note the Root Token displayed
# Example output: Root Token: hvs.CAESIJaNtF...
```

### 2. Set Environment Variables

```bash
# Set Vault address (if not using default)
export VAULT_ADDR='http://localhost:8200'

# Set your Vault token (use the Root Token from step 1)
export VAULT_TOKEN='hvs.CAESIJaNtF...'  # Replace with your actual token
```

### 3. Configure OAuth Secrets in Vault

```bash
# Add Google OAuth credentials (for user authentication)
vault kv put secret/strategiz/oauth/google \
  client-id="your-google-client-id" \
  client-secret="your-google-client-secret"

# Add Facebook OAuth credentials (for user authentication)
vault kv put secret/strategiz/oauth/facebook \
  client-id="your-facebook-client-id" \
  client-secret="your-facebook-client-secret"

# Add Coinbase OAuth credentials (for provider integration)
vault kv put secret/strategiz/oauth/coinbase \
  client-id="your-coinbase-client-id" \
  client-secret="your-coinbase-client-secret"

# Verify secrets are stored
vault kv get secret/strategiz/oauth/coinbase
```

### 4. Start the Application

```bash
# Option 1: Use the startup script
./scripts/local/run/start-with-vault.sh

# Option 2: Direct Java command
java -jar application/target/application-1.0-SNAPSHOT.jar

# Option 3: Maven
mvn spring-boot:run -pl application
```

## Production Setup

### 1. Vault Server Configuration

Your production Vault server should be properly secured with TLS and appropriate authentication methods.

### 2. Create Production Token

```bash
# Create a policy for the application
vault policy write strategiz-app - <<EOF
path "secret/data/strategiz/oauth/*" {
  capabilities = ["read", "list"]
}
path "secret/metadata/strategiz/oauth/*" {
  capabilities = ["read", "list"]
}
EOF

# Create a token with the policy
vault token create -policy=strategiz-app -ttl=720h
```

### 3. Deploy with Environment Variables

For Google Cloud Run:
```yaml
env:
  - name: VAULT_ADDR
    value: "https://vault.yourdomain.com"
  - name: VAULT_TOKEN
    valueFrom:
      secretKeyRef:
        name: vault-token
        key: token
```

For Docker:
```bash
docker run -e VAULT_ADDR=https://vault.yourdomain.com \
           -e VAULT_TOKEN=$VAULT_TOKEN \
           strategiz-core:latest
```

For Kubernetes:
```yaml
env:
  - name: VAULT_ADDR
    value: "https://vault.yourdomain.com"
  - name: VAULT_TOKEN
    valueFrom:
      secretKeyRef:
        name: vault-credentials
        key: token
```

## Verification

To verify Vault integration is working:

1. Check application logs for:
   ```
   Loading OAuth credentials from Vault at: http://localhost:8200
   Successfully loaded 6 OAuth properties from Vault
   ```

2. Test OAuth endpoint:
   ```bash
   curl -X POST http://localhost:8080/v1/providers \
     -H "Content-Type: application/json" \
     -d '{"providerId": "coinbase", "connectionType": "oauth"}'
   ```

   Should return an authorization URL with the client ID from Vault.

## Troubleshooting

### "No Vault token found" Error
- Ensure `VAULT_TOKEN` environment variable is set
- Check token hasn't expired: `vault token lookup`

### "Failed to load OAuth credentials from Vault" Error
- Verify Vault is running: `vault status`
- Check token has correct permissions: `vault token capabilities secret/strategiz/oauth/coinbase`
- Verify secrets exist: `vault kv get secret/strategiz/oauth/coinbase`

### OAuth Returns 502 Error
- This usually means OAuth credentials aren't loaded
- Check application logs for Vault errors
- Verify `VAULT_TOKEN` was set before starting the application

## Security Best Practices

1. **Never commit tokens** to version control
2. **Use short-lived tokens** in production (renew periodically)
3. **Use least-privilege policies** - only grant read access to required paths
4. **Enable audit logging** in production Vault
5. **Use TLS** for all production Vault communications
6. **Rotate OAuth secrets** periodically
7. **Monitor token expiration** and implement renewal

## Benefits of This Approach

1. **Consistency**: Same configuration method in all environments
2. **Security**: Secrets never stored in code or config files
3. **Auditability**: All secret access is logged in Vault
4. **Rotation**: Easy to rotate secrets without code changes
5. **Simplicity**: Single source of truth for all OAuth credentials