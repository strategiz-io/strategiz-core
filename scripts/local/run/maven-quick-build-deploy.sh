#!/bin/bash

#=============================================================================
# Strategiz Core - Maven Quick Build and Deploy Script
# Fast incremental Maven build for development iteration
#=============================================================================

set -e  # Exit on any error

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
PURPLE='\033[0;35m'
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

print_maven() {
    echo -e "${PURPLE}🔨 $1${NC}"
}

# Change to project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
cd "$PROJECT_ROOT"

print_step "Maven Quick Build and Deploy (Development Mode)"
echo "Using Maven incremental compilation for faster builds..."

# Set Maven options for faster builds
export MAVEN_OPTS="-Xmx1g -XX:+UseG1GC"

# Determine Maven command
if [ -f "./mvnw" ]; then
    MVN_CMD="./mvnw"
    print_maven "Using Maven wrapper"
else
    MVN_CMD="mvn"
    print_maven "Using system Maven"
fi

# Quick incremental compile (no clean, no tests, parallel)
print_step "Maven Incremental Build"
print_maven "Executing: $MVN_CMD compile install -DskipTests -T 1C"

$MVN_CMD compile install \
    -DskipTests \
    -T 1C \
    --batch-mode \
    -Dmaven.test.skip=true \
    -Dmaven.javadoc.skip=true \
    -Dcheckstyle.skip=true \
    -Dmaven.compile.fork=true

if [ $? -eq 0 ]; then
    print_success "Maven quick build completed!"
else
    print_warning "Quick build failed. Try running maven-clean-build-deploy.sh instead"
    exit 1
fi

# Check if JAR exists
JAR_FILE="$PROJECT_ROOT/application/target/application-1.0-SNAPSHOT.jar"
if [ ! -f "$JAR_FILE" ]; then
    print_warning "Application JAR not found. Running application module build..."
    $MVN_CMD clean install -DskipTests -pl application -am --batch-mode
fi

print_step "Starting Application (Maven Dev Mode)"
print_maven "Spring Boot with development profile"
print_warning "Press Ctrl+C to stop"
echo

cd "$PROJECT_ROOT/application/target"
java -Xmx512m \
     -Dspring.profiles.active=dev \
     -Dspring.devtools.restart.enabled=true \
     -Dmaven.build.tool=maven \
     -jar application-1.0-SNAPSHOT.jar