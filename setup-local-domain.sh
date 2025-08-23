#!/bin/bash

echo "Setting up local domain alias for development..."

# Add to /etc/hosts
echo "Adding strategiz-dev.local to /etc/hosts..."
echo "127.0.0.1 strategiz-dev.local" | sudo tee -a /etc/hosts

# Create SSL certificate for local domain
echo "Creating self-signed certificate for strategiz-dev.local..."
openssl req -x509 -newkey rsa:4096 -keyout strategiz-dev.key -out strategiz-dev.crt -days 365 -nodes \
  -subj "/C=US/ST=State/L=City/O=Organization/CN=strategiz-dev.local"

echo ""
echo "Local domain setup complete!"
echo "You can now use: https://strategiz-dev.local:8443"
echo ""
echo "Note: This only works locally, not for OAuth callbacks"
