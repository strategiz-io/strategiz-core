#!/usr/bin/env node

const fs = require('fs');
const path = require('path');
const axios = require('axios');

// Configuration
const CORE_REPO = 'strategiz-io/strategiz-core';
const BRANCH = 'main';
const DOCS_DIR = 'docs';

// GitHub API configuration
const GITHUB_API_BASE = 'https://api.github.com';
const GITHUB_RAW_BASE = 'https://raw.githubusercontent.com';

// Mapping of source files to destination paths
// Only including files that currently exist in the repository
const DOC_MAPPINGS = {
  // Authentication docs (files that exist)
  'service/service-auth/docs/TOTP.md': 'docs/auth/totp.md',
  
  // Architecture docs (files that exist)
  'docs/ARCHITECTURE.md': 'docs/architecture/overview.md',
  'docs/API_ENDPOINTS.md': 'docs/api/endpoints.md',
  'docs/DEPLOY.md': 'docs/deployment/overview.md',
  
  // Module specific docs (files that exist)
  'client/client-coinbase/COINBASE-INTEGRATION.md': 'docs/api/coinbase.md',
  'framework/framework-exception/README.md': 'docs/architecture/exception-handling.md',
  'framework/framework-logging/README.md': 'docs/architecture/logging.md',
  'service/service-device/README.md': 'docs/api/device.md',
  'service/service-provider/README.md': 'docs/api/provider.md',
  'data/data-user/README.md': 'docs/api/user.md',
  'business/business-token-auth/README.md': 'docs/auth/token-auth.md',
  
  // Root docs
  'README.md': 'docs/intro.md',
};

// Function to ensure directory exists
function ensureDirectoryExists(filePath) {
  const dir = path.dirname(filePath);
  if (!fs.existsSync(dir)) {
    fs.mkdirSync(dir, { recursive: true });
  }
}

// Function to add Docusaurus frontmatter
function addDocusaurusFrontmatter(content, title, description) {
  const frontmatter = `---
title: ${title}
description: ${description}
---

`;
  return frontmatter + content;
}

// Function to process markdown content
function processMarkdownContent(content, sourcePath) {
  // Extract title from first h1 heading or use filename
  const h1Match = content.match(/^# (.+)$/m);
  const title = h1Match ? h1Match[1] : path.basename(sourcePath, '.md');
  
  // Extract description from first paragraph after title
  const descriptionMatch = content.match(/^# .+$\n\n(.+)$/m);
  const description = descriptionMatch ? descriptionMatch[1].substring(0, 160) : `Documentation for ${title}`;
  
  // Add frontmatter
  let processedContent = addDocusaurusFrontmatter(content, title, description);
  
  // Fix relative links to point to GitHub for now
  processedContent = processedContent.replace(
    /\]\((?!https?:\/\/)([^)]+)\)/g,
    `](https://github.com/${CORE_REPO}/blob/${BRANCH}/$1)`
  );
  
  // Update terminology: exchange -> provider, Redis -> Firestore, MongoDB -> Firestore
  processedContent = processedContent.replace(/exchange API keys/gi, 'provider API keys');
  processedContent = processedContent.replace(/EXCHANGE_API_KEYS/g, 'PROVIDER_API_KEYS');
  processedContent = processedContent.replace(/Redis/g, 'Firestore');
  processedContent = processedContent.replace(/REDIS_URL/g, 'FIREBASE_PROJECT_ID');
  processedContent = processedContent.replace(/MongoDB/g, 'Firestore');
  processedContent = processedContent.replace(/MONGO_URL/g, 'FIREBASE_PROJECT_ID');
  
  return processedContent;
}

// Function to fetch file from GitHub
async function fetchFileFromGitHub(filePath) {
  try {
    const url = `${GITHUB_RAW_BASE}/${CORE_REPO}/${BRANCH}/${filePath}`;
    console.log(`Fetching: ${url}`);
    const response = await axios.get(url);
    return response.data;
  } catch (error) {
    console.error(`Error fetching ${filePath}:`, error.message);
    return null;
  }
}

// Function to write file to local docs
function writeLocalFile(content, destinationPath) {
  ensureDirectoryExists(destinationPath);
  fs.writeFileSync(destinationPath, content, 'utf8');
  console.log(`✅ Written: ${destinationPath}`);
}

