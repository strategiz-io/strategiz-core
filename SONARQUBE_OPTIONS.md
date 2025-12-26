# SonarQube Deployment Options for Strategiz

## Quick Comparison

| Option | Cost/Month | Setup Time | Maintenance | Production Dashboard |
|--------|-----------|------------|-------------|---------------------|
| **Local Only** | $0 | 5 min | None | Framework Compliance Only |
| **SonarCloud** | $10-120 | 10 min | None | Full Metrics ✅ |
| **GCE VM** | $30-50 | 30 min | Medium | Full Metrics ✅ |
| **Cloud Run** | $80-120 | 60 min | High | Full Metrics ✅ (complex) |

## Current Quality Dashboard Status

### ✅ Already Working in Production (No SonarQube Needed)
Your Framework Compliance Scanner provides:
- **Exception Handling**: Detects raw exception usage
- **Service Pattern**: Ensures services extend BaseService
- **Controller Pattern**: Ensures controllers extend BaseController
- **Violations List**: File paths, line numbers, severity
- **Overall Compliance Grade**: A-F based on adherence

This is **REAL-TIME** analysis of your actual codebase!

### 🔍 Additional Metrics from SonarQube (Optional)
- Bugs count
- Vulnerabilities count
- Code smells count
- Code coverage %
- Duplications %
- Technical debt
- Reliability rating

---

## Option 1: Local Development Only

### Setup
```bash
cd /Users/cuztomizer/Documents/GitHub/strategiz-core
./setup-sonarqube.sh
```

### Usage
```bash
# Developers run locally before committing
mvn clean verify sonar:sonar \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.token=<local-token>
```

### Production Configuration
```properties
# application-prod.properties
# Leave SonarQube settings empty/commented
# sonarqube.url=
# sonarqube.token=
# sonarqube.project-key=

# Quality dashboard will show:
# - Framework Compliance ✅ (works)
# - SonarQube section shows N/A or 0 values (graceful fallback)
```

**Best for:** Solo developers, small teams, cost-conscious projects

---

## Option 2: SonarCloud (Managed)

### Setup Steps

1. **Create SonarCloud Account**
   - Go to https://sonarcloud.io
   - Sign in with GitHub/Bitbucket/GitLab
   - Create organization

2. **Create Project**
   - Click "Analyze new project"
   - Select your repository
   - Project key: `strategiz_strategiz-core`

3. **Generate Token**
   - My Account > Security
   - Generate token with name "strategiz-production"
   - Copy token

4. **Configure Production Vault**
   ```bash
   # Production Vault (Cloud Run)
   vault kv put secret/strategiz/sonarqube \
     url="https://sonarcloud.io" \
     token="<your-sonarcloud-token>" \
     project-key="strategiz_strategiz-core"
   ```

5. **Run Analysis from CI/CD**
   ```yaml
   # .github/workflows/sonarcloud.yml
   name: SonarCloud Analysis
   on:
     push:
       branches: [main]
     pull_request:

   jobs:
     sonarcloud:
       runs-on: ubuntu-latest
       steps:
         - uses: actions/checkout@v3
         - name: Set up JDK 21
           uses: actions/setup-java@v3
           with:
             java-version: '21'
         - name: SonarCloud Scan
           env:
             SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
           run: |
             mvn clean verify sonar:sonar \
               -Dsonar.host.url=https://sonarcloud.io \
               -Dsonar.organization=strategiz \
               -Dsonar.projectKey=strategiz_strategiz-core
   ```

### Pricing
- **Free**: Public repositories
- **$10/month**: Up to 100k lines of code (private)
- **$75/month**: Up to 1M lines of code
- **$120/month**: Up to 2M lines of code

**Best for:** Teams who want zero infrastructure management

---

## Option 3: Google Compute Engine (Self-Hosted)

### Setup Steps

1. **Create VM Instance**
   ```bash
   gcloud compute instances create strategiz-sonarqube \
     --zone=us-east1-b \
     --machine-type=e2-medium \
     --boot-disk-size=50GB \
     --image-family=ubuntu-2204-lts \
     --image-project=ubuntu-os-cloud \
     --tags=sonarqube
   ```

