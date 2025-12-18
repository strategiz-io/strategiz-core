#!/bin/bash

# Start Vault with persistent file storage
# Data is stored in ./vault/data directory

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR/.."

# Create data directory if it doesn't exist
mkdir -p vault/data

echo "Starting Vault with persistent storage..."
echo "Data will be stored in: $SCRIPT_DIR/data"
echo ""

vault server -config=vault/config.hcl