// Function to create placeholder documentation
function createPlaceholderDocs() {
  console.log('📝 Creating placeholder documentation...');
  
  const placeholders = [
    {
      path: 'docs/intro.md',
      content: `---
title: Introduction
description: Welcome to Strategiz Documentation
---

# Welcome to Strategiz

Strategiz is a comprehensive trading platform built with modern microservices architecture.

## Features

- **Multi-Factor Authentication**: Secure login with TOTP, OAuth, SMS, and more
- **Portfolio Management**: Track and manage your trading portfolios
- **Real-time Data**: Live market data and trading signals
- **Strategy Automation**: Automated trading strategies and backtesting

## Getting Started

Visit our [Authentication Guide](./auth/totp) to get started with secure login.
`
    },
    {
      path: 'docs/auth/overview.md',
      content: `---
title: Authentication Overview
description: Overview of Strategiz authentication methods
---

# Authentication Overview

Strategiz provides multiple authentication methods to ensure secure access to your trading platform.

## Available Methods

- **TOTP (Time-based One-Time Password)**: Two-factor authentication using apps like Google Authenticator
- **OAuth**: Social login with Google, Facebook, and other providers
- **SMS**: Phone-based verification
- **Email OTP**: Email-based authentication codes
- **Passkey**: Modern WebAuthn/FIDO2 authentication

## Security Features

- End-to-end encryption
- Secure token management
- Device fingerprinting
- Session management

Learn more about [TOTP Authentication](./totp) for detailed implementation.
`
    },
    {
      path: 'docs/architecture/overview.md',
      content: `---
title: Architecture Overview
description: Strategiz platform architecture and design principles
---

# Architecture Overview

Strategiz is built using a microservices architecture with clear separation of concerns.

## Core Principles

- **Microservices**: Independent, scalable services
- **Domain-Driven Design**: Business logic organization
- **Event-Driven**: Asynchronous communication
- **SOLID Principles**: Clean, maintainable code

## Services

- **Authentication Service**: User authentication and authorization
- **Portfolio Service**: Portfolio management and tracking
- **Strategy Service**: Trading strategy execution
- **Provider Service**: Financial data and trading provider integration
- **User Service**: User profile and preferences

## Technology Stack

- **Backend**: Java with Spring Boot
- **Frontend**: React with TypeScript
- **Database**: PostgreSQL with Firebase Firestore
- **Message Queue**: Apache Kafka
- **Infrastructure**: Docker and Kubernetes
`
    },
    {
      path: 'docs/api/endpoints.md',
      content: `---
title: API Endpoints
description: Complete API reference for Strategiz platform
---

# API Endpoints

This section provides comprehensive API documentation for all Strategiz services.

## Authentication API

- \`POST /auth/signin\` - User sign in
- \`POST /auth/signup\` - User registration
- \`POST /auth/totp/setup/initialize\` - Initialize TOTP setup
- \`POST /auth/totp/setup/complete\` - Complete TOTP setup
- \`POST /auth/totp/authenticate\` - Authenticate with TOTP

## Portfolio API

- \`GET /portfolio\` - Get user portfolios
- \`POST /portfolio\` - Create new portfolio
- \`PUT /portfolio/{id}\` - Update portfolio
- \`DELETE /portfolio/{id}\` - Delete portfolio

## Strategy API

- \`GET /strategy\` - Get user strategies
- \`POST /strategy\` - Create new strategy
- \`PUT /strategy/{id}/execute\` - Execute strategy

## Provider API

- \`GET /provider\` - Get available providers
- \`POST /provider/connect\` - Connect to a provider
- \`DELETE /provider/{id}/disconnect\` - Disconnect from provider

## Base URL

All API endpoints are relative to: \`https://api.strategiz.io/v1\`

## Authentication

All API calls require a valid JWT token in the Authorization header:

\`\`\`
Authorization: Bearer <your-jwt-token>
\`\`\`
`
    },
    {
      path: 'docs/deployment/overview.md',
      content: `---
title: Deployment Overview
description: How to deploy Strategiz platform
---

# Deployment Overview

Strategiz can be deployed using multiple methods depending on your requirements.

## Deployment Options

### Docker Compose
Quick local deployment for development and testing.

### Kubernetes
Production-ready deployment with auto-scaling and high availability.

### Cloud Providers
- Google Cloud Platform
- Amazon Web Services
- Microsoft Azure

## Prerequisites

- Docker and Docker Compose
- Kubernetes cluster (for K8s deployment)
- PostgreSQL database
- Firebase project with Firestore enabled

## Environment Variables

Required environment variables:
- \`DATABASE_URL\` - PostgreSQL database connection string
- \`JWT_SECRET\` - JWT token secret
- \`FIREBASE_PROJECT_ID\` - Firebase project identifier
- \`FIREBASE_PRIVATE_KEY\` - Firebase service account private key
- \`FIREBASE_CLIENT_EMAIL\` - Firebase service account email
- \`PROVIDER_API_KEYS\` - Provider API credentials (Coinbase, Binance, etc.)

## Quick Start

1. Clone the repository
2. Configure environment variables
3. Set up Firebase project and Firestore
4. Run \`docker-compose up\`
5. Access the application at \`http://localhost:3000\`
`
    }
  ];

  placeholders.forEach(({ path, content }) => {
    ensureDirectoryExists(path);
    fs.writeFileSync(path, content, 'utf8');
    console.log(`✅ Created: ${path}`);
  });
}

// Main sync function
async function syncDocs() {
  console.log('🔄 Starting documentation sync...');
  
  let successCount = 0;
  let errorCount = 0;
  
  for (const [sourcePath, destinationPath] of Object.entries(DOC_MAPPINGS)) {
    try {
      const content = await fetchFileFromGitHub(sourcePath);
      if (content) {
        const processedContent = processMarkdownContent(content, sourcePath);
        writeLocalFile(processedContent, destinationPath);
        successCount++;
      } else {
        console.warn(`⚠️  Skipping ${sourcePath} (not found)`);
        errorCount++;
      }
    } catch (error) {
      console.error(`❌ Error processing ${sourcePath}:`, error.message);
      errorCount++;
    }
  }
  
  // Create placeholder documentation for missing files
  if (errorCount > 0) {
    createPlaceholderDocs();
  }
  
  console.log(`\n📊 Sync complete:`);
  console.log(`   ✅ Success: ${successCount}`);
  console.log(`   ❌ Errors: ${errorCount}`);
  console.log(`   📝 Placeholders created for missing files`);
  
  console.log('🎉 Documentation sync complete!');
}

// Run the sync
if (require.main === module) {
  syncDocs().catch(console.error);
}

module.exports = { syncDocs }; 