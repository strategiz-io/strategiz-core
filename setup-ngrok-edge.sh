#!/bin/bash

echo "Setting up ngrok with a more memorable configuration..."
echo ""
echo "With ngrok free tier, you get a random subdomain each time."
echo "To get 'strategiz-dev.ngrok.app' you need:"
echo "1. Paid plan ($8/month) for custom subdomains"
echo "2. Or use your own domain with ngrok"
echo ""
echo "Starting ngrok with standard configuration..."

ngrok http https://localhost:8443 \
  --log-level=info \
  --region=us \
  --scheme=https
