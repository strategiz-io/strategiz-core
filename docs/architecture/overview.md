---
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
- **Device Service**: Device management and fingerprinting
- **Exchange Service**: Market data aggregation and processing

## Technology Stack

### Backend
- **Java with Spring Boot**: Core application framework
- **Firebase Firestore**: Primary NoSQL document database for flexible data models
- **Apache Kafka**: Event streaming and message queuing
- **Docker**: Containerization

### Frontend
- **React**: User interface library
- **TypeScript**: Type-safe JavaScript
- **Material-UI**: Component library
- **Redux**: State management

### Infrastructure
- **Firebase**: Authentication, Firestore database, and hosting
- **Docker**: Container orchestration
- **Kubernetes**: Production deployment
- **Google Cloud Platform**: Cloud infrastructure
- **GitHub Actions**: CI/CD pipeline

## Provider Integrations

The platform integrates with multiple financial data and trading providers:

- **Coinbase**: Cryptocurrency trading and wallet management
- **Binance US**: Cryptocurrency trading platform
- **Alpha Vantage**: Stock market data and analytics
- **CoinGecko**: Cryptocurrency market data
- **Kraken**: Cryptocurrency trading platform

Each provider integration is handled through dedicated client modules with proper API key management and rate limiting. 