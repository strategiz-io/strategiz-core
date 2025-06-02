# Strategiz Core

Server-side API and services for the Strategiz platform. This repository contains all the backend code for handling exchange API integrations, user authentication, and data processing.

## Features

- **Exchange API Integrations**:
  - Binance US: Complete API integration with rate limiting and caching
  - Kraken: Complete API integration with rate limiting and caching
  - Coinbase: Complete API integration with rate limiting and caching
  - Uniswap: Complete integration with Ethereum blockchain for liquidity positions
  - Raw data endpoints for admin pages showing completely unmodified API responses

- **Rate Limiting**:
  - Global rate limiting for all API requests
  - Exchange-specific rate limiting for regular endpoints
  - More restrictive rate limiting for admin raw data endpoints

- **Caching**:
  - Server-side caching for both regular and raw data responses
  - Configurable TTL (Time To Live) for different types of data

- **User Authentication**:
  - Firebase Authentication integration
  - Secure storage of user credentials

## Documentation

Strategiz Core has comprehensive documentation organized into several focused documents:

### [Architecture Guidelines](docs/ARCHITECTURE.md)

Our architectural principles, design patterns implementation, and SOLID principles:
- Layered architecture diagram and dependencies
- Technology stack used in each layer
- Design patterns implementation (Factory, Builder, Adapter, Facade, etc.)
- SOLID principles implementation
- Naming conventions and class responsibilities

### [Developer Guide](docs/DEVELOPER_GUIDE.md)

Information on coding standards, contribution workflows, and development best practices:
- Code of conduct
- Coding standards for Java
- REST API design principles
- Pull request process
- Testing standards
- Release process

### [API Endpoints Reference](docs/API_ENDPOINTS.md)

Complete documentation of all available API endpoints:
- Authentication endpoints
- Exchange-specific endpoints
- Portfolio and strategy endpoints
- Response formats and rate limiting

### [Exchange Integrations](docs/EXCHANGE_INTEGRATIONS.md)

Detailed information about cryptocurrency exchange integrations:
- Integration principles
- Exchange-specific notes and data handling
- Raw data transparency
- Guidelines for adding new exchanges

### [Security Features](docs/SECURITY.md)

Comprehensive overview of security measures:
- Authentication and API key management
- Request and response protection
- Data security and encryption
- Monitoring and auditing

### [Deployment Guide](docs/DEPLOYMENT.md)

Instructions for deploying in different environments:
- Local development setup
- Production deployment options
- Environment configuration
- Troubleshooting common issues

### Module Structure

The project follows a modular architecture with clear separation of concerns:

- **API Modules (`api-*`)**: 
  - Contain only REST controllers and API-specific code
  - Handle HTTP requests, authentication, and input validation
  - Depend only on their corresponding service modules
  - Located in the `api/` directory

- **Service Modules (`service-*`)**:
  - Contain business logic and service implementations
  - Implement the core functionality of the application
  - May depend on data modules and client modules
  - Located in the `service/` directory

- **Data Modules (`data-*`)**:
  - Handle data persistence and database interactions
  - Contain repositories, entities, and data access objects
  - Located in the `data/` directory

- **Client Modules (`client-*`)**:
  - Provide integration with external services and APIs
  - Implement HTTP clients and API wrappers
  - Located in the `client/` directory

### Build and Deployment Scripts

The project includes dedicated scripts for building and deploying the application in both Windows and Linux/macOS environments:

- **Build Scripts**: Compile and package all modules in the correct dependency order
- **Deploy Scripts**: Run the compiled application with appropriate configuration
- **Combined Scripts**: Build and deploy in a single step

See the [Scripts README](scripts/README.md) for detailed usage instructions.

### Dependency Flow

The dependency flow follows a strict hierarchy to maintain clean architecture:

```
API Modules → Service Modules → Data Modules → Client Modules
```

This ensures that:
1. Higher-level modules are not affected by changes in lower-level modules
2. Business logic is isolated from presentation and data access concerns
3. Each module has a single responsibility

## Getting Started

### Prerequisites

- Node.js (v16 or higher)
- npm (v6 or higher)
- Firebase account and project
- Firebase CLI (`npm install -g firebase-tools`)

### Installation

1. Clone the repository:
   ```
   git clone https://github.com/cuztomizer/strategiz-core.git
   cd strategiz-core
   ```

2. Install dependencies:
   ```
   npm install
   ```

3. Create a `.env` file in the root directory based on the `.env.example` file
   ```
   cp .env.example .env
   ```

## Deployment

### Local Development

To run the server locally:

```
npm run dev
```

The server will start on port 3001 (or the port specified in your `.env` file).

### Firebase Deployment

This API is designed to be deployed as a Firebase Cloud Function. Follow these steps to deploy:

1. Login to Firebase (if not already logged in):
   ```
   firebase login
   ```

2. Initialize your Firebase project (if not already initialized):
   ```
   firebase init
   ```
   - Select "Functions" when prompted
   - Select your Firebase project
   - Choose "Use an existing project" and select your project
   - Select JavaScript when asked about language
   - Choose "No" when asked about ESLint
   - Choose "Yes" to install dependencies

3. Deploy to Firebase:
   ```
   npm run deploy
   ```
   
   Or use the provided deployment script:
   ```
   ./deploy.bat
   ```

4. After deployment, your API will be available at:
   ```
   https://us-central1-[YOUR-PROJECT-ID].cloudfunctions.net/api
   ```

5. Update the client-side application to use this URL by setting the `REACT_APP_API_URL` environment variable.

### Viewing Logs

To view the logs for your deployed functions:

```
npm run logs
```

Or:

```
firebase functions:log
```

## Key Features Quick Reference

### API Integration Points

Strategiz Core integrates with multiple cryptocurrency exchanges and services:

- **Binance US** - Balance retrieval and raw data access
- **Kraken** - Balance retrieval and raw data access
- **Coinbase** - Balance retrieval and raw data access
- **Uniswap** - Portfolio retrieval and blockchain data access

For complete API documentation, see [API Endpoints Reference](docs/API_ENDPOINTS.md).

### Security Overview

The application implements comprehensive security measures including:

- **Real credential handling** (never using mock or test data)
- **End-to-end encryption** for API keys and sensitive data
- **Server-side API requests** (no client-side exposure of credentials)

For detailed security information, see [Security Features](docs/SECURITY.md).

## Core Principles

### Real Data Only

Strategiz Core strictly adheres to using only real API data and real user credentials. This project:

- **Never** uses mock responses, test data, or dummy data
- **Always** connects to actual exchange APIs using real credentials
- **Shows** completely unmodified API responses through admin endpoints
- **Maintains** transparency about what's actually coming from the APIs

### Transparency in Data Processing

We maintain transparency in how we process exchange data:

- Special handling for Kraken asset names (preserving .F suffixes for futures contracts)
- Minimal transformation of raw API data
- Admin pages showing exactly what we receive from exchanges

For detailed information about exchange integrations, see [Exchange Integrations](docs/EXCHANGE_INTEGRATIONS.md).

## Troubleshooting

### Server Won't Start
- Check if port 3001 is already in use
- Verify that your `serviceAccountKey.json` file is valid
- Make sure all dependencies are installed

### Authentication Errors
- Verify that your API credentials are correct
- Check if your credentials have the necessary permissions
- Ensure the system clock is synchronized (for HMAC timestamp validation)

### Rate Limiting Issues
- The server implements rate limiting to prevent API abuse
- If you're getting 429 (Too Many Requests) errors, wait and try again later
- Different endpoints have different rate limits

## Contributing

Please follow the established code style and add appropriate tests for new features.