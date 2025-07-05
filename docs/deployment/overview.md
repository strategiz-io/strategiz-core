---
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
- Firebase project with Firestore enabled

## Environment Variables

Required environment variables:
- `FIREBASE_DATABASE_URL` - Firebase Firestore database URL
- `JWT_SECRET` - JWT token secret
- `FIREBASE_PROJECT_ID` - Firebase project identifier
- `FIREBASE_PRIVATE_KEY` - Firebase service account private key
- `FIREBASE_CLIENT_EMAIL` - Firebase service account email
- `PROVIDER_API_KEYS` - Provider API credentials (Coinbase, Binance, etc.)

## Firebase Configuration

### Firestore Setup
1. Create a Firebase project at [Firebase Console](https://console.firebase.google.com)
2. Enable Firestore database in production mode
3. Create a service account and download the JSON key file
4. Set up Firestore security rules for your application

### Authentication Integration
Firebase Authentication can be used alongside the custom authentication system for OAuth providers and user management.

## Quick Start

1. Clone the repository
2. Configure environment variables
3. Set up Firebase project and Firestore
4. Run `docker-compose up`
5. Access the application at `http://localhost:3000`

## Provider Integration

The platform integrates with various financial data and trading providers:
- **Coinbase**: Trading and wallet integration
- **Binance US**: Trading platform integration
- **Alpha Vantage**: Market data provider
- **CoinGecko**: Cryptocurrency data
- **Kraken**: Trading platform integration

Each provider requires specific API keys and configuration. 