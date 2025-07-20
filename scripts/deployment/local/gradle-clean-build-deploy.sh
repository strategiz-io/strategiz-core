#!/bin/bash

#=============================================================================
# Strategiz Core - Gradle Clean Build and Deploy Script
# Modern, efficient Gradle-based build and deploy script
#=============================================================================

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
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

print_gradle() {
    echo -e "${CYAN}🐘 Gradle: $1${NC}"
}

# Change to project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
cd "$PROJECT_ROOT"

print_step "Strategiz Core - Gradle Build System"
echo "Project root: $PROJECT_ROOT"
echo "Build system: Gradle"
echo "Timestamp: $(date)"
echo

# Pre-build checks
print_step "Pre-build Environment Checks"

# Check if Gradle is available (wrapper or system)
if [ -f "./gradlew" ]; then
    GRADLE_CMD="./gradlew"
    print_success "Gradle wrapper found (recommended)"
    
    # Make gradlew executable if needed
    chmod +x ./gradlew
    
    # Get Gradle version from wrapper
    GRADLE_VERSION=$($GRADLE_CMD --version | grep "Gradle" | head -n1)
    print_gradle "$GRADLE_VERSION"
elif command -v gradle &> /dev/null; then
    GRADLE_CMD="gradle"
    GRADLE_VERSION=$(gradle --version | grep "Gradle" | head -n1)
    print_warning "Using system Gradle (wrapper recommended): $GRADLE_VERSION"
else
    print_error "Neither Gradle wrapper (./gradlew) nor system Gradle found"
    echo
    echo "To fix this:"
    echo "1. Use Gradle wrapper: ./gradlew wrapper --gradle-version 8.5"
    echo "2. Or install Gradle: https://gradle.org/install/"
    exit 1
fi

# Check if Java is available
if ! command -v java &> /dev/null; then
    print_error "Java is not installed or not in PATH"
    exit 1
fi
JAVA_VERSION=$(java -version 2>&1 | head -n1)
print_success "Java found: $JAVA_VERSION"

# Check Gradle-Java compatibility
if $GRADLE_CMD --version | grep -q "JVM:.*21\|JVM:.*17"; then
    print_success "Java version is compatible with Gradle"
else
    print_warning "Java version compatibility should be verified"
fi

# Check if Vault process is running (optional but recommended)
if pgrep -f "vault server" > /dev/null; then
    print_success "Vault server is running"
else
    print_warning "Vault server doesn't appear to be running. You may want to start it with: vault server -dev"
fi

echo

# Gradle-specific setup
print_step "Gradle Configuration"

# Set Gradle options for better performance
export GRADLE_OPTS="-Xmx2g -XX:MaxMetaspaceSize=512m -XX:+UseG1GC"
print_gradle "Memory settings: $GRADLE_OPTS"

# Check Gradle properties
if [ -f "./gradle.properties" ]; then
    print_gradle "Found gradle.properties"
    
    # Show key Gradle properties
    if grep -q "org.gradle.parallel" gradle.properties; then
        PARALLEL_SETTING=$(grep "org.gradle.parallel" gradle.properties)
        print_gradle "Parallel builds: $PARALLEL_SETTING"
    fi
    
    if grep -q "org.gradle.caching" gradle.properties; then
        CACHE_SETTING=$(grep "org.gradle.caching" gradle.properties)
        print_gradle "Build cache: $CACHE_SETTING"
    fi
else
    print_warning "No gradle.properties found - consider adding for performance optimization"
fi

# Validate Gradle build files
print_gradle "Validating build configuration..."
$GRADLE_CMD help --quiet > /dev/null 2>&1
if [ $? -eq 0 ]; then
    print_success "Gradle configuration is valid"
else
    print_error "Gradle build configuration has issues"
    exit 1
fi

# Show available Gradle tasks (top-level)
print_gradle "Key available tasks:"
$GRADLE_CMD tasks --group="build" --quiet | grep -E "(build|clean|assemble)" | head -5 || true

echo

# Clean build using Gradle
print_step "Phase 1: Gradle Clean Build"
echo "Building with Gradle for dependency management and parallel execution..."

# Create build log
BUILD_LOG="$PROJECT_ROOT/gradle-build.log"
echo "Build log: $BUILD_LOG"

print_gradle "Executing: $GRADLE_CMD clean build -x test --parallel --build-cache"

# Perform clean build with Gradle optimizations
$GRADLE_CMD clean build \
    -x test \
    --parallel \
    --build-cache \
    --configure-on-demand \
    --info 2>&1 | tee "$BUILD_LOG"

BUILD_EXIT_CODE=${PIPESTATUS[0]}

if [ $BUILD_EXIT_CODE -eq 0 ]; then
    print_success "Gradle build completed successfully!"
    
    # Show build summary
    if grep -q "BUILD SUCCESSFUL" "$BUILD_LOG"; then
        BUILD_TIME=$(grep "BUILD SUCCESSFUL in" "$BUILD_LOG" | tail -1)
        print_gradle "$BUILD_TIME"
    fi
    
    # Show cache performance if available
    if grep -q "cache hit" "$BUILD_LOG"; then
        CACHE_HITS=$(grep -c "cache hit" "$BUILD_LOG")
        print_gradle "Build cache hits: $CACHE_HITS"
    fi
