# Vault Token Storage Guide

## Development Token

**Token Value:** `root` (default for Vault dev server)

### Storage Options:

1. **Shell Profile** (Recommended for dev)
   ```bash
   # Add to ~/.zshrc or ~/.bashrc
   export VAULT_TOKEN=root
   export VAULT_ADDR=http://localhost:8200
   ```

2. **Environment File** (`.env.local` - never commit!)
   ```bash
   VAULT_TOKEN=root
   VAULT_ADDR=http://localhost:8200
   ```

3. **IDE Configuration**
   - IntelliJ: Run Configuration → Environment Variables
   - VSCode: `.vscode/launch.json` (add to `.gitignore`)

## Production Token

**Token Value:** Generated using `scripts/vault/create-prod-token.sh`

### Storage Options:

1. **Google Cloud Secret Manager** (Recommended for Cloud Run)
   ```bash
   # Store the token
   echo -n "YOUR_PROD_TOKEN" | gcloud secrets create vault-token \
     --data-file=- \
     --replication-policy="automatic"
   
   # Reference in Cloud Run
   gcloud run deploy strategiz-core \
     --set-secrets="VAULT_TOKEN=vault-token:latest"
   ```

2. **GitHub Secrets** (for CI/CD)
   - Go to Repository → Settings → Secrets
   - Add `VAULT_TOKEN_PROD` with your production token

3. **Kubernetes Secrets**
   ```yaml
   apiVersion: v1
   kind: Secret
   metadata:
     name: vault-credentials
   type: Opaque
   data:
     token: <base64-encoded-token>
   ```

4. **Docker Compose** (for staging)
   ```yaml
   services:
     app:
       environment:
         VAULT_TOKEN: ${VAULT_TOKEN_PROD}
   ```

## Security Best Practices

### DO:
- ✅ Use different tokens for dev and prod
- ✅ Store production tokens in platform secret managers
- ✅ Rotate production tokens every 30-90 days
- ✅ Use read-only policies in production
- ✅ Monitor token usage in Vault audit logs

### DON'T:
- ❌ Commit tokens to git (add `.env*` to `.gitignore`)
- ❌ Share production tokens between environments
- ❌ Use root tokens in production
- ❌ Store tokens in plain text files in production
- ❌ Log token values in application logs

## Token Lifecycle

### Development
```bash
# Start Vault (creates root token)
vault server -dev

# Token is always: root
export VAULT_TOKEN=root
```

### Production
```bash
# Create token (run once)
./scripts/vault/create-prod-token.sh

# Store in secret manager
gcloud secrets create vault-token --data-file=-

# Renew before expiration (every 25 days)
vault token renew
```

## Verification

Test your token:
```bash
# Check token is valid
vault token lookup

# Test OAuth secret access
vault kv get secret/strategiz/oauth/coinbase
```