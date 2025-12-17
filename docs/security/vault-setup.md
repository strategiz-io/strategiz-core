# Vault Setup for Strategiz

## Overview

Strategiz uses HashiCorp Vault deployed on Google Cloud Run for production secrets management. The setup includes:

- **Persistent Storage**: Google Cloud Storage (GCS)
- **Auto-Unseal**: Google Cloud KMS
- **High Availability**: Cloud Run with auto-scaling

## Production Architecture

```
                              ┌─────────────────────────────────────┐
                              │         Google Cloud Platform        │
                              │                                       │
┌──────────────┐              │  ┌─────────────────────────────────┐ │
│              │   HTTPS      │  │        strategiz-vault          │ │
│  strategiz-  │─────────────▶│  │        (Cloud Run)              │ │
│    core      │              │  │                                 │ │
│              │              │  │  ┌─────────┐    ┌─────────────┐ │ │
└──────────────┘              │  │  │ Vault   │───▶│  GCS Bucket │ │ │
                              │  │  │ Server  │    │  (Storage)  │ │ │
                              │  │  └────┬────┘    └─────────────┘ │ │
                              │  │       │                         │ │
                              │  │       ▼                         │ │
                              │  │  ┌─────────────┐               │ │
                              │  │  │  Cloud KMS  │               │ │
                              │  │  │(Auto-Unseal)│               │ │
                              │  │  └─────────────┘               │ │
                              │  └─────────────────────────────────┘ │
                              │                                       │
                              │  ┌─────────────────────────────────┐ │
                              │  │       Secret Manager            │ │
                              │  │  (Vault Root Token Storage)    │ │
                              │  └─────────────────────────────────┘ │
                              └───────────────────────────────────────┘
```

## Infrastructure Components

### 1. Cloud KMS (Auto-Unseal)

- **Key Ring**: `vault-keyring`
- **Crypto Key**: `vault-unseal-key`
- **Location**: `us-central1`
- **Purpose**: Automatically unseal Vault on startup

### 2. Cloud Storage (Persistent Backend)

- **Bucket**: `gs://strategiz-vault-storage`
- **Location**: `us-central1`
- **Purpose**: Store encrypted Vault data

### 3. Secret Manager

- **vault-root-token**: Vault root authentication token
- **vault-unseal-keys**: Recovery keys for disaster recovery

### 4. Cloud Run Service

- **Service**: `strategiz-vault`
- **Image**: `gcr.io/strategiz-io/vault-prod`
- **Port**: 8200
- **Memory**: 512Mi
- **Min Instances**: 0 (scales to zero when idle)

## Configuration Files

### vault.hcl

Located at `/vault-config/vault.hcl`:

```hcl
ui = true
disable_mlock = true

listener "tcp" {
  address     = "0.0.0.0:8200"
  tls_disable = true  # Cloud Run handles TLS
}

storage "gcs" {
  bucket     = "strategiz-vault-storage"
  ha_enabled = "false"
}

seal "gcpckms" {
  project     = "strategiz-io"
  region      = "us-central1"
  key_ring    = "vault-keyring"
  crypto_key  = "vault-unseal-key"
}

api_addr = "https://strategiz-vault-xxx.us-central1.run.app"
```

## Setup Instructions

### Prerequisites

```bash
# Enable required APIs
gcloud services enable cloudkms.googleapis.com
gcloud services enable run.googleapis.com
gcloud services enable storage.googleapis.com
```

### Step 1: Create KMS Key for Auto-Unseal

```bash
# Create keyring
gcloud kms keyrings create vault-keyring --location=us-central1

# Create crypto key
gcloud kms keys create vault-unseal-key \
  --location=us-central1 \
  --keyring=vault-keyring \
  --purpose=encryption
```

### Step 2: Create Storage Bucket

```bash
gcloud storage buckets create gs://strategiz-vault-storage \
  --location=us-central1 \
  --uniform-bucket-level-access
```

### Step 3: Grant Service Account Permissions

```bash
PROJECT_NUMBER=$(gcloud projects describe strategiz-io --format='value(projectNumber)')
SA_EMAIL="${PROJECT_NUMBER}-compute@developer.gserviceaccount.com"

# KMS permissions
gcloud kms keys add-iam-policy-binding vault-unseal-key \
  --location=us-central1 \
  --keyring=vault-keyring \
  --member="serviceAccount:$SA_EMAIL" \
  --role="roles/cloudkms.cryptoKeyEncrypterDecrypter"

# GCS permissions
gcloud storage buckets add-iam-policy-binding gs://strategiz-vault-storage \
  --member="serviceAccount:$SA_EMAIL" \
  --role="roles/storage.objectAdmin"
```

