# API Naming Guide

## Overview
This guide establishes the naming conventions for APIs in the Strategiz platform. Each controller should be documented with a business-friendly API name that clearly describes its purpose.

## API Naming Conventions

### Structure
```
[Domain] [Action] API
```

Examples:
- Provider Connection API
- User Authentication API
- Portfolio Sync API
- Strategy Execution API

### Rules

1. **Use Business Terms**: Name APIs based on business functionality, not technical implementation
   - ✅ Good: "Provider Connection API"
   - ❌ Bad: "CreateProviderController API"

2. **Action-Oriented**: Include the primary action in the name
   - Connection, Query, Update, Sync, Authentication, etc.

3. **Domain-Specific**: Start with the business domain
   - Provider, Portfolio, User, Strategy, Market, etc.

4. **Consistent Slug Format**: Use kebab-case for file names and URLs
   - API Name: "Provider Connection API"
   - File Name: `provider-connection-api.mdx`
   - API Slug: `provider-connection`

## Standard API Names by Domain

### Provider Domain
| Controller | API Name | API Slug | Purpose |
|------------|----------|----------|---------|
| CreateProviderController | Provider Connection API | provider-connection | Connect external providers |
| ReadProviderController | Provider Query API | provider-query | Query provider information |
| UpdateProviderController | Provider Update API | provider-update | Update provider settings |
| DeleteProviderController | Provider Disconnection API | provider-disconnection | Disconnect providers |
| ProviderCallbackController | Provider OAuth Callback API | provider-oauth-callback | Handle OAuth callbacks |
| SyncProviderController | Provider Sync API | provider-sync | Sync provider data |

### Portfolio Domain
| Controller | API Name | API Slug | Purpose |
|------------|----------|----------|---------|
| GetPortfolioController | Portfolio Query API | portfolio-query | Get portfolio data |
| UpdatePortfolioController | Portfolio Update API | portfolio-update | Update portfolio |
| SyncPortfolioController | Portfolio Sync API | portfolio-sync | Sync portfolio data |
| AnalyzePortfolioController | Portfolio Analysis API | portfolio-analysis | Analyze portfolio performance |

### User Domain
| Controller | API Name | API Slug | Purpose |
|------------|----------|----------|---------|
| AuthenticationController | User Authentication API | user-authentication | User login/logout |
| RegistrationController | User Registration API | user-registration | New user signup |
| ProfileController | User Profile API | user-profile | Manage user profile |
| PreferencesController | User Preferences API | user-preferences | User settings |

### Strategy Domain
| Controller | API Name | API Slug | Purpose |
|------------|----------|----------|---------|
| CreateStrategyController | Strategy Creation API | strategy-creation | Create trading strategies |
| ExecuteStrategyController | Strategy Execution API | strategy-execution | Execute strategies |
| BacktestController | Strategy Backtest API | strategy-backtest | Backtest strategies |
| OptimizeController | Strategy Optimization API | strategy-optimization | Optimize parameters |

## File Structure

```
service-[module]/
├── docs/
│   ├── diagrams/
│   │   ├── [api-slug]-flow.drawio
│   │   ├── [api-slug]-flow.png
│   │   ├── [api-slug]-components.drawio
│   │   └── [api-slug]-components.png
│   ├── [api-slug].mdx
│   ├── api-naming-guide.md
│   ├── controller-documentation-template.mdx
│   └── README.md
```

## Documentation Metadata

Each API documentation file should include:

```yaml
---
title: [API Name]                    # Business-friendly name
apiName: [api-slug]                  # URL-friendly identifier
module: [module-name]                # Service module name
controller: [ControllerClassName]     # Technical controller class
version: 1.0.0                       # API version
lastUpdated: YYYY-MM-DD              # Last update date
author: [Team Name]                  # Responsible team
---
```

## URL Patterns

APIs should follow RESTful naming with the API slug:

```
/v1/[domain]/[action]
```

Examples:
- `/v1/providers/connect` - Provider Connection API
- `/v1/providers/query` - Provider Query API
- `/v1/portfolio/sync` - Portfolio Sync API
- `/v1/strategy/execute` - Strategy Execution API

## Benefits of This Naming Convention

1. **Business Alignment**: Non-technical stakeholders can understand API purposes
2. **Consistency**: Predictable naming across all modules
3. **Discoverability**: Easy to find related APIs by domain
4. **Documentation**: Clear relationship between API name and functionality
5. **Marketing**: API names can be used in external documentation and marketing materials

## Migration Guide

When renaming existing controller documentation:

1. Choose appropriate API name following conventions above
2. Rename MDX file to use api-slug
3. Update frontmatter with new title and apiName
4. Create/update diagram files with api-slug naming
5. Update README.md with new API name
6. Update any references in other documentation

## Examples

### Good API Names
- ✅ Provider Connection API
- ✅ Portfolio Sync API
- ✅ User Authentication API
- ✅ Strategy Execution API
- ✅ Market Data Stream API

### Poor API Names
- ❌ CreateProviderController
- ❌ Provider API
- ❌ Connect API
- ❌ API for Providers
- ❌ Strategiz Provider Connection Service Endpoint

## Questions?

For questions about API naming, contact:
- **Slack**: #api-design
- **Email**: api-team@strategiz.io