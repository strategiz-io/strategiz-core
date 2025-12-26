# Quality Monitoring Setup Guide

Quick setup guide to get quality metrics flowing to your dashboard.

## What You Built

✅ **Analysis tools** - SpotBugs, PMD, Checkstyle, JaCoCo
✅ **GitHub Actions workflow** - Runs on every push
✅ **Backend storage** - Firestore cache
✅ **Dashboard** - console.strategiz.io/quality
✅ **Authentication** - Vault token validation

## Setup Steps (5 minutes)

### Step 1: Add Token to Vault

Add the CI/CD token to your Vault instance:

```bash
# Local development (Vault running on localhost:8200)
export VAULT_ADDR=http://127.0.0.1:8200
export VAULT_TOKEN=root  # or your dev token

# Create the token (use a strong random value)
vault kv put secret/strategiz/ci-cd \
  quality-api-token="sqak_$(openssl rand -hex 32)"

# Verify it was saved
vault kv get secret/strategiz/ci-cd
```

**Production Vault:**
```bash
# Use your production Vault address and token
export VAULT_ADDR=https://strategiz-vault-xxx.us-east1.run.app
export VAULT_TOKEN=<your-prod-token>

# Create the same token
vault kv put secret/strategiz/ci-cd \
  quality-api-token="sqak_$(openssl rand -hex 32)"
```

**Copy the token value** - you'll need it for GitHub!

---

### Step 2: Add Token to GitHub Secrets

1. Go to your GitHub repository
2. Click **Settings** → **Secrets and variables** → **Actions**
3. Click **New repository secret**
4. Name: `QUALITY_API_TOKEN`
5. Value: Paste the token from Step 1 (the `sqak_xxx` value)
6. Click **Add secret**

---

### Step 3: Test It

**Option A: Push to main**
```bash
# Make a small change and push
echo "# Quality test" >> README.md
git add README.md
git commit -m "test: Trigger quality analysis"
git push origin main
```

**Option B: Run workflow manually**
1. Go to **Actions** tab in GitHub
2. Click **Quality Analysis** workflow
3. Click **Run workflow** → **Run workflow**

**Check the results:**
1. Wait ~5 minutes for workflow to complete
2. Go to `console.strategiz.io/quality`
3. Refresh the page
4. You should see metrics!

---

## How It Works

```
1. You push code
   ↓
2. GitHub Actions runs analysis tools
   ↓
3. Python script aggregates results
   ↓
4. curl posts to /v1/console/quality/cache
   with Authorization: Bearer sqak_xxx
   ↓
5. CiCdAuthFilter validates token against Vault
   ↓
6. Results saved to Firestore
   ↓
7. Dashboard displays metrics
```

---

## Troubleshooting

### "Invalid API token" error in GitHub Actions

**Problem:** Token doesn't match what's in Vault

**Fix:**
```bash
# Get token from Vault
vault kv get secret/strategiz/ci-cd

# Update GitHub secret with exact same value
```

### "VaultTemplate not available" warning

**Problem:** Vault not configured for local development

**Fix:** Start Vault server:
```bash
vault server -dev
export VAULT_TOKEN=root
```

### No metrics showing on dashboard

**Problem:** Workflow hasn't run yet, or failed

**Fix:**
1. Check GitHub Actions tab for workflow status
2. Look for errors in workflow logs
3. Verify token is set correctly

### Analysis reports not found

**Problem:** Maven plugins didn't run

**Fix:**
```bash
# Run locally to test
mvn verify -DskipTests

# Check for reports
ls -la **/target/spotbugs/
ls -la **/target/pmd/
ls -la **/target/site/jacoco/
```

---

## What's Next?

Once metrics are flowing:

1. **Set quality gates** - Fail builds if quality drops
2. **Track trends** - Monitor improvements over time
3. **Add custom rules** - Create project-specific Checkstyle/PMD rules
4. **Expand coverage** - Write more tests to increase coverage %

---

## Files Created

- `service/service-console-quality/src/main/java/io/strategiz/service/console/quality/config/CiCdAuthConfig.java` - Loads token from Vault
- `service/service-console-quality/src/main/java/io/strategiz/service/console/quality/config/CiCdAuthFilter.java` - Validates token
- `.github/workflows/quality-analysis.yml` - GitHub Actions workflow
- `scripts/aggregate-quality-metrics.py` - Aggregates tool reports

---

## Quick Reference

**Vault Path:** `secret/strategiz/ci-cd`
**Vault Key:** `quality-api-token`
**GitHub Secret:** `QUALITY_API_TOKEN`
**API Endpoint:** `POST /v1/console/quality/cache`
**Dashboard:** `console.strategiz.io/quality`

---

Need help? Check the full documentation in `docs/QUALITY_MONITORING.md`
