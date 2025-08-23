# 📖 Strategiz Documentation

Welcome to the Strategiz platform documentation! This comprehensive guide covers all aspects of the platform, from architecture and development to deployment and security.

## 📋 **Quick Navigation**

### 🎯 **Getting Started**
- [Developer Guide](development/developer-guide.md) - Complete guide for new developers
- [Architecture Overview](architecture/overview.md) - High-level system architecture
- [API Overview](api/overview.md) - API endpoints and usage

### 🏗️ **Architecture & Design**
- [Detailed Architecture](architecture/detailed.md) - In-depth architectural documentation
- [System Diagram](architecture/diagram.html) - Interactive architecture diagram
- [Base Classes Standards](development/base-classes-standards.md) - Development standards for controllers and services

### 🔧 **Development**
- [Developer Guide](development/developer-guide.md) - Complete development setup and workflow
- [Development Configuration](DEVELOPMENT-CONFIGURATION.md) - HTTP/HTTPS setup and environment configuration
- [Base Classes Standards](development/base-classes-standards.md) - BaseController and BaseService usage
- [Naming Conventions](development/naming-conventions.md) - Code and file naming standards
- [Scripts](development/scripts.md) - Available build and utility scripts
- [Gradle Migration](development/gradle-migration.md) - Maven to Gradle migration notes

### 🚀 **Deployment**
- [Deployment Overview](deployment/overview.md) - Deployment strategy and environments
- [Deployment Guide](deployment/guide.md) - Detailed deployment instructions
- [Deploy Scripts](deployment/deploy.md) - Automated deployment scripts and commands

### 🔒 **Security**
- [Security Overview](security/overview.md) - Security architecture and best practices
- [Secrets Management](security/secrets-management.md) - HashiCorp Vault integration and secret handling
- [Vault Setup](security/vault-setup.md) - Complete Vault setup guide
- [Vault Token Storage](security/vault-token-storage.md) - Token management best practices
- [Passkey Debug Info](security/passkey-debug-info.md) - WebAuthn/Passkey troubleshooting

### 🔌 **Integrations**
- [Exchange Integrations](integrations/exchanges.md) - Cryptocurrency exchange integrations

### 📡 **API Documentation**
- [API Overview](api/overview.md) - Complete API endpoint documentation
- [API Endpoints](api/endpoints.md) - Detailed endpoint specifications
- [Resource Paths](api/resource-paths.md) - Complete API resource path reference

---

## 🏗️ **Documentation Structure**

```
docs/
├── api/              # API documentation
│   ├── overview.md   # API overview and authentication
│   └── endpoints.md  # Detailed endpoint specifications
├── architecture/     # System architecture
│   ├── overview.md   # High-level architecture overview
│   ├── detailed.md   # Detailed architecture documentation
│   └── diagram.html  # Interactive architecture diagram
├── development/      # Development guides & standards
│   ├── developer-guide.md        # Complete development guide
│   ├── base-classes-standards.md # BaseController & BaseService standards
│   ├── naming-conventions.md     # Code and file naming standards
│   └── scripts.md               # Build and utility scripts
├── deployment/       # Deployment guides
│   ├── overview.md   # Deployment strategy overview
│   ├── guide.md      # Detailed deployment instructions
│   └── deploy.md     # Deployment scripts and commands
├── security/         # Security documentation
│   ├── overview.md           # Security architecture overview
│   └── secrets-management.md # Vault integration and secrets
└── integrations/     # Third-party integrations
    └── exchanges.md  # Cryptocurrency exchange integrations
```

---

## 🎯 **Documentation Standards**

### **File Naming Convention**
- **Lowercase with hyphens** for multi-word files: `base-classes-standards.md`
- **Descriptive names** that clearly indicate content
- **Consistent extensions** (.md for markdown, .html for HTML)

### **Folder Organization**
- **Logical grouping** by functionality/topic
- **Consistent folder names** (lowercase, descriptive)
- **Clear hierarchy** with main topics as top-level folders

### **Content Standards**
- **Clear headings** with emoji indicators for easy scanning
- **Comprehensive examples** for all code snippets
- **Cross-references** between related documents
- **Regular updates** to keep content current

---

## 🚀 **Popular Documentation Paths**

### **For New Developers**
1. [Developer Guide](development/developer-guide.md) - Start here
2. [Base Classes Standards](development/base-classes-standards.md) - Learn the patterns
3. [Architecture Overview](architecture/overview.md) - Understand the system
4. [API Overview](api/overview.md) - API basics

### **For DevOps/Deployment**
1. [Deployment Overview](deployment/overview.md) - Deployment strategy
2. [Security Overview](security/overview.md) - Security considerations
3. [Secrets Management](security/secrets-management.md) - Vault setup
4. [Deployment Guide](deployment/guide.md) - Step-by-step deployment

### **For Frontend Developers**
1. [API Overview](api/overview.md) - API authentication and usage
2. [API Endpoints](api/endpoints.md) - Available endpoints
3. [Architecture Overview](architecture/overview.md) - System understanding

### **For Integration Work**
1. [Exchange Integrations](integrations/exchanges.md) - Third-party integrations
2. [API Endpoints](api/endpoints.md) - Integration endpoints
3. [Security Overview](security/overview.md) - Integration security

---

## 📝 **Contributing to Documentation**

### **Adding New Documentation**
1. **Choose appropriate folder** based on content type
2. **Follow naming conventions** (lowercase-with-hyphens.md)
3. **Include in this README** under relevant sections
4. **Cross-reference** from related documents

### **Updating Existing Documentation**
1. **Keep content current** with code changes
2. **Update cross-references** when moving/renaming files
3. **Test all code examples** before committing
4. **Maintain consistent formatting** and style

### **Documentation Review Process**
1. **Technical accuracy** - Code examples must work
2. **Clarity** - Easy to understand for target audience
3. **Completeness** - Cover all necessary aspects
4. **Consistency** - Follow established patterns and style

---

## 🔍 **Quick Reference**

### **Development**
- [Base Classes Standards](development/base-classes-standards.md#quick-reference) - Controller & Service patterns
- [Naming Conventions](development/naming-conventions.md) - Code naming standards
- [Developer Guide](development/developer-guide.md) - Complete development workflow

### **Deployment**
- [Deployment Commands](deployment/deploy.md) - Common deployment commands
- [Environment Setup](deployment/guide.md) - Environment configuration
- [Secrets Setup](security/secrets-management.md) - Vault configuration

### **API**
- [Authentication](api/overview.md#authentication) - API authentication methods
- [Endpoints](api/endpoints.md) - Complete endpoint list
- [Error Codes](development/base-classes-standards.md#error-handling-standards) - Standard error responses

---

## 🎉 **Happy Building!**

This documentation is your comprehensive guide to the Strategiz platform. Whether you're developing, deploying, or integrating, you'll find everything you need here.

**Questions or suggestions?** Feel free to improve this documentation by following our [contribution guidelines](#contributing-to-documentation) above.

---

*Last updated: December 2024* 