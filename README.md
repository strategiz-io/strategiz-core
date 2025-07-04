# Strategiz Core Platform

Strategiz is a comprehensive financial platform for portfolio management, strategy development, and multi-exchange trading.

## 🚀 Quick Start

### Local Development
```bash
# Backend
cd scripts/local
./build-and-deploy.sh    # Linux/Mac
build-and-deploy.bat     # Windows

# Frontend
cd strategiz-ui
npm install && npm start
```

### Production Deployment
```bash
# Set OAuth credentials first
cd deployment
./deploy-to-cloud-run.ps1
```

## 📁 Project Structure

```
strategiz-core/
├── deployment/              # 🚀 All deployment configs
│   ├── deploy-to-cloud-run.ps1
│   ├── cloudbuild*.yaml
│   ├── firebase.json
│   └── README.md           # Deployment guide
├── scripts/local/          # 🛠️ Local development
├── docs/                   # 📚 All documentation
├── observability/          # 📊 Monitoring configs
├── application/            # 🎯 Main Spring Boot app
├── service/               # 🔧 Microservices
├── business/              # 💼 Business logic
├── data/                  # 📄 Data layer
├── client/                # 🌐 External API clients
├── framework/             # 🏗️ Common frameworks
└── strategiz-ui/          # 💻 React frontend
```

## 🔧 Core Features

### Authentication
- **Passkey Authentication** - Passwordless login with WebAuthn
- **OAuth Integration** - Google, Facebook login
- **Multi-Factor Authentication** - TOTP, SMS, Email OTP

### Portfolio Management
- **Multi-Exchange Support** - Coinbase, Kraken, Binance.US
- **Real-time Market Data** - Live prices and portfolio tracking
- **Strategy Development** - Custom trading strategies

### Architecture
- **Microservices** - Modular service architecture
- **Spring Boot** - Java 21 backend
- **React** - TypeScript frontend
- **Firebase** - Firestore database
- **Google Cloud** - Cloud Run deployment

## 📖 Documentation

- **[Deployment Guide](deployment/README.md)** - All deployment options
- **[Architecture](docs/ARCHITECTURE.md)** - System architecture
- **[API Documentation](docs/API_ENDPOINTS.md)** - REST API reference
- **[Security](docs/SECURITY.md)** - Security implementation
- **[Developer Guide](docs/DEVELOPER_GUIDE.md)** - Development setup

## 🛠️ Development

### Backend Requirements
- Java 21
- Maven 3.8+
- Docker (for deployment)

### Frontend Requirements
- Node.js 18+
- npm or yarn

### Environment Setup
```bash
# Required OAuth credentials
export AUTH_GOOGLE_CLIENT_ID="your-google-client-id"
export AUTH_GOOGLE_CLIENT_SECRET="your-google-client-secret"
export AUTH_FACEBOOK_CLIENT_ID="your-facebook-client-id"
export AUTH_FACEBOOK_CLIENT_SECRET="your-facebook-client-secret"
```

## 🚀 Deployment

### Local Development
```bash
cd scripts/local && ./build-and-deploy.sh
```

### Production (Google Cloud Run)
```bash
cd deployment && ./deploy-to-cloud-run.ps1
```

### CI/CD Pipeline
```bash
gcloud builds submit --config deployment/cloudbuild.yaml
```

## 📊 Monitoring

- **Prometheus** - Metrics collection
- **Grafana** - Visualization dashboards
- **Cloud Monitoring** - Google Cloud metrics
- **Actuator** - Spring Boot health checks

## 🔐 Security

- **OAuth 2.0** - Secure authentication
- **PASETO Tokens** - Secure stateless tokens
- **WebAuthn** - Passwordless authentication
- **CORS Configuration** - Cross-origin security
- **Environment Variables** - Secure configuration

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Run tests and ensure they pass
5. Submit a pull request

## 📝 License

This project is proprietary software. All rights reserved.

## 🆘 Support

For issues and questions:
- Check the [documentation](docs/)
- Review [troubleshooting](deployment/README.md#troubleshooting)
- Contact the development team