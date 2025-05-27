# Strategiz Dashboard API

This module provides REST API endpoints for the Strategiz platform's dashboard functionality, offering a comprehensive interface for clients to access portfolio analytics, market data, and user-specific investment insights.

![Strategiz Dashboard](https://via.placeholder.com/800x400?text=Strategiz+Dashboard+API)

## Overview

The Dashboard API serves as the central interface for the Strategiz platform's frontend, delivering critical financial data in a structured format. The API follows a clean, modular architecture with clear separation of concerns, making it maintainable and extensible.

## API Endpoints

| Endpoint | Description | Response Model |
|----------|-------------|----------------|
| `GET /dashboard` | Comprehensive dashboard with all components | `DashboardResponse` |
| `GET /dashboard/portfolio-summary` | Portfolio value, allocation, and exchange information | `PortfolioSummaryResponse` |
| `GET /dashboard/asset-allocation` | Breakdown of assets by type and specific holdings | `AssetAllocationResponse` |
| `GET /dashboard/performance-metrics` | Historical performance and key metrics | `PerformanceMetricsResponse` |
| `GET /dashboard/risk-analysis` | Volatility, diversification, and risk metrics | `RiskAnalysisResponse` |
| `GET /dashboard/market-sentiment` | Market trends and sentiment indicators | `MarketSentimentResponse` |
| `GET /dashboard/watchlist` | User's watched assets and related data | `WatchlistResponse` |

## Model Structure

Each endpoint has its own dedicated model package for better organization:

```
io.strategiz.api.dashboard.model/
├── assetallocation/
│   └── AssetAllocationResponse.java
├── dashboard/
│   └── DashboardResponse.java
├── marketsentiment/
│   └── MarketSentimentResponse.java
├── performancemetrics/
│   └── PerformanceMetricsResponse.java
├── portfoliosummary/
│   └── PortfolioSummaryResponse.java
├── riskanalysis/
│   └── RiskAnalysisResponse.java
└── watchlist/
    └── WatchlistResponse.java
```

This organization aligns with our modular architecture approach, where each component has a single responsibility.

## Authentication

All Dashboard API endpoints require authentication. The API uses JSON Web Tokens (JWT) for securing requests. Include the token in the Authorization header:

```
Authorization: Bearer <your_jwt_token>
```

## Error Handling

The API uses standard HTTP status codes and returns detailed error messages in the following format:

```json
{
  "timestamp": "2025-05-26T12:34:56.789Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid asset type provided",
  "path": "/dashboard/asset-allocation"
}
```

## Account Modes

The Dashboard API respects the user's account mode setting:

- **PAPER Mode**: Returns simulated data for testing and learning
- **LIVE Mode**: Returns real data from connected providers

## Sample Response

Here's a sample response from the portfolio summary endpoint:

```json
{
  "totalValue": 125350.75,
  "dailyChange": {
    "amount": 1250.25,
    "percentage": 1.01
  },
  "exchanges": [
    {
      "name": "Kraken",
      "value": 75350.50,
      "assets": 12
    },
    {
      "name": "Binance US",
      "value": 50000.25,
      "assets": 8
    }
  ]
}
```

## Dependencies

The Dashboard API module depends on:
- Spring Boot Web
- Synapse Framework API
- Strategiz Service Dashboard
- Strategiz Business Portfolio
- Lombok
- Jackson

## Getting Started for Developers

1. Ensure all required dependencies are available in your Maven repository
2. Configure your `application.properties` with the necessary settings
3. Run the application with `mvn spring-boot:run`
4. Access the API at `http://localhost:8080/dashboard`

## See Also

- [Service Dashboard Module](../service/service-dashboard)
- [Business Portfolio Module](../business/business-portfolio)
- [Data User Module](../data/data-user)
