# Vault Configuration for Strategiz

This directory contains the production HashiCorp Vault configuration for Strategiz.

## Architecture

```
┌─────────────────────┐     ┌─────────────────────┐
│   strategiz-core    │────▶│   strategiz-vault   │
│   (Cloud Run)       │     │   (Cloud Run)       │
└─────────────────────┘     └──────────┬──────────┘
                                       │
                    ┌──────────────────┼──────────────────┐
                    ▼                  ▼                  ▼
            ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
            │  Cloud KMS   │  │    GCS       │  │   Secrets    │
            │ (auto-unseal)│  │  (storage)   │  │   (data)     │
            └──────────────┘  └──────────────┘  └──────────────┘
```

## Files

- `vault.hcl` - Vault server configuration
- `Dockerfile` - Docker image for production Vault

## Configuration Details

### Storage Backend
- **Type**: Google Cloud Storage (GCS)
- **Bucket**: `gs://strategiz-vault-storage`
- **Location**: us-central1

### Auto-Unseal
- **Type**: Google Cloud KMS
- **Project**: strategiz-io
- **Key Ring**: vault-keyring
- **Key**: vault-unseal-key
- **Location**: us-central1

### Listener
- **Address**: 0.0.0.0:8200
- **TLS**: Disabled (Cloud Run handles TLS termination)

## Deployment

### Prerequisites

1. GCP project with the following APIs enabled:
   - Cloud Run API
   - Cloud KMS API
   - Cloud Storage API

2. Service account with permissions:
   - `roles/cloudkms.cryptoKeyEncrypterDecrypter` on the KMS key
   - `roles/storage.objectAdmin` on the GCS bucket

### Build and Deploy

```bash
# Build the Docker image
cd vault-config
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

### Initial Setup (First Time Only)

After first deployment, Vault needs to be initialized:

```bash
# Initialize Vault with recovery keys
curl -X POST \
  https://strategiz-vault-<project-number>.us-central1.run.app/v1/sys/init \
  -H "Content-Type: application/json" \
  -d '{"recovery_shares": 5, "recovery_threshold": 3}'
```

Save the recovery keys and root token securely in GCP Secret Manager:

```bash
# Save root token
echo -n "<root-token>" | gcloud secrets versions add vault-root-token --data-file=-

# Save recovery keys
echo "<recovery-keys>" | gcloud secrets versions add vault-unseal-keys --data-file=-
```

### Enable KV Secrets Engine

```bash
VAULT_TOKEN="<root-token>"
VAULT_ADDR="https://strategiz-vault-<project-number>.us-central1.run.app"

curl -X POST \
  -H "X-Vault-Token: $VAULT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"type": "kv", "options": {"version": "2"}}' \
  "$VAULT_ADDR/v1/sys/mounts/secret"
```

## Secret Paths

Secrets are stored at the following paths:

| Path | Description |
|------|-------------|
| `secret/strategiz/tokens/prod` | PASETO token signing keys |
| `secret/strategiz/oauth/google` | Google OAuth credentials |
| `secret/strategiz/oauth/facebook` | Facebook OAuth credentials |
| `secret/strategiz/providers/coinbase` | Coinbase API credentials |
| `secret/strategiz/providers/schwab` | Charles Schwab credentials |
| `secret/strategiz/providers/alpaca` | Alpaca OAuth credentials |
| `secret/strategiz/alpaca/paper` | Alpaca Paper trading API |
| `secret/strategiz/alpaca/marketdata` | Alpaca Market data API |
| `secret/strategiz/providers/kraken` | Kraken API credentials |
| `secret/strategiz/gemini` | Google Gemini AI API key |

## Adding Secrets

```bash
VAULT_TOKEN="<root-token>"
VAULT_ADDR="https://strategiz-vault-<project-number>.us-central1.run.app"

# Add a secret
curl -X POST \
  -H "X-Vault-Token: $VAULT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"data": {"key": "value"}}' \
  "$VAULT_ADDR/v1/secret/data/strategiz/<path>"

# Read a secret
curl -s -H "X-Vault-Token: $VAULT_TOKEN" \
  "$VAULT_ADDR/v1/secret/data/strategiz/<path>"
```

## Health Check

```bash
curl https://strategiz-vault-<project-number>.us-central1.run.app/v1/sys/health
```

Expected response:
```json
{
  "initialized": true,
  "sealed": false,
  "standby": false,
  "version": "1.15.6"
}
```

## Cost

With auto-unseal and persistent storage, Vault can safely scale to zero:

| Component | Monthly Cost |
|-----------|-------------|
| Cloud Run (pay-per-use) | ~$2-5 |
| Cloud Storage | ~$0.02 |
| Cloud KMS | ~$1 |
| **Total** | **~$3-6/month** |

## Recovery

If Vault becomes unavailable or needs to be rebuilt:

1. The GCS bucket retains all encrypted data
2. Cloud KMS key allows auto-unseal
3. Recovery keys can be used if KMS is unavailable
4. Root token stored in GCP Secret Manager

## Security Considerations

1. **TLS**: Cloud Run provides TLS termination
2. **Authentication**: Root token required for all operations
3. **Encryption**: All data encrypted at rest in GCS
4. **Auto-unseal**: KMS key protects the master key
5. **Recovery keys**: Stored separately for disaster recovery
