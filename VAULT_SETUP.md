# Vault Setup Documentation

**Last Updated:** 2026-01-10
**Purpose:** Permanent reference for Vault configuration to prevent repeated rediscovery

---

## CRITICAL: Read This First

**DO NOT** change Vault configuration without updating this file.
**DO NOT** assume Vault setup - always refer to this document.

---

## Vault Engine Version

**Version:** KV v2
**Path Format:** `secret/data/strategiz/<path>` (includes `/data/`)

### Verification
```bash
vault secrets list -detailed | grep "^secret/"
# Output should show: version: 2
```

### Code Implementation
VaultSecretService.buildVaultPath() correctly adds `/data/` to paths:
```java
// Correct implementation (DO NOT CHANGE):
properties.getSecretsPath() + "/data/strategiz"
// Results in: secret/data/strategiz/...
```

---

## Environment-Specific Configuration

### Local Development

**Vault Server:** Dev mode (in-memory, auto-unsealed)
```bash
# Start Vault
vault server -dev

# Get root token from output
export VAULT_TOKEN=root  # or token from dev server output
export VAULT_ADDR=http://localhost:8200
```

**Secret Locations:**
```bash
# ClickHouse credentials
secret/strategiz/clickhouse
  ├── host: zlqq5972rl.us-east1.gcp.clickhouse.cloud
  ├── port: 8443
  ├── database: default
  ├── username: default
  └── password: N9mP8efqdNK_V

# OAuth credentials (examples)
secret/strategiz/oauth/google
  ├── client-id: ...
  └── client-secret: ...

secret/strategiz/oauth/facebook
  ├── client-id: ...
  └── client-secret: ...
```

### Production (Google Cloud Run)

**Vault Server:** Embedded in Docker container (starts with application)

**Token Storage:** GCP Secret Manager
```bash
# Vault root token stored in:
gcloud secrets versions access latest --secret="vault-root-token"
# Value: hvs.q2Lg7uILKNkEs20UA8mbT9Cr

# Unseal keys stored in:
gcloud secrets versions access latest --secret="vault-unseal-keys"
```

**Cloud Run Configuration:**
```yaml
env:
  - name: VAULT_TOKEN
    valueFrom:
      secretKeyRef:
        name: vault-root-token
        key: latest
```

**Important:** Production does NOT use an external Vault server URL. The application:
1. Starts embedded Vault server (via scripts/production/utils/start.sh)
2. Unseals it using keys from GCP Secret Manager
3. Uses localhost:8200 as VAULT_ADDR

---

## Application Configuration

### Application-API

**Environment Variables Required:**
```bash
export VAULT_TOKEN=root  # or production token
export VAULT_ADDR=http://localhost:8200
```

**Spring Properties:**
```properties
strategiz.vault.enabled=true
strategiz.vault.address=http://localhost:8200
strategiz.vault.secrets-path=secret
strategiz.vault.fail-fast=true
strategiz.vault.fallback-to-properties=false

spring.cloud.vault.authentication=TOKEN
spring.cloud.vault.uri=http://localhost:8200
spring.cloud.vault.token=${VAULT_TOKEN:root}
spring.cloud.vault.kv.enabled=true
spring.cloud.vault.kv.backend=secret
spring.cloud.vault.kv.default-context=strategiz
```

### Application-Console

**Additional Requirements:**
```bash
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/firebase-service-account.json
```

**Spring Properties:**
```properties
# Same as application-api, plus:
console.auth.enabled=false  # for local dev
console.auth.enabled=true   # for production
```

---

## Secret Path Mapping

### How Secrets Are Accessed

When code calls: `secretManager.readSecret("clickhouse.host")`

VaultSecretService does:
1. Split key by `.` → `["clickhouse", "host"]`
2. Build path: `secret/data/strategiz/clickhouse` (all parts except last)
3. Extract field: `host` (last part)
4. Read from Vault: `GET /v1/secret/data/strategiz/clickhouse`
5. Return field value from response

### Common Secret Keys

| Code Reference | Vault Path | Field Name | Example Value |
|---------------|------------|------------|---------------|
| `clickhouse.host` | `secret/data/strategiz/clickhouse` | `host` | `*.gcp.clickhouse.cloud` |
| `clickhouse.port` | `secret/data/strategiz/clickhouse` | `port` | `8443` |
| `clickhouse.database` | `secret/data/strategiz/clickhouse` | `database` | `default` |
| `clickhouse.username` | `secret/data/strategiz/clickhouse` | `username` | `default` |
| `clickhouse.password` | `secret/data/strategiz/clickhouse` | `password` | `...` |
| `oauth.google.client-id` | `secret/data/strategiz/oauth/google` | `client-id` | `...` |
| `oauth.google.client-secret` | `secret/data/strategiz/oauth/google` | `client-secret` | `...` |

---

## Troubleshooting Guide

### Issue: "Cannot connect to Vault"

**Local Development:**
```bash
# Check if Vault is running
vault status

# If not running:
vault server -dev

# Export token from dev server output
export VAULT_TOKEN=<dev-token>
export VAULT_ADDR=http://localhost:8200
```

**Production:**
- Vault runs embedded in the container
- Check Cloud Run logs for Vault startup errors
- Verify vault-root-token secret exists in GCP

### Issue: "Secret not found"

