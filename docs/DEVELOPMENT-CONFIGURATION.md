# Development Configuration Guide

## Quick Start

### HTTP Development (Default - Port 8080)
Use this for standard development without SSL requirements:

```bash
# Backend
./scripts/local/run/start-backend-http.sh

# Frontend (in strategiz-ui directory)
./scripts/start-with-http-backend.sh
```

- Backend: http://localhost:8080
- Frontend: http://localhost:3000
- Suitable for: General development, Coinbase OAuth

### HTTPS Development (Port 8443)
Use this when SSL is required (e.g., Charles Schwab OAuth):

```bash
# Backend
./scripts/local/run/start-backend-https.sh

# Frontend (in strategiz-ui directory)
./scripts/start-with-https-backend.sh
```

- Backend: https://localhost:8443 (self-signed certificate)
- Frontend: http://localhost:3000
- Required for: Charles Schwab OAuth, other SSL-required providers

## Environment Configuration

### Backend Profiles

- `dev-http`: HTTP on port 8080 (default)
- `dev-https`: HTTPS on port 8443 with self-signed certificate

### Frontend Configuration

Configure via `.env.local` file:

```env
# For HTTP backend
REACT_APP_API_PROTOCOL=http
REACT_APP_API_HOST=localhost
REACT_APP_API_PORT=8080

# For HTTPS backend
REACT_APP_API_PROTOCOL=https
REACT_APP_API_HOST=localhost
REACT_APP_API_PORT=8443
REACT_APP_ACCEPT_SELF_SIGNED_CERTS=true
```

## OAuth Provider Requirements

| Provider | HTTP Support | HTTPS Required | Notes |
|----------|-------------|----------------|-------|
| Coinbase | ✅ | Optional | Works with both HTTP and HTTPS |
| Charles Schwab | ❌ | ✅ Required | Must use HTTPS with port 8443 |
| Alpaca | ✅ | Optional | Works with both |
| Kraken | ✅ | Optional | Works with both |

## Switching Between Modes

1. Stop the current backend (Ctrl+C)
2. Choose the appropriate startup script
3. Restart the frontend with matching configuration

## Troubleshooting

### Certificate Warnings
When using HTTPS mode, you'll see certificate warnings. This is normal for self-signed certificates:
1. Access https://localhost:8443 directly in browser
2. Accept the certificate warning
3. The frontend proxy will then work

### Port Already in Use
If ports are already in use:
```bash
# Check what's using port 8080
lsof -i :8080

# Check what's using port 8443
lsof -i :8443
```

### Environment Variables Not Loading
Ensure `.env.local` exists in strategiz-ui directory. Copy from `.env.example` if needed.