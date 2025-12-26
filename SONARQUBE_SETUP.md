# SonarQube Setup Guide

This guide walks you through setting up a self-hosted SonarQube instance for the Strategiz platform.

## 🚀 Quick Start

```bash
# 1. Start SonarQube
./setup-sonarqube.sh

# 2. Access SonarQube
# Open http://localhost:9000
# Login: admin / admin (change password on first login)

# 3. Generate token (My Account > Security > Generate Token)

# 4. Store in Vault
export VAULT_ADDR=http://127.0.0.1:8200
export VAULT_TOKEN=<your-vault-token>

vault kv put secret/strategiz/sonarqube \
  url="http://localhost:9000" \
  token="<your-sonarqube-token>" \
  project-key="strategiz-core"

# 5. Run analysis
mvn clean verify sonar:sonar \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.token=<your-token>

# 6. Restart Spring Boot to load Vault config
mvn spring-boot:run -pl application-api
```

## 📋 What Gets Installed

- **SonarQube 10.8.1 Community**: The main code analysis server
- **PostgreSQL 15**: Database for SonarQube data persistence
- **Ports**: SonarQube runs on `http://localhost:9000`

## 🔧 Configuration Files

### 1. `docker-compose.sonarqube.yml`
Docker Compose configuration with:
- SonarQube Community Edition (latest)
- PostgreSQL database
- Persistent volumes for data retention
- Health checks for both services

### 2. `sonar-project.properties`
Project-specific SonarQube configuration:
- Project key: `strategiz-core`
- Java 21 source level
- Exclusions for test files, generated code, config files
- Source and binary directories

### 3. Vault Integration
The backend automatically loads SonarQube config from Vault:
- Path: `secret/strategiz/sonarqube`
- Required keys: `url`, `token`, `project-key`

## 🎯 What Gets Analyzed

SonarQube scans your codebase for:

### Code Quality Issues
- **Bugs**: Potential runtime errors
- **Vulnerabilities**: Security issues
- **Code Smells**: Maintainability problems
- **Duplications**: Repeated code blocks

### Metrics Tracked
- **Reliability Rating**: A-E based on bugs
- **Security Rating**: A-E based on vulnerabilities
- **Maintainability Rating**: A-E based on code smells
- **Coverage**: Test code coverage percentage
- **Technical Debt**: Time to fix all issues

## 🖥️ Quality Dashboard Integration

Your console quality dashboard (`http://localhost:3001/quality`) shows:
1. **Framework Compliance**: Custom rules (exception handling, service pattern)
2. **SonarQube Metrics**: Bugs, vulnerabilities, code smells from SonarQube API
3. **Combined Grade**: Overall quality score

## 🔍 Running Analysis

### Option 1: Maven Command (Recommended)
```bash
mvn clean verify sonar:sonar \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.token=<your-token>
```

### Option 2: Using sonar-scanner CLI
```bash
# Install sonar-scanner
brew install sonar-scanner  # macOS
# or download from https://docs.sonarqube.org/latest/analysis/scan/sonarscanner/

# Run scan
sonar-scanner \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.token=<your-token>
```

### Option 3: Integrate with CI/CD
Add to your GitHub Actions, GitLab CI, or Jenkins pipeline.

## 🛠️ Troubleshooting

### SonarQube won't start
```bash
# Check logs
docker logs strategiz-sonarqube

# Common fix: increase Docker memory to 4GB+
# Docker Desktop > Settings > Resources > Memory
```

### Analysis fails with memory error
```bash
# Increase Maven memory
export MAVEN_OPTS="-Xmx2048m"
mvn clean verify sonar:sonar ...
```

### Connection refused to Vault
```bash
# Make sure Vault is running
vault status

# Check Vault address
echo $VAULT_ADDR  # should be http://127.0.0.1:8200
```

### Backend doesn't show SonarQube metrics
```bash
# Verify Vault secrets are set
vault kv get secret/strategiz/sonarqube

# Check backend logs for SonarQube client errors
# Look for: "Loading SonarQube configuration from Vault..."
```

### Quality dashboard shows 0 bugs/code smells
```bash
# Make sure you've run at least one scan
mvn clean verify sonar:sonar ...

# Refresh the quality dashboard
# The data comes from SonarQube API in real-time
```

## 📊 Viewing Results

### In SonarQube UI
1. Open http://localhost:9000
2. Click on "strategiz-core" project
3. View detailed metrics, hotspots, and issues

### In Console Dashboard
1. Open http://localhost:3001/quality
2. See aggregated metrics with framework compliance
3. Click through to SonarQube for detailed analysis

## 🔐 Security Notes

### Local Development
- Default credentials: `admin/admin`
- Token stored in Vault (encrypted at rest)
- SonarQube only accessible on localhost

### Production
- Use strong admin password
- Generate project-specific tokens
- Store tokens in production Vault
- Consider SonarQube authentication (LDAP, SAML, etc.)
- Enable HTTPS with proper certificates
- Restrict network access with firewall rules

## 📝 Regular Maintenance

```bash
# Stop SonarQube
docker-compose -f docker-compose.sonarqube.yml down

# Stop and remove volumes (clean slate)
docker-compose -f docker-compose.sonarqube.yml down -v

# Update SonarQube
docker-compose -f docker-compose.sonarqube.yml pull
docker-compose -f docker-compose.sonarqube.yml up -d

# Backup database
docker exec strategiz-sonarqube-db pg_dump -U sonar sonarqube > sonarqube-backup.sql
```

## 🎓 Resources

- [SonarQube Documentation](https://docs.sonarqube.org/latest/)
- [Java Analysis Parameters](https://docs.sonarqube.org/latest/analyzing-source-code/languages/java/)
- [Quality Gates](https://docs.sonarqube.org/latest/user-guide/quality-gates/)
- [Vault Integration](https://www.vaultproject.io/docs)

## 💡 Tips

1. **Run analysis regularly**: Integrate into your Git workflow
2. **Fix issues early**: Address bugs and vulnerabilities ASAP
3. **Monitor trends**: Track quality improvements over time
4. **Set quality gates**: Enforce minimum standards for deployments
5. **Review hotspots**: Focus on security-sensitive code first