2. **Configure Firewall**
   ```bash
   gcloud compute firewall-rules create allow-sonarqube \
     --allow tcp:9000 \
     --target-tags sonarqube \
     --source-ranges 0.0.0.0/0
   ```

3. **SSH and Install**
   ```bash
   gcloud compute ssh strategiz-sonarqube --zone=us-east1-b

   # On the VM:
   sudo apt update
   sudo apt install -y docker.io docker-compose

   # Copy your docker-compose.sonarqube.yml to VM
   # Start services
   sudo docker-compose -f docker-compose.sonarqube.yml up -d
   ```

4. **Get External IP**
   ```bash
   gcloud compute instances describe strategiz-sonarqube \
     --zone=us-east1-b \
     --format='get(networkInterfaces[0].accessConfigs[0].natIP)'
   ```

5. **Configure Production Vault**
   ```bash
   vault kv put secret/strategiz/sonarqube \
     url="http://<external-ip>:9000" \
     token="<your-sonarqube-token>" \
     project-key="strategiz-core"
   ```

### Security Hardening (Production)
```bash
# 1. Set up Cloud Armor or restrict firewall to your IPs only
gcloud compute firewall-rules update allow-sonarqube \
  --source-ranges=<your-office-ip>/32,<cloud-run-nat-ip>/32

# 2. Set up SSL with Let's Encrypt + nginx reverse proxy
# 3. Enable Cloud Monitoring and alerting
# 4. Set up automated backups for PostgreSQL
```

### Cost Breakdown
- **e2-medium VM**: $24.27/month (730 hours)
- **50GB Persistent Disk**: $8.00/month
- **Network Egress**: $1-5/month
- **Total**: ~$33-37/month

**Best for:** Teams wanting full control, medium+ size codebases

---

## Option 4: Cloud Run + Cloud SQL (Advanced)

**⚠️ NOT RECOMMENDED** - Complex setup, higher cost, stateful app on stateless platform

If you insist:
- SonarQube on Cloud Run (with persistent disk)
- Cloud SQL PostgreSQL instance
- Cost: $80-120/month
- Setup complexity: High

---

## Decision Matrix

### Choose Local Only if:
- ✅ Solo developer or small team (1-3 people)
- ✅ Budget is tight
- ✅ Framework Compliance metrics are sufficient
- ✅ Willing to run analysis manually

### Choose SonarCloud if:
- ✅ Want production dashboard with full metrics
- ✅ Don't want to manage infrastructure
- ✅ Have budget for $10-120/month
- ✅ Want PR quality checks automated

### Choose GCE if:
- ✅ Need full control over instance
- ✅ Have 5+ developers
- ✅ Want to minimize recurring costs ($30 vs $120)
- ✅ Have DevOps resources to maintain

---

## Current Recommendation: Start Local, Scale When Needed

```bash
# Phase 1: Local Development (Now)
./setup-sonarqube.sh
# Run scans locally

# Phase 2: When you have 2+ devs or want production metrics
# Option A: SonarCloud (easiest)
# Option B: GCE VM (cheapest self-hosted)

# Phase 3: When you have 10+ devs and complex needs
# Consider dedicated infrastructure or enterprise SonarQube
```

---

## What Your Quality Dashboard Shows Right Now

**Without SonarQube Integration:**
```
✅ Overall Grade: B+
✅ Compliance Score: 87.3%
✅ Framework Compliance:
   - Exception Handling: 89% (Grade: B+)
   - Service Pattern: 92% (Grade: A-)
   - Controller Pattern: 81% (Grade: B)
✅ Top 20 Violations with file paths

❌ SonarQube Rating: N/A
❌ Bugs: 0 (or "Not configured")
❌ Vulnerabilities: 0
❌ Code Smells: 0
```

**With SonarQube Integration:**
```
✅ Everything above, PLUS:
✅ SonarQube Rating: A
✅ Bugs: 12
✅ Vulnerabilities: 3
✅ Code Smells: 156
✅ Coverage: 68.4%
✅ Technical Debt: 2d 4h
```

---

## Migration Path

If you start local and want to move to production later:

```bash
# 1. Already have local setup ✅
# 2. Choose cloud option
# 3. Update Vault secrets (one command)
# 4. Backend automatically picks up new config
# 5. Quality dashboard shows cloud metrics

# No code changes needed! 🎉
```
