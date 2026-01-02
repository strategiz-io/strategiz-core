# GitHub App Setup for Platform Agents

This guide explains how to create and configure a GitHub App for Platform Agents automation.

## What is Platform Agents?

Platform Agents is an automated system that:
- Monitors Maven/NPM dependencies for security vulnerabilities
- Automatically updates dependencies via GitHub Actions workflows
- Creates pull requests with dependency updates
- Tracks execution history and success metrics

## Step 1: Create GitHub App

1. Navigate to: https://github.com/organizations/strategiz-io/settings/apps
2. Click "New GitHub App"
3. Fill in the details:

### Basic Information
- **GitHub App name**: `Strategiz Platform Agents` (or your preferred name)
- **Homepage URL**: `https://strategiz.io`
- **Webhook URL**: `https://api.strategiz.io/webhooks/github` (optional for now)
- **Webhook secret**: Leave empty for now

### Repository Permissions
Set the following permissions:

**Contents**: Read & Write (to create commits)
- Allows creating/updating files in repositories

**Workflows**: Read & Write (to trigger workflows)
- Allows triggering GitHub Actions workflows via workflow_dispatch

**Pull Requests**: Read & Write (to create PRs)
- Allows creating pull requests with dependency updates

**Actions**: Read (to query workflow run status)
- Allows fetching workflow execution history

### Where can this GitHub App be installed?
- Select: **Only on this account** (strategiz-io)

### Create the App
4. Click "Create GitHub App"
5. **Note the App ID** - you'll see it at the top of the settings page

## Step 2: Generate Private Key

1. Scroll down to "Private keys" section
2. Click "Generate a private key"
3. A `.pem` file will download automatically
4. **Save this file securely** - you'll need it in the next step

## Step 3: Install the App

1. Click "Install App" in the left sidebar
2. Select "strategiz-io" organization
3. Choose repositories:
   - **Select repositories**: `strategiz-core` and `strategiz-ui`
   - Or choose "All repositories" if you want it on all repos
4. Click "Install"

## Step 4: Store Credentials in Vault

### Local Development (Vault running at http://localhost:8200)

```bash
# Set Vault address and token
export VAULT_ADDR=http://localhost:8200
export VAULT_TOKEN=root

# Store GitHub App ID (replace with your actual App ID)
vault kv put secret/strategiz/github-app app-id="YOUR_APP_ID_HERE"

# Store private key (replace with path to your downloaded .pem file)
PRIVATE_KEY=$(cat ~/Downloads/strategiz-platform-agents.*.private-key.pem)
vault kv put secret/strategiz/github-app private-key="$PRIVATE_KEY"

# Verify
vault kv get secret/strategiz/github-app
```

### Production (Vault running at https://strategiz-vault-43628135674.us-east1.run.app)

```bash
# Set Vault address and token
export VAULT_ADDR=https://strategiz-vault-43628135674.us-east1.run.app
export VAULT_TOKEN=hvs.q2Lg7uILKNkEs20UA8mbT9Cr

# Store GitHub App ID
vault kv put secret/strategiz/github-app app-id="YOUR_APP_ID_HERE"

# Store private key
PRIVATE_KEY=$(cat ~/Downloads/strategiz-platform-agents.*.private-key.pem)
vault kv put secret/strategiz/github-app private-key="$PRIVATE_KEY"

# Verify
vault kv get secret/strategiz/github-app
```

## Step 5: Enable Platform Agents

Add to `application-api/src/main/resources/application-prod.properties`:

```properties
# GitHub App configuration
github.app.enabled=true
```

## Step 6: Test the Configuration

1. Deploy the updated application
2. Check logs for: "GitHub App configuration loaded successfully - Platform Agents enabled"
3. Test API endpoint:

```bash
curl https://api.strategiz.io/v1/console/agents
```

You should see agents with real status from GitHub Actions, not just default "IDLE" status.

## Troubleshooting

### "GitHub App private key not configured"
- Verify the private key is stored in Vault at `secret/strategiz/github-app`
- Ensure the key includes the full PEM format with header/footer:
  ```
  -----BEGIN RSA PRIVATE KEY-----
  ...
  -----END RSA PRIVATE KEY-----
  ```

### "Failed to obtain GitHub App installation token"
- Verify the App is installed on the repositories
- Check that repository permissions are correctly set
- Ensure the App ID matches the installed app

### Platform Agents still return default status
- Check that `github.app.enabled=true` is set in application properties
- Verify Vault credentials are loaded (check application logs)
- Ensure the GitHub App has the required permissions

## Security Notes

- **Never commit the private key to git**
- Store only in Vault (local and production)
- Rotate keys periodically via GitHub App settings
- Use minimum required permissions

## GitHub Actions Workflows

The Platform Agents system expects these workflows to exist in your repositories:

### Backend (strategiz-core)
`.github/workflows/security-agent.yml` - Maven dependency updates

### Frontend (strategiz-ui)
`.github/workflows/security-agent.yml` - NPM dependency updates

These workflows should:
1. Run on schedule (e.g., every 6 hours with `cron`)
2. Support manual trigger via `workflow_dispatch`
3. Check for dependency updates
4. Create PRs when updates are available
5. Run tests to verify updates don't break anything

## Next Steps

After setup:
1. Access the console at: https://console.strategiz.io/automation
2. View agent status and execution history
3. Manually trigger agents as needed
4. Monitor success rates and vulnerabilities fixed
