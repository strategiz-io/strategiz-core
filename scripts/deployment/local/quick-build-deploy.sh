#!/bin/bash

#=============================================================================
# Strategiz Core - Quick Build and Deploy Script
# Fast incremental build for development iteration
#=============================================================================

set -e  # Exit on any error

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

print_step() {
    echo -e "${BLUE}🚀 $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

# Change to project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
cd "$PROJECT_ROOT"

print_step "Quick Build and Deploy (Development Mode)"
echo "Using incremental compilation for faster builds..."

# Set Maven options for faster builds
export MAVEN_OPTS="-Xmx1g"

# Quick compile (no clean, no tests, parallel)
print_step "Incremental Build"
mvn compile install -DskipTests -T 1C -q --batch-mode \
    -Dmaven.test.skip=true \
    -Dmaven.javadoc.skip=true \
    -Dcheckstyle.skip=true

if [ $? -eq 0 ]; then
    print_success "Quick build completed!"
else
    echo "❌ Quick build failed. Try running clean-build-deploy.sh instead"
    exit 1
fi

# Check if JAR exists
JAR_FILE="$PROJECT_ROOT/application/target/application-1.0-SNAPSHOT.jar"
if [ ! -f "$JAR_FILE" ]; then
    print_warning "Application JAR not found. Running full build..."
    mvn clean install -DskipTests -pl application -am -q
fi

print_step "Starting Application (Dev Mode)"
print_warning "Press Ctrl+C to stop"
echo

cd "$PROJECT_ROOT/application/target"
java -Xmx512m \
     -Dspring.profiles.active=dev \
     -Dspring.devtools.restart.enabled=true \
     -jar application-1.0-SNAPSHOT.jar