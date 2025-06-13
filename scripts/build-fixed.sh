#!/bin/bash
echo "===================================================================="
echo "Building Strategiz Core in the correct dependency order..."
echo "===================================================================="
echo

# Store the project root directory
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROJECT_ROOT"

echo "Step 1/6: Building framework modules"
# Install the framework parent POM
echo "Building framework parent POM"
mvn -f framework/pom.xml clean install -N -DskipTests
[ $? -ne 0 ] && echo "Build failed in framework parent POM" && exit 1

echo "Building framework-core"
mvn -f framework/framework-core/pom.xml clean install -DskipTests
[ $? -ne 0 ] && echo "Build failed in framework-core" && exit 1

echo "Building framework-api-docs"
mvn -f framework/framework-api-docs/pom.xml clean install -DskipTests
[ $? -ne 0 ] && echo "Build failed in framework-api-docs" && exit 1

echo "Step 2/6: Building data modules"
echo "Building data-base"
mvn -f data/data-base/pom.xml clean install -DskipTests
[ $? -ne 0 ] && echo "Build failed in data-base" && exit 1

echo "Building data-strategy"
mvn -f data/data-strategy/pom.xml clean install -DskipTests
[ $? -ne 0 ] && echo "Build failed in data-strategy" && exit 1

echo "Building data-user"
mvn -f data/data-user/pom.xml clean install -DskipTests
[ $? -ne 0 ] && echo "Build failed in data-user" && exit 1

echo "Step 3/6: Building client modules"
echo "Building client-base"
mvn -f client/client-base/pom.xml clean install -DskipTests
[ $? -ne 0 ] && echo "Build failed in client-base" && exit 1

echo "Building client-ccxt"
mvn -f client/client-ccxt/pom.xml clean install -DskipTests
[ $? -ne 0 ] && echo "Build failed in client-ccxt" && exit 1

echo "Building client-coinbase"
mvn -f client/client-coinbase/pom.xml clean install -DskipTests
[ $? -ne 0 ] && echo "Build failed in client-coinbase" && exit 1

echo "Building client-coingecko"
mvn -f client/client-coingecko/pom.xml clean install -DskipTests
[ $? -ne 0 ] && echo "Build failed in client-coingecko" && exit 1

echo "Building client-binanceus"
mvn -f client/client-binanceus/pom.xml clean install -DskipTests
[ $? -ne 0 ] && echo "Build failed in client-binanceus" && exit 1

echo "Building client-kucoin"
mvn -f client/client-kucoin/pom.xml clean install -DskipTests
[ $? -ne 0 ] && echo "Build failed in client-kucoin" && exit 1

echo "Step 4/6: Building service modules"
echo "Building service-base"
mvn -f service/service-base/pom.xml clean install -DskipTests
[ $? -ne 0 ] && echo "Build failed in service-base" && exit 1

echo "Building service-strategy"
mvn -f service/service-strategy/pom.xml clean install -DskipTests
[ $? -ne 0 ] && echo "Build failed in service-strategy" && exit 1

echo "Building service-exchange"
mvn -f service/service-exchange/pom.xml clean install -DskipTests
[ $? -ne 0 ] && echo "Build failed in service-exchange" && exit 1

echo "Building service-portfolio"
mvn -f service/service-portfolio/pom.xml clean install -DskipTests
[ $? -ne 0 ] && echo "Build failed in service-portfolio" && exit 1

echo "Building service-dashboard"
mvn -f service/service-dashboard/pom.xml clean install -DskipTests
[ $? -ne 0 ] && echo "Build failed in service-dashboard" && exit 1

echo "Building service-auth"
mvn -f service/service-auth/pom.xml clean install -DskipTests
[ $? -ne 0 ] && echo "Build failed in service-auth" && exit 1

echo "Building service-marketplace"
mvn -f service/service-marketplace/pom.xml clean install -DskipTests
[ $? -ne 0 ] && echo "Build failed in service-marketplace" && exit 1

echo "Step 5/6: Building API modules"
echo "Building api-base"
mvn -f api/api-base/pom.xml clean install -DskipTests
[ $? -ne 0 ] && echo "Build failed in api-base" && exit 1

echo "Building api-dashboard"
mvn -f api/api-dashboard/pom.xml clean install -DskipTests
[ $? -ne 0 ] && echo "Build failed in api-dashboard" && exit 1

echo "Building api-exchange"
mvn -f api/api-exchange/pom.xml clean install -DskipTests
[ $? -ne 0 ] && echo "Build failed in api-exchange" && exit 1

echo "Building api-strategy" 
mvn -f api/api-strategy/pom.xml clean install -DskipTests
[ $? -ne 0 ] && echo "Build failed in api-strategy" && exit 1

echo "Building api-monitoring"
mvn -f api/api-monitoring/pom.xml clean install -DskipTests
[ $? -ne 0 ] && echo "Building api-monitoring" && exit 1

echo "Building api-portfolio"
mvn -f api/api-portfolio/pom.xml clean install -DskipTests
[ $? -ne 0 ] && echo "Build failed in api-portfolio" && exit 1

echo "Building api-auth"
mvn -f api/api-auth/pom.xml clean install -DskipTests
[ $? -ne 0 ] && echo "Build failed in api-auth" && exit 1

echo "Step 6/6: Building application"
echo "Building application"
mvn -f application/pom.xml clean install -DskipTests
[ $? -ne 0 ] && echo "Build failed in application" && exit 1

echo "Build successful!"
echo "To run the application, execute ./deploy.sh"
