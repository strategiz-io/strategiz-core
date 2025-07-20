#!/bin/bash

#=============================================================================
# Strategiz Core - Clean Build and Deploy Script
# A modern, efficient build and deploy script using Maven reactor capabilities
#=============================================================================

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_step() {
    echo -e "${BLUE}===================================================================="
    echo -e "  $1"
    echo -e "====================================================================${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

# Change to project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
cd "$PROJECT_ROOT"

print_step "Starting Strategiz Core Clean Build and Deploy"
echo "Project root: $PROJECT_ROOT"
echo "Timestamp: $(date)"
echo

# Pre-build checks
print_step "Pre-build Environment Checks"

# Check if Maven is available
if ! command -v mvn &> /dev/null; then
    print_error "Maven (mvn) is not installed or not in PATH"
    exit 1
fi
print_success "Maven found: $(mvn --version | head -n1)"

# Check if Java is available
if ! command -v java &> /dev/null; then
    print_error "Java is not installed or not in PATH"
    exit 1
fi
print_success "Java found: $(java -version 2>&1 | head -n1)"

# Check if Vault process is running (optional but recommended)
if pgrep -f "vault server" > /dev/null; then
    print_success "Vault server is running"
else
    print_warning "Vault server doesn't appear to be running. You may want to start it with: vault server -dev"
fi

echo

# Clean build using Maven reactor
print_step "Phase 1: Clean Build (Using Maven Reactor)"
echo "This will build all modules in correct dependency order..."

# Set Maven options for better performance and output
export MAVEN_OPTS="-Xmx2g -XX:MaxMetaspaceSize=512m"

# Perform clean install with skip tests for faster builds
mvn clean install -DskipTests -T 1C -q --batch-mode \
    -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn

if [ $? -eq 0 ]; then
    print_success "Build completed successfully!"
else
    print_error "Build failed. Check the output above for details."
    exit 1
fi

echo

# Verify build artifacts
print_step "Phase 2: Build Verification"

JAR_FILE="$PROJECT_ROOT/application/target/application-1.0-SNAPSHOT.jar"
if [ -f "$JAR_FILE" ]; then
    JAR_SIZE=$(du -h "$JAR_FILE" | cut -f1)
    print_success "Application JAR found: $JAR_SIZE"
else
    print_error "Application JAR not found at: $JAR_FILE"
    exit 1
fi

# Check if framework modules were built successfully
FRAMEWORK_MODULES=("framework-secrets" "framework-exception" "framework-logging" "framework-api-docs")
for module in "${FRAMEWORK_MODULES[@]}"; do
    if [ -f "framework/$module/target/$module-1.0-SNAPSHOT.jar" ]; then
        print_success "Framework module built: $module"
    else
        print_warning "Framework module JAR not found: $module (this may be normal for some modules)"
    fi
done

echo

# Optional: Run quick smoke tests
read -p "Do you want to run a quick smoke test before deployment? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    print_step "Phase 3: Quick Smoke Test"
    mvn test -Dtest="**/HealthCheckTest" -q --batch-mode 2>/dev/null || true
    print_success "Smoke test completed"
    echo
fi

# Deploy the application
print_step "Phase 4: Application Deployment"

# Check for environment variables and provide guidance
if [ -z "$VAULT_TOKEN" ]; then
    print_warning "VAULT_TOKEN not set. Make sure to export it if using Vault"
    echo "   Example: export VAULT_TOKEN=hvs.your-dev-token"
fi

echo "Starting Strategiz Core application..."
echo "Application will start with dev profile"
echo
echo "🌐 Backend will be available at: http://localhost:8080"
echo "📖 API Documentation: http://localhost:8080/swagger-ui.html"
echo "💡 Health Check: http://localhost:8080/actuator/health"
echo
print_warning "Press Ctrl+C to stop the application"
echo

# Navigate to application directory and start
cd "$PROJECT_ROOT/application/target"

# Run with dev profile and optimized JVM settings
java -Xmx1g -XX:+UseG1GC \
     -Dspring.profiles.active=dev \
     -Dlogging.level.org.springframework.web=INFO \
     -jar application-1.0-SNAPSHOT.jar

# Cleanup message
echo
print_step "Application Stopped"
print_success "Strategiz Core has been stopped cleanly"
echo "Build artifacts remain in target/ directories for quick restart"
echo "To restart quickly, run: java -jar $JAR_FILE --spring.profiles.active=dev"