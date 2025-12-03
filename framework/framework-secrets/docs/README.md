<p align="center">
  <img src="https://img.shields.io/badge/HashiCorp-Vault-000000?style=for-the-badge&logo=vault" alt="Vault"/>
  <img src="https://img.shields.io/badge/Spring%20Cloud-Vault-6DB33F?style=for-the-badge&logo=spring" alt="Spring Cloud"/>
</p>

# Secrets Management Framework

> **HashiCorp Vault integration for secure secret management in Strategiz Core**

---

## Overview

This module provides secure secret management using HashiCorp Vault, enabling:

- Centralized secret storage
- Dynamic secret injection
- Environment-specific configurations
- Secure OAuth credential management

```
┌─────────────────────────────────────────────────────────────────────┐
│                                                                     │
│   Application                     HashiCorp Vault                   │
│   ═══════════                     ═══════════════                   │
│                                                                     │
│   @Value("${...}")  ◄──────────►  secret/strategiz/                 │
│                                   ├── oauth/google                  │
│   VaultTemplate     ◄──────────►  ├── oauth/coinbase                │
│                                   ├── tokens/dev                    │
│   Environment       ◄──────────►  └── gemini/api-key                │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Quick Start

### 1. Add Dependency

```xml
<dependency>
    <groupId>io.strategiz</groupId>
    <artifactId>framework-secrets</artifactId>
    <version>${project.version}</version>