else
    print_error "Gradle build failed. Check $BUILD_LOG for details."
    echo
    print_gradle "Common Gradle build issues:"
    echo "  • Task dependencies not properly defined"
    echo "  • Plugin version conflicts"
    echo "  • Gradle version compatibility"
    echo "  • Memory issues (increase GRADLE_OPTS)"
    echo "  • Network issues (dependencies download)"
    exit 1
fi

echo

# Gradle-specific verification
print_step "Phase 2: Gradle Build Verification"

# Check main application JAR
JAR_FILE="$PROJECT_ROOT/application/build/libs/application-1.0-SNAPSHOT.jar"
if [ -f "$JAR_FILE" ]; then
    JAR_SIZE=$(du -h "$JAR_FILE" | cut -f1)
    print_success "Application JAR created: $JAR_SIZE"
    
    # Verify JAR structure (Gradle Spring Boot plugin)
    if jar -tf "$JAR_FILE" | grep -q "BOOT-INF\|org/springframework/boot"; then
        print_success "Spring Boot JAR structure verified"
    else
        print_warning "JAR may not be a proper Spring Boot executable JAR"
    fi
else
    print_error "Application JAR not found at: $JAR_FILE"
    
    # Check alternative locations
    ALT_JAR="$PROJECT_ROOT/application/build/libs/application.jar"
    if [ -f "$ALT_JAR" ]; then
        JAR_FILE="$ALT_JAR"
        print_warning "Found JAR at alternative location: $ALT_JAR"
    else
        exit 1
    fi
fi

# Check Gradle build outputs
print_gradle "Build outputs directory: $PROJECT_ROOT/application/build"

# Verify framework modules (if using Gradle multi-project)
if [ -d "framework" ]; then
    FRAMEWORK_MODULES=("framework-secrets" "framework-exception" "framework-logging")
    for module in "${FRAMEWORK_MODULES[@]}"; do
        MODULE_JAR="framework/$module/build/libs/$module-1.0-SNAPSHOT.jar"
        if [ -f "$MODULE_JAR" ]; then
            print_success "Framework module: $module"
        else
            print_warning "Framework module JAR not found: $module (may not be configured for Gradle)"
        fi
    done
fi

# Show Gradle build scan URL if available
if grep -q "build scan" "$BUILD_LOG"; then
    BUILD_SCAN_URL=$(grep "https://gradle.com/s/" "$BUILD_LOG" | tail -1 || true)
    if [ ! -z "$BUILD_SCAN_URL" ]; then
        print_gradle "Build scan: $BUILD_SCAN_URL"
    fi
fi

# Check dependencies
print_gradle "Checking dependency insights..."
$GRADLE_CMD dependencyInsight --dependency org.springframework.boot --quiet || true

echo

# Optional Gradle-specific analysis
read -p "Do you want to run Gradle build analysis? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    print_step "Gradle Build Analysis"
    
    # Dependencies report
    print_gradle "Generating dependencies report..."
    $GRADLE_CMD dependencies --configuration compileClasspath > gradle-dependencies.txt 2>/dev/null || true
    print_success "Dependencies saved to gradle-dependencies.txt"
    
    # Project report
    print_gradle "Generating project structure report..."
    $GRADLE_CMD projects > gradle-projects.txt 2>/dev/null || true
    print_success "Project structure saved to gradle-projects.txt"
    
    echo
fi

# Deploy the application
print_step "Phase 3: Application Deployment"

# Gradle-specific environment setup
if [ -z "$VAULT_TOKEN" ]; then
    print_warning "VAULT_TOKEN not set. Make sure to export it if using Vault"
    echo "   Example: export VAULT_TOKEN=hvs.your-dev-token"
fi

# Show Gradle version info
GRADLE_FULL_VERSION=$($GRADLE_CMD --version | head -5)
print_gradle "Runtime info:"
echo "$GRADLE_FULL_VERSION" | sed 's/^/  /'

echo
echo "Starting Strategiz Core application..."
echo "Built with Gradle build system"
echo
echo "🌐 Backend: http://localhost:8080"
echo "📖 API Docs: http://localhost:8080/swagger-ui.html"
echo "💡 Health: http://localhost:8080/actuator/health"
echo "🐘 Build Tool: Gradle"
echo
print_warning "Press Ctrl+C to stop the application"
echo

# Navigate to Gradle build output directory
cd "$(dirname "$JAR_FILE")"

# Run with optimized settings for Gradle build
java -Xmx1g -XX:+UseG1GC \
     -Dspring.profiles.active=dev \
     -Dlogging.level.org.springframework.web=INFO \
     -Dgradle.build.tool=gradle \
     -jar "$(basename "$JAR_FILE")"

# Cleanup message
echo
print_step "Application Stopped"
print_success "Strategiz Core (Gradle build) has been stopped cleanly"
echo "Gradle artifacts available in build/ directories"
echo "Build log: $BUILD_LOG"
echo
if [ -f "$BUILD_LOG" ] && grep -q "https://gradle.com/s/" "$BUILD_LOG"; then
    BUILD_SCAN_URL=$(grep "https://gradle.com/s/" "$BUILD_LOG" | tail -1)
    print_gradle "Build scan: $BUILD_SCAN_URL"
fi
echo
print_gradle "Quick restart: java -jar $JAR_FILE --spring.profiles.active=dev"