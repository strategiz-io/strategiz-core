#!/bin/bash
echo "===================================================================="
echo "Building Strategiz Core in the correct dependency order..."
echo "===================================================================="
echo

# Change to the project root directory
cd "$(dirname "$0")/../.."

echo "Current working directory: $(pwd)"

echo "Step 1/6: Building framework modules"
echo "Building framework-exception"
mvn -f framework/framework-exception/pom.xml clean install -DskipTests
[ $? -ne 0 ] && echo "Build failed in framework-exception" && exit 1

echo "Building framework-logging"
mvn -f framework/framework-logging/pom.xml clean install -DskipTests
[ $? -ne 0 ] && echo "Build failed in framework-logging" && exit 1

echo "Building framework-secrets"
mvn -f framework/framework-secrets/pom.xml clean install -DskipTests
[ $? -ne 0 ] && echo "Build failed in framework-secrets" && exit 1

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

echo "Building data-auth"
mvn -f data/data-auth/pom.xml clean install -DskipTests
[ $? -ne 0 ] && echo "Build failed in data-auth" && exit 1

echo "Building data-exchange"
mvn -f data/data-exchange/pom.xml clean install -DskipTests
[ $? -ne 0 ] && echo "Build failed in data-exchange" && exit 1

echo "Building data-portfolio"
mvn -f data/data-portfolio/pom.xml clean install -DskipTests
[ $? -ne 0 ] && echo "Build failed in data-portfolio" && exit 1

echo "Building data-device"
mvn -f data/data-device/pom.xml clean install -DskipTests
[ $? -ne 0 ] && echo "Build failed in data-device" && exit 1

echo "Step 3/6: Building client modules"
echo "Building client-base"
mvn -f client/client-base/pom.xml clean install -DskipTests
[ $? -ne 0 ] && echo "Build failed in client-base" && exit 1

# Removed client-ccxt as it doesn't exist in the project

echo "Building client-coinbase"
mvn -f client/client-coinbase/pom.xml clean install -DskipTests
[ $? -ne 0 ] && echo "Build failed in client-coinbase" && exit 1

echo "Building client-coingecko"
mvn -f client/client-coingecko/pom.xml clean install -DskipTests
[ $? -ne 0 ] && echo "Build failed in client-coingecko" && exit 1

echo "Building client-binanceus"
mvn -f client/client-binanceus/pom.xml clean install -DskipTests
[ $? -ne 0 ] && echo "Build failed in client-binanceus" && exit 1

echo "Building client-alphavantage"
mvn -f client/client-alphavantage/pom.xml clean install -DskipTests
[ $? -ne 0 ] && echo "Build failed in client-alphavantage" && exit 1

echo "Building client-kraken"
mvn -f client/client-kraken/pom.xml clean install -DskipTests
[ $? -ne 0 ] && echo "Build failed in client-kraken" && exit 1

echo "Building client-walletaddress"
mvn -f client/client-walletaddress/pom.xml clean install -DskipTests
[ $? -ne 0 ] && echo "Build failed in client-walletaddress" && exit 1

echo "Building client-google"
mvn -f client/client-google/pom.xml clean install -DskipTests
[ $? -ne 0 ] && echo "Build failed in client-google" && exit 1

echo "Building client-facebook"
mvn -f client/client-facebook/pom.xml clean install -DskipTests
[ $? -ne 0 ] && echo "Build failed in client-facebook" && exit 1

echo "Step 4/6: Building business modules"
echo "Building business-base"
mvn -f business/business-base/pom.xml clean install -DskipTests
[ $? -ne 0 ] && echo "Build failed in business-base" && exit 1

echo "Building business-portfolio"
mvn -f business/business-portfolio/pom.xml clean install -DskipTests
[ $? -ne 0 ] && echo "Build failed in business-portfolio" && exit 1

echo "Building business-token-auth"
mvn -f business/business-token-auth/pom.xml clean install -DskipTests
[ $? -ne 0 ] && echo "Build failed in business-token-auth" && exit 1

echo "Step 5/6: Building service modules"
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

echo "Building service-provider"
mvn -f service/service-provider/pom.xml clean install -DskipTests
[ $? -ne 0 ] && echo "Build failed in service-provider" && exit 1

echo "Building service-profile"
mvn -f service/service-profile/pom.xml clean install -DskipTests
[ $? -ne 0 ] && echo "Build failed in service-profile" && exit 1

echo "Building service-device"
mvn -f service/service-device/pom.xml clean install -DskipTests
[ $? -ne 0 ] && echo "Build failed in service-device" && exit 1

echo "Building service-walletaddress"
mvn -f service/service-walletaddress/pom.xml clean install -DskipTests
[ $? -ne 0 ] && echo "Build failed in service-walletaddress" && exit 1

echo "Step 6/6: Building API modules"
# api-base has been merged into service-base - removed build step

# api-dashboard has been consolidated into service-dashboard


# api-strategy has been merged into service-strategy - removed build step

echo "Building service-monitoring"
mvn -f service/service-monitoring/pom.xml clean install -DskipTests
[ $? -ne 0 ] && echo "Build failed in service-monitoring" && exit 1

# api-portfolio has been merged into service-portfolio - removed build step

# api-marketplace has been merged into service-marketplace - removed build step

# api-provider has been consolidated into service-provider

# api-auth has been consolidated into service-auth
# echo "Building api-auth"
# mvn -f api/api-auth/pom.xml clean install -DskipTests
# [ $? -ne 0 ] && echo "Build failed in api-auth" && exit 1

echo "Final Step: Building application"
echo "Building application"
mvn -f application/pom.xml clean install -DskipTests
[ $? -ne 0 ] && echo "Build failed in application" && exit 1

echo "Build successful!"
echo "To run the application, execute ./deploy.sh"
