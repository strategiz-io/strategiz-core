#!/bin/bash

#=============================================================================
# Strategiz Core - Gradle Quick Build and Deploy Script
# Fast incremental Gradle build for development iteration
#=============================================================================

set -e  # Exit on any error

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
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

print_gradle() {
    echo -e "${CYAN}🐘 $1${NC}"
}

# Change to project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
cd "$PROJECT_ROOT"

print_step "Gradle Quick Build and Deploy (Development Mode)"
echo "Using Gradle incremental compilation and build cache for speed..."

# Set Gradle options for faster builds
export GRADLE_OPTS="-Xmx1g -XX:+UseG1GC"

# Determine Gradle command
if [ -f "./gradlew" ]; then
    GRADLE_CMD="./gradlew"
    chmod +x ./gradlew
    print_gradle "Using Gradle wrapper"
else
    GRADLE_CMD="gradle"
    print_gradle "Using system Gradle"
fi

# Quick incremental build (no clean, use build cache, parallel)
print_step "Gradle Incremental Build"
print_gradle "Executing: $GRADLE_CMD assemble -x test --parallel --build-cache"

$GRADLE_CMD assemble \
    -x test \
    --parallel \
    --build-cache \
    --configure-on-demand \
    --quiet

if [ $? -eq 0 ]; then
    print_success "Gradle quick build completed!"
    
    # Show cache hit info if available
    CACHE_INFO=$($GRADLE_CMD assemble --dry-run 2>/dev/null | grep -i cache || true)
    if [ ! -z "$CACHE_INFO" ]; then
        print_gradle "Build cache active"
    fi
else
    print_warning "Quick build failed. Try running gradle-clean-build-deploy.sh instead"
    exit 1
fi

# Check if JAR exists
JAR_FILE="$PROJECT_ROOT/application/build/libs/application-1.0-SNAPSHOT.jar"
if [ ! -f "$JAR_FILE" ]; then
    # Try alternative name
    ALT_JAR="$PROJECT_ROOT/application/build/libs/application.jar"
    if [ -f "$ALT_JAR" ]; then
        JAR_FILE="$ALT_JAR"
    else
        print_warning "Application JAR not found. Running application build..."
        $GRADLE_CMD :application:build -x test --parallel
        
        # Recheck
        if [ -f "$PROJECT_ROOT/application/build/libs/application-1.0-SNAPSHOT.jar" ]; then
            JAR_FILE="$PROJECT_ROOT/application/build/libs/application-1.0-SNAPSHOT.jar"
        elif [ -f "$PROJECT_ROOT/application/build/libs/application.jar" ]; then
            JAR_FILE="$PROJECT_ROOT/application/build/libs/application.jar"
        else
            print_warning "Could not locate application JAR after build"
            exit 1
        fi
    fi
fi

print_step "Starting Application (Gradle Dev Mode)"
print_gradle "Spring Boot with development profile"
print_warning "Press Ctrl+C to stop"
echo

cd "$(dirname "$JAR_FILE")"
java -Xmx512m \
     -Dspring.profiles.active=dev \
     -Dspring.devtools.restart.enabled=true \
     -Dgradle.build.tool=gradle \
     -jar "$(basename "$JAR_FILE")"