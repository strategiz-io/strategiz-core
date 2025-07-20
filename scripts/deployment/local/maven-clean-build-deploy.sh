#!/bin/bash

#=============================================================================
# Strategiz Core - Maven Clean Build and Deploy Script
# Modern, efficient Maven-based build and deploy script
#=============================================================================

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
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

print_maven() {
    echo -e "${PURPLE}🔨 Maven: $1${NC}"
}

# Change to project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
cd "$PROJECT_ROOT"

print_step "Strategiz Core - Maven Build System"
echo "Project root: $PROJECT_ROOT"
echo "Build system: Apache Maven"
echo "Timestamp: $(date)"
echo

# Pre-build checks
print_step "Pre-build Environment Checks"

# Check if Maven is available
if ! command -v mvn &> /dev/null; then
    print_error "Maven (mvn) is not installed or not in PATH"
    exit 1
fi
MAVEN_VERSION=$(mvn --version | head -n1)
print_success "Maven found: $MAVEN_VERSION"

# Check if Java is available
if ! command -v java &> /dev/null; then
    print_error "Java is not installed or not in PATH"
    exit 1
fi
JAVA_VERSION=$(java -version 2>&1 | head -n1)
print_success "Java found: $JAVA_VERSION"

# Check Maven version compatibility
if mvn --version | grep -q "Apache Maven 3\.[89]"; then
    print_success "Maven version is compatible (3.8+)"
else
    print_warning "Maven version may be older than recommended (3.8+)"
fi

# Check if Vault process is running (optional but recommended)
if pgrep -f "vault server" > /dev/null; then
    print_success "Vault server is running"
else
    print_warning "Vault server doesn't appear to be running. You may want to start it with: vault server -dev"
fi

echo

# Maven-specific setup
print_step "Maven Configuration"

# Set Maven options for better performance
export MAVEN_OPTS="-Xmx2g -XX:MaxMetaspaceSize=512m -XX:+UseG1GC"
print_maven "Memory settings: $MAVEN_OPTS"

# Check for Maven wrapper
if [ -f "./mvnw" ]; then
    MVN_CMD="./mvnw"
    print_maven "Using Maven wrapper (mvnw)"
else
    MVN_CMD="mvn"
    print_maven "Using system Maven"
fi

# Display Maven effective settings
print_maven "Effective POM validation..."
$MVN_CMD help:effective-pom -q -N > /dev/null 2>&1
if [ $? -eq 0 ]; then
    print_success "Maven configuration is valid"
else
    print_error "Maven POM configuration has issues"
    exit 1
fi

echo

# Clean build using Maven reactor
print_step "Phase 1: Maven Clean Build"
echo "Building with Maven reactor for optimal dependency resolution..."

# Create build log
BUILD_LOG="$PROJECT_ROOT/maven-build.log"
echo "Build log: $BUILD_LOG"

print_maven "Executing: mvn clean install -DskipTests -T 1C"

# Perform clean install with optimized settings
$MVN_CMD clean install \
    -DskipTests \
    -T 1C \
    --batch-mode \
    -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn \
    -Dmaven.compile.fork=true \
    -Dmaven.compiler.maxmem=1024m \
    2>&1 | tee "$BUILD_LOG"

BUILD_EXIT_CODE=${PIPESTATUS[0]}

if [ $BUILD_EXIT_CODE -eq 0 ]; then
    print_success "Maven build completed successfully!"
    
    # Show build summary
    if grep -q "BUILD SUCCESS" "$BUILD_LOG"; then
        BUILD_TIME=$(grep "Total time:" "$BUILD_LOG" | tail -1)
        print_maven "$BUILD_TIME"
    fi
else
    print_error "Maven build failed. Check $BUILD_LOG for details."
    echo
    print_maven "Common Maven build issues:"
    echo "  • Module dependency conflicts"
    echo "  • Compilation errors"
    echo "  • Missing dependencies"
    echo "  • Memory issues (increase MAVEN_OPTS)"
    exit 1
fi

echo

# Maven-specific verification
print_step "Phase 2: Maven Build Verification"

# Check main application JAR
JAR_FILE="$PROJECT_ROOT/application/target/application-1.0-SNAPSHOT.jar"
if [ -f "$JAR_FILE" ]; then
    JAR_SIZE=$(du -h "$JAR_FILE" | cut -f1)
    print_success "Application JAR created: $JAR_SIZE"
    
    # Verify JAR structure
    if jar -tf "$JAR_FILE" | grep -q "BOOT-INF"; then
        print_success "Spring Boot JAR structure verified"
    else
        print_warning "JAR may not be a proper Spring Boot executable JAR"
    fi
else
    print_error "Application JAR not found at: $JAR_FILE"
    exit 1
fi

# Check Maven artifacts in local repository
LOCAL_REPO=$(mvn help:evaluate -Dexpression=settings.localRepository -q -DforceStdout)
print_maven "Local repository: $LOCAL_REPO"

# Verify key framework modules
FRAMEWORK_MODULES=("framework-secrets" "framework-exception" "framework-logging")
for module in "${FRAMEWORK_MODULES[@]}"; do
    MODULE_JAR="framework/$module/target/$module-1.0-SNAPSHOT.jar"
    if [ -f "$MODULE_JAR" ]; then
        print_success "Framework module: $module"
    else
        print_warning "Framework module JAR not found: $module"
    fi
done

# Check for dependency conflicts
print_maven "Checking for dependency conflicts..."
$MVN_CMD dependency:analyze-only -q > /dev/null 2>&1 || print_warning "Some dependency analysis warnings found"

echo

# Optional Maven-specific tests
read -p "Do you want to run Maven dependency tree analysis? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    print_step "Maven Dependency Analysis"
    print_maven "Generating dependency tree..."
    $MVN_CMD dependency:tree -Doutput=maven-dependencies.txt
    print_success "Dependency tree saved to maven-dependencies.txt"
    echo
fi

# Deploy the application
print_step "Phase 3: Application Deployment"

# Maven-specific environment checks
if [ -z "$VAULT_TOKEN" ]; then
    print_warning "VAULT_TOKEN not set. Make sure to export it if using Vault"
    echo "   Example: export VAULT_TOKEN=hvs.your-dev-token"
fi

# Check active Maven profiles
ACTIVE_PROFILES=$($MVN_CMD help:active-profiles -q | grep "Active Profiles" || echo "No active profiles")
print_maven "$ACTIVE_PROFILES"

echo "Starting Strategiz Core application..."
echo "Built with Maven reactor"
echo
echo "🌐 Backend: http://localhost:8080"
echo "📖 API Docs: http://localhost:8080/swagger-ui.html"
echo "💡 Health: http://localhost:8080/actuator/health"
echo "🔨 Build Tool: Maven"
echo
print_warning "Press Ctrl+C to stop the application"
echo

# Navigate to application directory and start
cd "$PROJECT_ROOT/application/target"

# Run with optimized settings for Maven build
java -Xmx1g -XX:+UseG1GC \
     -Dspring.profiles.active=dev \
     -Dlogging.level.org.springframework.web=INFO \
     -Dmaven.build.tool=maven \
     -jar application-1.0-SNAPSHOT.jar

# Cleanup message
echo
print_step "Application Stopped"
print_success "Strategiz Core (Maven build) has been stopped cleanly"
echo "Maven artifacts available in target/ directories"
echo "Local repository: $LOCAL_REPO"
echo "Build log: $BUILD_LOG"
echo
print_maven "Quick restart: java -jar $JAR_FILE --spring.profiles.active=dev"