# Infrastructure Documentation

This folder contains documentation about Strategiz infrastructure and production environment.

## Contents

| Document | Description |
|----------|-------------|
| [production.md](./production.md) | Complete production infrastructure overview |

## Quick Reference

### Production URLs

| Service | URL |
|---------|-----|
| Main App | https://strategiz.io |
| Admin Console | https://console.strategiz.io |
| API | https://api.strategiz.io |

### GCP Resources

| Resource | Type | Region |
|----------|------|--------|
| strategiz-api | Cloud Run | us-east1 |
| strategiz-vault | Cloud Run | us-east1 |
| Firestore | Database | multi-region |

### Health Check

```bash
# Check API status
gcloud run services describe strategiz-api --region us-east1 --format="value(status.conditions[0].status)"

# Check Vault status
gcloud run services describe strategiz-vault --region us-east1 --format="value(status.conditions[0].status)"
```
