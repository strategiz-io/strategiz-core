# Strategiz Secrets Management

## Overview

Strategiz uses HashiCorp Vault for centralized secrets management across all environments. This provides secure storage, access control, and audit logging for all sensitive credentials including OAuth keys, API tokens, and encryption keys.

## Architecture

### Production Environment

```
┌─────────────────────┐     ┌─────────────────────┐
│   strategiz-core    │────▶│   strategiz-vault   │
│   (Cloud Run)       │     │   (Cloud Run)       │
└─────────────────────┘     └──────────┬──────────┘
                                       │
                    ┌──────────────────┼──────────────────┐
                    ▼                  ▼                  ▼
            ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
            │  Cloud KMS   │  │    GCS       │  │  Secret Mgr  │
            │ (auto-unseal)│  │  (storage)   │  │ (root token) │
            └──────────────┘  └──────────────┘  └──────────────┘
```

### Local Development

```
┌─────────────────────┐     ┌─────────────────────┐
│   strategiz-core    │────▶│   Vault Dev Server  │
│   (localhost:8080)  │     │   (localhost:8200)  │
└─────────────────────┘     └─────────────────────┘
```

## Secret Categories

### 1. Authentication Secrets

| Secret Path | Description | Fields |
|-------------|-------------|--------|
| `strategiz/tokens/prod` | PASETO token signing keys | `identity-key`, `session-key` |
| `strategiz/oauth/google` | Google OAuth | `client_id`, `client_secret` |
| `strategiz/oauth/facebook` | Facebook OAuth | `client_id`, `client_secret` |

### 2. Provider Secrets

| Secret Path | Description | Fields |
|-------------|-------------|--------|
| `strategiz/providers/coinbase` | Coinbase OAuth | `client_id`, `client_secret` |
| `strategiz/providers/schwab` | Charles Schwab | `app_key`, `secret` |
| `strategiz/providers/alpaca` | Alpaca OAuth | `client_id`, `client_secret` |
| `strategiz/providers/kraken` | Kraken | `api_key`, `private_key` |
| `strategiz/alpaca/paper` | Alpaca Paper Trading | `api-key`, `api-secret`, `api-url` |
| `strategiz/alpaca/marketdata` | Alpaca Market Data | `api-key`, `api-secret`, `base-url`, `feed` |

### 3. AI/ML Secrets

| Secret Path | Description | Fields |
|-------------|-------------|--------|
| `strategiz/gemini` | Google Gemini AI | `api_key` |

## Accessing Secrets in Code

### Using VaultSecretService

```java
@Autowired
private VaultSecretService vaultSecretService;

// Read a single secret field
String apiKey = vaultSecretService.readSecret("alpaca.paper.api-key");

// Read with default value
String apiUrl = vaultSecretService.readSecret("alpaca.paper.api-url", "https://paper-api.alpaca.markets");

// Read entire secret as map
Map<String, Object> secrets = vaultSecretService.readSecretAsMap("alpaca.paper");
```

### Secret Key Format

Secret keys use dot notation: `<category>.<subcategory>.<field>`

Examples:
- `alpaca.paper.api-key` → Path: `secret/data/strategiz/alpaca/paper`, Field: `api-key`
- `oauth.google.client-id` → Path: `secret/data/strategiz/oauth/google`, Field: `client-id`

## Local Development Setup

### 1. Start Vault Dev Server

```bash
# Start Vault in development mode
vault server -dev

# Note the Root Token displayed in output
```

### 2. Set Environment Variables

```bash
export VAULT_ADDR='http://localhost:8200'
export VAULT_TOKEN='<root-token-from-step-1>'
```

### 3. Add Required Secrets

```bash
# Token keys (generate new ones)
vault kv put secret/strategiz/tokens/prod \
  identity-key="$(openssl rand -base64 32)" \
  session-key="$(openssl rand -base64 32)"

# OAuth credentials
vault kv put secret/strategiz/oauth/google \
  client_id="your-google-client-id" \
  client_secret="your-google-client-secret"

# Provider credentials
vault kv put secret/strategiz/alpaca/paper \
  api-key="your-paper-api-key" \
  api-secret="your-paper-api-secret" \
  api-url="https://paper-api.alpaca.markets/v2"
```

### 4. Run Application

```bash
# The application will connect to Vault at $VAULT_ADDR
mvn spring-boot:run -pl application-core
```

## Production Configuration

### Application Properties

```properties
# Enable Vault in production
strategiz.vault.enabled=true
strategiz.vault.address=${STRATEGIZ_VAULT_ADDRESS:https://strategiz-vault-xxx.us-central1.run.app}
strategiz.vault.cache-timeout-ms=60000
strategiz.vault.fail-fast=true
strategiz.vault.secrets-path=secret
```

### Cloud Run Configuration

The `VAULT_TOKEN` is provided via GCP Secret Manager:

```bash
gcloud run services update strategiz-core \
  --region=us-central1 \
  --set-secrets VAULT_TOKEN=vault-root-token:latest
```

## Security Best Practices

### 1. Token Management

- Use short-lived tokens in production
- Store root token only in GCP Secret Manager
- Never commit tokens to version control
- Rotate tokens periodically

### 2. Access Control

- Apply least-privilege policies
- Separate tokens for different services
- Enable audit logging

### 3. Secret Rotation

```bash
# Generate new token keys (will log out all users)
IDENTITY_KEY=$(openssl rand -base64 32)
SESSION_KEY=$(openssl rand -base64 32)

curl -X POST \
  -H "X-Vault-Token: $VAULT_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"data\": {\"identity-key\": \"$IDENTITY_KEY\", \"session-key\": \"$SESSION_KEY\"}}" \
  "$VAULT_ADDR/v1/secret/data/strategiz/tokens/prod"
```

### 4. Monitoring

Check Vault health:
```bash
curl $VAULT_ADDR/v1/sys/health
```

Expected response:
```json
{"initialized": true, "sealed": false, "standby": false}
```

## Troubleshooting

### "Secret not found" Error

1. Check the secret path exists:
   ```bash
   vault kv get secret/strategiz/<path>
   ```

2. Verify key format matches expected pattern:
   - Code: `alpaca.paper.api-key`
   - Vault: `secret/strategiz/alpaca/paper` with field `api-key`

### "Vault is sealed" Error

For production (with auto-unseal), this shouldn't happen. If it does:
1. Check Cloud KMS key permissions
2. Check GCS bucket accessibility
3. Review Vault logs in Cloud Run

For development:
```bash
# Restart Vault dev server
vault server -dev
```

### "Permission denied" Error

1. Verify token is valid: `vault token lookup`
2. Check token has required policy
3. Ensure VAULT_TOKEN environment variable is set

## Migration from .env Files

If migrating from .env-based secrets:

1. Export existing secrets from .env
2. Add to Vault using appropriate paths
3. Update code to use `VaultSecretService`
4. Remove .env file

## Related Documentation

- [Vault Setup](vault-setup.md) - Vault server configuration
- [Vault Token Storage](vault-token-storage.md) - Token management
- [Security Overview](overview.md) - Overall security architecture