</dependency>
```

### 2. Configuration

```properties
# Vault Connection
strategiz.vault.enabled=true
strategiz.vault.address=${VAULT_ADDR:http://localhost:8200}
strategiz.vault.secrets-path=secret
strategiz.vault.fail-fast=false
strategiz.vault.fallback-to-properties=true

# Spring Cloud Vault
spring.cloud.vault.authentication=TOKEN
spring.cloud.vault.uri=${VAULT_ADDR:http://localhost:8200}
spring.cloud.vault.token=${VAULT_TOKEN:}
spring.cloud.vault.kv.enabled=true
spring.cloud.vault.kv.backend=secret
spring.cloud.vault.kv.default-context=strategiz
```

### 3. Set Environment Variables

```bash
export VAULT_ADDR=http://localhost:8200
export VAULT_TOKEN=your-vault-token
```

---

## Vault Secret Structure

```
secret/strategiz/
├── oauth/
│   ├── google
│   │   ├── client-id
│   │   └── client-secret
│   ├── facebook
│   │   ├── client-id
│   │   └── client-secret
│   ├── coinbase
│   │   ├── client-id
│   │   ├── client-secret
│   │   └── redirect-uri-local
│   ├── alpaca
│   │   ├── client-id
│   │   ├── client-secret
│   │   ├── api-url
│   │   └── scope
│   ├── schwab
│   │   ├── client-id
│   │   └── client-secret
│   └── kraken
│       ├── client-id
│       └── client-secret
├── tokens/
│   ├── dev
│   │   ├── session-key      # PASETO signing key (base64)
│   │   └── identity-key     # PASETO identity key (base64)
│   └── prod
│       ├── session-key
│       └── identity-key
└── gemini/
    └── api-key              # Google Gemini API key
```

---

## Usage Patterns

### Property Injection

```java
@Service
public class OAuthService {

    @Value("${oauth.providers.google.client-id}")
    private String googleClientId;

    @Value("${oauth.providers.google.client-secret}")
    private String googleClientSecret;
}
```

### VaultTemplate (Programmatic Access)

```java
@Service
public class DynamicSecretService {

    @Autowired
    private VaultTemplate vaultTemplate;

    public String getSecret(String path) {
        VaultResponse response = vaultTemplate.read("secret/data/strategiz/" + path);
        return (String) response.getData().get("data").get("value");
    }
}
```

### Configuration Properties

```java
@ConfigurationProperties(prefix = "oauth.providers.coinbase")
public class CoinbaseOAuthProperties {
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    // getters/setters
}
```

---

## Local Development Setup

### 1. Start Vault in Dev Mode

```bash
# Start Vault (dev mode with in-memory storage)
vault server -dev

# Note the root token printed in output
# Root Token: hvs.xxxxxxxxxxxxx
```

### 2. Configure Environment

```bash
export VAULT_ADDR=http://localhost:8200
export VAULT_TOKEN=root  # or the token from dev mode output
```

### 3. Add Required Secrets

```bash
# OAuth Providers
vault kv put secret/strategiz/oauth/google \
  client-id="your-google-client-id" \
  client-secret="your-google-client-secret"

vault kv put secret/strategiz/oauth/facebook \
  client-id="your-facebook-client-id" \
  client-secret="your-facebook-client-secret"

vault kv put secret/strategiz/oauth/coinbase \
  client-id="your-coinbase-client-id" \
  client-secret="your-coinbase-client-secret" \
  redirect-uri-local="https://localhost:8443/v1/providers/callback/cb"

# PASETO Token Keys (generate with: openssl rand -base64 32)
vault kv put secret/strategiz/tokens/dev \
  session-key="$(openssl rand -base64 32)" \
  identity-key="$(openssl rand -base64 32)"

# AI Integration
vault kv put secret/strategiz/gemini \
  api-key="your-gemini-api-key"
```

### 4. Verify Secrets

```bash
# List all secrets
vault kv list secret/strategiz

# Read a specific secret
vault kv get secret/strategiz/oauth/google
```

---

## Production Setup

### 1. Use Proper Authentication

```properties
# Token auth (simplest)
spring.cloud.vault.authentication=TOKEN
spring.cloud.vault.token=${VAULT_TOKEN}

# AppRole auth (recommended for production)
spring.cloud.vault.authentication=APPROLE
spring.cloud.vault.app-role.role-id=${VAULT_ROLE_ID}
spring.cloud.vault.app-role.secret-id=${VAULT_SECRET_ID}

# Kubernetes auth (for K8s deployments)
spring.cloud.vault.authentication=KUBERNETES
spring.cloud.vault.kubernetes.role=strategiz-core
spring.cloud.vault.kubernetes.kubernetes-path=kubernetes
```

### 2. Enable TLS

```properties
spring.cloud.vault.ssl.trust-store=classpath:vault-truststore.jks
spring.cloud.vault.ssl.trust-store-password=${VAULT_TRUSTSTORE_PASSWORD}
```

### 3. Set Up Policies

```hcl
# strategiz-policy.hcl
path "secret/data/strategiz/*" {
  capabilities = ["read", "list"]
}

path "secret/metadata/strategiz/*" {
  capabilities = ["read", "list"]
}
```

```bash
vault policy write strategiz strategiz-policy.hcl
```

---

## Fallback Behavior

When Vault is unavailable, the framework can fall back to property files:

```properties
# Enable fallback (not recommended for production)
strategiz.vault.fail-fast=false
strategiz.vault.fallback-to-properties=true

# Fallback values in application.properties
oauth.providers.google.client-id=fallback-value
```

---

## Troubleshooting

### Connection Issues

```bash
# Check Vault status
vault status

# Verify token
vault token lookup

# Test connection
curl -H "X-Vault-Token: $VAULT_TOKEN" \
  $VAULT_ADDR/v1/secret/data/strategiz/oauth/google
```

### Secret Not Found

```bash
# List available secrets
vault kv list secret/strategiz/oauth

# Check secret exists
vault kv get secret/strategiz/oauth/google
```

### Permission Denied

```bash
# Check token capabilities
vault token capabilities secret/data/strategiz/oauth/google

# Check policy
vault policy read strategiz
```

### Enable Debug Logging

```properties
logging.level.org.springframework.cloud.vault=DEBUG
logging.level.org.springframework.vault=DEBUG
```

---

## Security Best Practices

### 1. Never Commit Secrets

```gitignore
# .gitignore
*.token
.vault-token
application-secrets.properties
```

### 2. Use Short-Lived Tokens

```bash
# Create token with TTL
vault token create -ttl=1h -policy=strategiz
```

### 3. Rotate Secrets Regularly

```bash
# Update a secret
vault kv put secret/strategiz/tokens/prod \
  session-key="$(openssl rand -base64 32)" \
  identity-key="$(openssl rand -base64 32)"
```

### 4. Audit Secret Access

```bash
# Enable audit logging
vault audit enable file file_path=/var/log/vault/audit.log
```

---

## Environment-Specific Configuration

### Development

```properties
# application-dev.properties
strategiz.vault.address=http://localhost:8200
strategiz.vault.fail-fast=false
```

### Production

```properties
# application-prod.properties
strategiz.vault.address=https://vault.strategiz.io:8200
strategiz.vault.fail-fast=true
strategiz.vault.fallback-to-properties=false
```

---

## Related Documentation

- [Authorization Framework](../../framework-authorization/docs/README.md) - Uses token keys from Vault
- [Framework Documentation Hub](../../docs/README.md)
- [HashiCorp Vault Documentation](https://developer.hashicorp.com/vault/docs)
- [Spring Cloud Vault Reference](https://spring.io/projects/spring-cloud-vault)
