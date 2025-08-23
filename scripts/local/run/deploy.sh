#!/bin/bash
echo "===================================================================="
echo "Deploying Strategiz Core application..."
echo "===================================================================="
echo

# Check if application JAR exists
if [ ! -f "../../../application/target/application-1.0-SNAPSHOT.jar" ]; then
    echo "Error: Application JAR not found."
    echo "Please run build.sh first or use build-and-deploy.sh"
    exit 1
fi

# Ensure Vault environment is set up
if [ -z "$VAULT_TOKEN" ]; then
    echo "Setting up Vault environment for development..."
    export VAULT_ADDR="http://localhost:8200"
    export VAULT_TOKEN="root"
    echo "✅ VAULT_ADDR=$VAULT_ADDR"
    echo "✅ VAULT_TOKEN=root (dev mode)"
fi

# Check if Vault is running
if ! pgrep -f "vault server" > /dev/null; then
    echo "⚠️  Vault server not running. Starting in dev mode..."
    vault server -dev > /tmp/vault-dev.log 2>&1 &
    sleep 2
    echo "✅ Vault server started"
fi

echo "Starting Strategiz Core application..."
echo "Press Ctrl+C to stop the application when finished."
echo

cd ../../../application/target
java -jar application-1.0-SNAPSHOT.jar --spring.profiles.active=dev

echo
echo "Strategiz Core application stopped."

# Return to scripts directory
# cd ../../../scripts # Removed to fix path error
exit 0
