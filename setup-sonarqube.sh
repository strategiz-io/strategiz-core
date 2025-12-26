#!/bin/bash

# ===================================================================
# SonarQube Setup Script for Strategiz
# ===================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SONAR_URL="http://localhost:9000"
DEFAULT_USER="admin"
DEFAULT_PASS="admin"

echo "======================================================================"
echo "  SonarQube Setup for Strategiz"
echo "======================================================================"
echo ""

# Step 1: Start SonarQube
echo "📦 Step 1: Starting SonarQube containers..."
docker-compose -f docker-compose.sonarqube.yml up -d

echo "⏳ Waiting for SonarQube to start (this may take 60-90 seconds)..."
sleep 10

# Wait for SonarQube to be ready
MAX_ATTEMPTS=30
ATTEMPT=0
while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
  if curl -s "$SONAR_URL/api/system/status" | grep -q "UP"; then
    echo "✅ SonarQube is ready!"
    break
  fi
  ATTEMPT=$((ATTEMPT + 1))
  echo "   Waiting... (attempt $ATTEMPT/$MAX_ATTEMPTS)"
  sleep 3
done

if [ $ATTEMPT -eq $MAX_ATTEMPTS ]; then
  echo "❌ SonarQube failed to start. Check logs with: docker logs strategiz-sonarqube"
  exit 1
fi

echo ""
echo "======================================================================"
echo "  NEXT STEPS:"
echo "======================================================================"
echo ""
echo "1. 🌐 Open SonarQube in your browser:"
echo "   http://localhost:9000"
echo ""
echo "2. 🔐 Login with default credentials:"
echo "   Username: admin"
echo "   Password: admin"
echo "   (You'll be prompted to change the password)"
echo ""
echo "3. 🔑 Generate an authentication token:"
echo "   - Go to: My Account > Security"
echo "   - Name: strategiz-core"
echo "   - Type: Project Analysis Token"
echo "   - Click 'Generate'"
echo "   - COPY THE TOKEN (you won't see it again!)"
echo ""
echo "4. 🗝️  Store the token in Vault:"
echo "   export VAULT_ADDR=http://127.0.0.1:8200"
echo "   export VAULT_TOKEN=<your-vault-token>"
echo ""
echo "   vault kv put secret/strategiz/sonarqube \\"
echo "     url=\"http://localhost:9000\" \\"
echo "     token=\"<your-sonarqube-token>\" \\"
echo "     project-key=\"strategiz-core\""
echo ""
echo "5. 🔍 Run your first scan:"
echo "   cd $SCRIPT_DIR"
echo "   mvn clean verify sonar:sonar \\"
echo "     -Dsonar.host.url=http://localhost:9000 \\"
echo "     -Dsonar.token=<your-token>"
echo ""
echo "6. 🔄 Restart your Spring Boot app to load Vault config:"
echo "   (The app will now pull SonarQube metrics!)"
echo ""
echo "======================================================================"
echo ""
echo "💡 TIP: For production, use a dedicated SonarQube instance and"
echo "   secure the token properly in your production Vault."
echo ""
echo "📝 Configuration file: sonar-project.properties"
echo "🐳 Docker Compose file: docker-compose.sonarqube.yml"
echo ""