**Verify secret exists:**
```bash
# Local:
export VAULT_TOKEN=root
vault kv get secret/strategiz/clickhouse

# Production:
export VAULT_TOKEN=hvs.q2Lg7uILKNkEs20UA8mbT9Cr
vault kv get -address=<vault-url> secret/strategiz/clickhouse
```

**Check path format:**
- CLI uses: `secret/strategiz/clickhouse` (KV v2 CLI auto-adds /data/)
- API uses: `secret/data/strategiz/clickhouse` (HTTP API requires /data/)
- Code uses: VaultSecretService automatically adds /data/

### Issue: "Application can't read secrets"

**Check environment variables are exported:**
```bash
# NOT ENOUGH (JVM property, won't work):
mvn spring-boot:run -DVAULT_TOKEN=root

# CORRECT (environment variable):
export VAULT_TOKEN=root
mvn spring-boot:run
```

**Why:** VaultHttpClient reads from Spring Environment, which needs actual environment variables.

---

## Adding New Secrets

### Local Development
```bash
# Set your local Vault token
export VAULT_TOKEN=root
export VAULT_ADDR=http://localhost:8200

# Add secret
vault kv put secret/strategiz/path/to/secret \
  field1=value1 \
  field2=value2

# Verify
vault kv get secret/strategiz/path/to/secret
```

### Production
```bash
# Use production token
export VAULT_TOKEN=hvs.q2Lg7uILKNkEs20UA8mbT9Cr

# Access production Vault (if external) or update via Cloud Run
vault kv put secret/strategiz/path/to/secret \
  field1=value1 \
  field2=value2
```

**Note:** For embedded production Vault, secrets must be added before container starts or by accessing the running container.

---

## Common Mistakes (DO NOT DO THESE)

### ❌ Changing VaultSecretService Path Logic
```java
// WRONG - breaks production:
return properties.getSecretsPath() + "/strategiz/" + key;

// CORRECT - works with KV v2:
return properties.getSecretsPath() + "/data/strategiz" + path;
```

### ❌ Using JVM Properties Instead of Environment Variables
```bash
# WRONG:
mvn spring-boot:run -DVAULT_TOKEN=root -DVAULT_ADDR=http://localhost:8200

# CORRECT:
export VAULT_TOKEN=root
export VAULT_ADDR=http://localhost:8200
mvn spring-boot:run
```

### ❌ Assuming External Vault in Production
```bash
# WRONG - production doesn't have external Vault:
export VAULT_ADDR=https://vault.strategiz.io

# CORRECT - production uses embedded Vault:
# Vault starts inside container, no external URL needed
```

### ❌ Hardcoding Credentials in Properties
```properties
# WRONG:
strategiz.clickhouse.host=zlqq5972rl.us-east1.gcp.clickhouse.cloud
strategiz.clickhouse.password=N9mP8efqdNK_V

# CORRECT:
# Leave empty, load from Vault via SecretManager
```

---

## Files That Reference Vault

**Configuration:**
- `application-api/src/main/resources/application-dev.properties`
- `application-console/src/main/resources/application.properties`

**Secret Loading:**
- `framework/framework-secrets/src/main/java/io/strategiz/framework/secrets/service/VaultSecretService.java`
- `framework/framework-secrets/src/main/java/io/strategiz/framework/secrets/client/VaultHttpClient.java`

**Startup Scripts:**
- `scripts/production/utils/start.sh` (starts embedded Vault, exports VAULT_TOKEN)
- `/tmp/start_strategiz_proper.sh` (local dev startup)

**Deployment:**
- `scripts/production/deploy/deploy-console-app.sh` (deploys console app with Vault secret mounting)
  - **IMPORTANT**: Does NOT try to connect to external Vault (production uses embedded)
  - Mounts VAULT_TOKEN from GCP Secret Manager: `vault-root-token`
  - No VAULT_ADDR needed (defaults to localhost:8200 inside container)
- Cloud Run service configs (mount vault-root-token from Secret Manager)

---

## Production Token Rotation

**Current Token:** `hvs.q2Lg7uILKNkEs20UA8mbT9Cr`

**To Rotate:**
1. Create new token in Vault
2. Update GCP Secret Manager:
   ```bash
   echo -n 'new-token' | gcloud secrets versions add vault-root-token --data-file=-
   ```
3. Redeploy Cloud Run services (they'll pick up new token)
4. Update this document with new token

---

## Quick Reference Commands

```bash
# Local Development Setup
vault server -dev
export VAULT_TOKEN=root
export VAULT_ADDR=http://localhost:8200

# List all secrets
vault kv list secret/strategiz

# Get specific secret
vault kv get secret/strategiz/clickhouse

# Add/update secret
vault kv put secret/strategiz/path field=value

# Production token (read-only, from GCP)
gcloud secrets versions access latest --secret="vault-root-token"

# Check Vault status
vault status
```

---

## Questions to Ask Before Making Changes

1. **"Am I changing Vault path logic?"** → Check this document first, likely NO
2. **"Should I use KV v1 or v2?"** → Always KV v2 (see top of document)
3. **"Where should I add secrets?"** → See "Adding New Secrets" section
4. **"How do I fix Vault connection errors?"** → See "Troubleshooting Guide"
5. **"Should I change environment variable passing?"** → See "Common Mistakes"

---

## Contact & Updates

**Document Owner:** Project maintainers
**Update Frequency:** Every time Vault configuration changes
**Last Verified Working:** 2026-01-10 (all applications starting successfully)

**If you update Vault configuration, YOU MUST update this document.**