### Step 4: Build and Deploy Vault

```bash
cd vault-config

# Build Docker image
gcloud builds submit --tag gcr.io/strategiz-io/vault-prod .

# Deploy to Cloud Run
gcloud run deploy strategiz-vault \
  --image=gcr.io/strategiz-io/vault-prod:latest \
  --region=us-central1 \
  --platform=managed \
  --allow-unauthenticated \
  --memory=512Mi \
  --cpu=1 \
  --min-instances=0 \
  --max-instances=3 \
  --port=8200 \
  --no-cpu-throttling
```

### Step 5: Initialize Vault

```bash
VAULT_ADDR="https://strategiz-vault-xxx.us-central1.run.app"

# Initialize with recovery keys
curl -X POST "$VAULT_ADDR/v1/sys/init" \
  -H "Content-Type: application/json" \
  -d '{"recovery_shares": 5, "recovery_threshold": 3}'

# Save the output! Contains root_token and recovery_keys
```

### Step 6: Store Credentials in Secret Manager

```bash
# Save root token
echo -n "<root-token>" | gcloud secrets create vault-root-token --data-file=-

# Save recovery keys
echo "<recovery-keys-json>" | gcloud secrets create vault-unseal-keys --data-file=-
```

### Step 7: Enable KV Secrets Engine

```bash
VAULT_TOKEN="<root-token>"

curl -X POST "$VAULT_ADDR/v1/sys/mounts/secret" \
  -H "X-Vault-Token: $VAULT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"type": "kv", "options": {"version": "2"}}'
```

## Local Development Setup

For local development, use Vault in dev mode:

```bash
# Start Vault dev server
vault server -dev

# Set environment variables
export VAULT_ADDR='http://localhost:8200'
export VAULT_TOKEN='<root-token-from-output>'

# Add secrets
vault kv put secret/strategiz/tokens/prod \
  identity-key="$(openssl rand -base64 32)" \
  session-key="$(openssl rand -base64 32)"
```

## Operations

### Health Check

```bash
curl https://strategiz-vault-xxx.us-central1.run.app/v1/sys/health
```

Response when healthy:
```json
{
  "initialized": true,
  "sealed": false,
  "standby": false,
  "version": "1.15.6"
}
```

### Add a Secret

```bash
VAULT_TOKEN=$(gcloud secrets versions access latest --secret=vault-root-token)
VAULT_ADDR="https://strategiz-vault-xxx.us-central1.run.app"

curl -X POST "$VAULT_ADDR/v1/secret/data/strategiz/<path>" \
  -H "X-Vault-Token: $VAULT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"data": {"key": "value"}}'
```

### Read a Secret

```bash
curl -s "$VAULT_ADDR/v1/secret/data/strategiz/<path>" \
  -H "X-Vault-Token: $VAULT_TOKEN"
```

### List Secrets

```bash
curl -s "$VAULT_ADDR/v1/secret/metadata/strategiz?list=true" \
  -H "X-Vault-Token: $VAULT_TOKEN"
```

## Disaster Recovery

### If Vault Becomes Unavailable

1. **Check Cloud Run logs** for errors
2. **Verify KMS permissions** are intact
3. **Check GCS bucket** accessibility
4. **Redeploy** if necessary - data persists in GCS

### If Root Token is Compromised

1. Generate new root token using recovery keys
2. Update Secret Manager with new token
3. Restart strategiz-core to pick up new token

### Complete Rebuild

If everything is lost:

1. Data in GCS bucket is preserved (encrypted)
2. Use recovery keys to unseal manually
3. Generate new root token
4. Resume operations

## Cost Analysis

| Component | Monthly Cost |
|-----------|-------------|
| Cloud Run (Vault) | ~$2-5 (pay-per-use) |
| Cloud Storage | ~$0.02 |
| Cloud KMS | ~$1 |
| Secret Manager | ~$0.06 |
| **Total** | **~$3-7/month** |

Compared to always-on (min-instances=1): **~$20/month savings**

## Security Considerations

1. **TLS Termination**: Cloud Run provides automatic HTTPS
2. **Authentication**: All requests require X-Vault-Token header
3. **Encryption at Rest**: GCS storage is encrypted
4. **Auto-Unseal**: KMS key protects master key
5. **Recovery Keys**: Stored separately for disaster recovery
6. **Audit Logging**: Enable Vault audit logs for compliance

## Related Documentation

- [Secrets Management](secrets-management.md) - How to use secrets in code
- [Vault Token Storage](vault-token-storage.md) - Token management details
- [Security Overview](overview.md) - Overall security architecture
- [Vault Config README](/vault-config/README.md) - Module documentation
