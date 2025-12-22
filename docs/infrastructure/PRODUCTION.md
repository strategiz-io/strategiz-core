# Strategiz Production Infrastructure

## Overview

Strategiz runs on Google Cloud Platform (GCP) with a 3-tier architecture:
- **Presentation Tier**: Firebase Hosting (static frontends)
- **Application Tier**: Google Cloud Run (containerized backend services)
- **Data Tier**: Firebase Firestore + TimescaleDB

## Google Cloud Project

| Property | Value |
|----------|-------|
| Project ID | `strategiz-io` |
| Primary Region | `us-east1` |

---

## Presentation Tier (Frontend)

### Firebase Hosting Sites

| Site | Domain | Description |
|------|--------|-------------|
| strategiz-io | https://strategiz.io | Main web application |
| strategiz-console | https://console.strategiz.io | Admin console |
| strategiz-docs | https://strategiz-docs.web.app | Documentation |

### Frontend Stack
- React 19 with TypeScript
- Material-UI (MUI) v7
- Redux Toolkit for state management
- Turborepo monorepo (apps/web, apps/console)

---

## Application Tier (Backend)

### Cloud Run Services

| Service | URL | Description |
|---------|-----|-------------|
| strategiz-api | https://strategiz-api-43628135674.us-east1.run.app | Main API backend |
| strategiz-vault | https://strategiz-vault-43628135674.us-east1.run.app | HashiCorp Vault (secrets management) |

### Custom Domain Mapping

| Domain | Target Service |
|--------|----------------|
| api.strategiz.io | strategiz-api |

### Backend Stack
- Java 21
- Spring Boot 3.5.7
- Maven build system

### Key Backend Modules
```
application-api/      # Main Spring Boot application (REST API entry point)
service-*/            # REST API controllers (including service-console for admin)
business-*/           # Business logic layer
data-*/               # Repository interfaces
client-*/             # External service integrations
framework-*/          # Core utilities
```

---

## Data Tier (Databases)

### Firebase Firestore

| Property | Value |
|----------|-------|
| Mode | Native |
| Location | Multi-region |
| Edition | Standard (Free Tier) |

**Collections:**
- `users/` - User profiles and preferences
- `users/{userId}/providers/` - Connected trading providers
- `users/{userId}/subscription/` - Subscription information
- `users/{userId}/preferences/` - User preferences (AI, etc.)

### TimescaleDB (Timescale Cloud)

| Property | Value |
|----------|-------|
| Provider | Timescale Cloud |
| Version | 2.24.0 |
| Purpose | Time-series market data storage |

**Tables:**
- `market_data_bars` - OHLCV price data with hypertable partitioning

---

## Secrets Management

### HashiCorp Vault

| Property | Value |
|----------|-------|
| Deployment | Cloud Run (strategiz-vault) |
| Storage | Integrated Raft storage |
| Version | 1.20.0 |

**Secret Paths:**
```
secret/data/strategiz/
├── alpaca/           # Alpaca API credentials
├── binanceus/        # Binance.US API credentials
├── kraken/           # Kraken API credentials
├── coinbase/         # Coinbase API credentials
├── timescale/        # TimescaleDB connection
├── firebase/         # Firebase service account
├── google/           # Google OAuth credentials
├── facebook/         # Facebook OAuth credentials
├── grafana/          # Grafana Cloud OTLP credentials
└── claude/           # Claude AI API credentials
```

---

## Observability

### Grafana Cloud

| Component | Purpose |
|-----------|---------|
| OTLP Endpoint | OpenTelemetry traces and metrics |
| Trace Sampling | 10% in production |

---

## External Integrations

### Trading Providers
| Provider | Connection Type |
|----------|-----------------|
| Alpaca | OAuth + API Key |
| Kraken | API Key |
| Binance.US | API Key |
| Coinbase | OAuth |

### AI/LLM Providers
| Provider | Models |
|----------|--------|
| Google Vertex AI | Gemini 2.0 Flash, Gemini 3 Flash/Pro |
| Anthropic (via Vertex) | Claude Opus 4.5, Claude Sonnet 4, Claude Haiku 4.5 |

### OAuth Providers
- Google
- Facebook
- Microsoft (planned)
- GitHub (planned)

---

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                     PRESENTATION TIER                            │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐  │
│  │  strategiz.io   │  │ console.strat.. │  │ docs.strat...   │  │
│  │ (Firebase Host) │  │ (Firebase Host) │  │ (Firebase Host) │  │
│  └────────┬────────┘  └────────┬────────┘  └─────────────────┘  │
└───────────┼────────────────────┼────────────────────────────────┘
            │                    │
            ▼                    ▼
┌─────────────────────────────────────────────────────────────────┐
│                     APPLICATION TIER                             │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │                    api.strategiz.io                         ││
│  │              (Cloud Run: strategiz-api)                     ││
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────┐   ││
│  │  │   Auth   │ │ Provider │ │  Market  │ │   AI/Chat    │   ││
│  │  │ Service  │ │ Service  │ │  Data    │ │   Service    │   ││
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────────┘   ││
│  └─────────────────────────────────────────────────────────────┘│
│  ┌─────────────────────────────────────────────────────────────┐│
│  │              strategiz-vault (Cloud Run)                    ││
│  │                   HashiCorp Vault                           ││
│  └─────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────┘
            │                    │
            ▼                    ▼
┌─────────────────────────────────────────────────────────────────┐
│                        DATA TIER                                 │
│  ┌─────────────────────────┐  ┌─────────────────────────────┐   │
│  │    Firebase Firestore   │  │      TimescaleDB            │   │
│  │    (User data, etc.)    │  │   (Market data time-series) │   │
│  └─────────────────────────┘  └─────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

---

## Local Development

### Prerequisites
- Java 21
- Maven 3.8+
- Node.js 18+
- HashiCorp Vault (dev mode)

### Quick Start

```bash
# Terminal 1: Start Vault
vault server -dev

# Terminal 2: Start Backend
cd strategiz-core
VAULT_TOKEN=root mvn spring-boot:run -pl application-api
# Backend runs on https://localhost:8443

# Terminal 3: Start Frontend
cd strategiz-ui
npm run dev:web
# Frontend runs on http://localhost:3000
```

---

## CI/CD

### Cloud Build
- Triggered on push to main branch
- Builds Docker image and deploys to Cloud Run
- Config: `cloudbuild.yaml`

### Firebase Deploy
```bash
# Deploy frontend
cd strategiz-ui
npm run build
firebase deploy --only hosting
```
