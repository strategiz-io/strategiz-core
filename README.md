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

# Strategiz Documentation

[![Deploy to GitHub Pages](https://github.com/strategiz-io/strategiz-docs/actions/workflows/sync-and-deploy.yml/badge.svg)](https://github.com/strategiz-io/strategiz-docs/actions/workflows/sync-and-deploy.yml)

Welcome to the **Strategiz Documentation** repository! This site provides comprehensive documentation for the Strategiz trading platform, built with [Docusaurus](https://docusaurus.io/) and automatically synced from the main [strategiz-core](https://github.com/strategiz-io/strategiz-core) repository.

## 📖 Live Documentation

Visit the live documentation site: **[https://strategiz-io.github.io/strategiz-docs/](https://strategiz-io.github.io/strategiz-docs/)**

## 🏗️ Architecture

This documentation site follows a **hybrid approach**:

- **📝 Source of Truth**: Documentation files remain in the main `strategiz-core` repository alongside the code
- **🔄 Automatic Sync**: GitHub Actions automatically pulls and processes documentation files
- **🌐 Beautiful Presentation**: Docusaurus provides a professional, searchable documentation website

## 🚀 Quick Start

### Local Development

1. **Install Dependencies**
   ```bash
   npm install
   ```

2. **Sync Documentation**
   ```bash
   npm run sync-docs
   ```

3. **Start Local Development Server**
   ```bash
   npm start
   ```

4. **Build for Production**
   ```bash
   npm run build
   ```

### Automatic Deployment

The documentation is automatically deployed to GitHub Pages when:
- Changes are pushed to the `main` branch
- Daily at 6 AM UTC (to catch updates from the main repo)
- Manually triggered via GitHub Actions

## 📁 Documentation Structure

The documentation is organized into the following sections:

### 🏛️ Architecture
- **Overview**: High-level system architecture
- **Microservices**: Service boundaries and communication
- **Database**: Data models and relationships

### 🔐 Authentication
- **TOTP**: Time-based One-Time Password implementation
- **OAuth**: Social login integration
- **SMS**: SMS-based authentication
- **Email OTP**: Email-based verification
- **Passkey**: WebAuthn/FIDO2 implementation

### 🔌 API Reference
- **Endpoints**: Complete API documentation
- **Authentication**: API authentication methods
- **Services**: Individual service APIs

### 🚀 Deployment
- **Docker**: Container deployment
- **Kubernetes**: Orchestration setup
- **Cloud**: Cloud provider configurations

## 🔄 Synchronization Process

The sync process works as follows:

1. **GitHub Actions Trigger**: On schedule, push, or manual trigger
2. **Document Fetching**: Pulls latest markdown files from `strategiz-core`
3. **Processing**: Adds Docusaurus frontmatter and fixes links
4. **Site Building**: Generates static site with Docusaurus
5. **Deployment**: Deploys to GitHub Pages
6. **Commit**: Commits synced docs back to repository

## 📋 Mapped Documentation Files

The following files are automatically synced:

| Source (strategiz-core) | Destination (strategiz-docs) |
|-------------------------|------------------------------|
| `docs/ARCHITECTURE.md` | `docs/architecture/overview.md` |
| `docs/API_ENDPOINTS.md` | `docs/api/endpoints.md` |
| `docs/DEPLOY.md` | `docs/deployment/overview.md` |
| `service/service-auth/docs/TOTP.md` | `docs/auth/totp.md` |
| `service/service-auth/docs/OAuth.md` | `docs/auth/oauth.md` |
| `README.md` | `docs/intro.md` |
| *...and more* | *See scripts/sync-docs.js* |

## 🛠️ Configuration

### Adding New Documentation

To add new documentation files to the sync process:

1. Add the mapping to `scripts/sync-docs.js` in the `DOC_MAPPINGS` object
2. Update the sidebar configuration in `sidebars.js`
3. The next sync will automatically include the new files

### Customizing the Site

- **Branding**: Update `docusaurus.config.js` for site metadata
- **Styling**: Modify `src/css/custom.css` for custom styles
- **Homepage**: Edit `src/pages/index.js` for homepage content
- **Navigation**: Update `sidebars.js` for documentation structure

## 🤝 Contributing

This repository primarily serves as a documentation presentation layer. For content changes:

1. **Documentation Content**: Edit files in the main `strategiz-core` repository
2. **Site Configuration**: Submit PRs to this repository for presentation changes
3. **New Documentation**: Add new files to the main repo and update sync configuration

## 📝 Scripts

- `npm start`: Start local development server
- `npm run build`: Build for production
- `npm run sync-docs`: Manually sync documentation from main repo
- `npm run clear`: Clear Docusaurus cache

## 📊 Features

- **📱 Responsive Design**: Works on all devices
- **🔍 Search**: Built-in search functionality
- **🌙 Dark Mode**: Light/dark theme toggle
- **📖 Versioning**: Support for documentation versions
- **🔗 Link Validation**: Automatic broken link detection
- **📈 Analytics**: Google Analytics integration ready

## 🆘 Support

For issues with:
- **Documentation Content**: Create issues in [strategiz-core](https://github.com/strategiz-io/strategiz-core/issues)
- **Site Functionality**: Create issues in this repository
- **Questions**: Use [GitHub Discussions](https://github.com/strategiz-io/strategiz-core/discussions)

## 📄 License

This project is licensed under the same terms as the main Strategiz platform.

---

**🎯 Maintained by the Strategiz Team** | **📧 Contact**: [team@strategiz.io](mailto:team@strategiz.io)