#!/bin/bash
echo "===================================================================="
echo "Building Strategiz Core in the correct dependency order..."
echo "===================================================================="
echo

# Record the start time
start_time=$(date +%s)

# Install the root POM first
echo "Installing root POM (strategiz-core/pom.xml)..."
cd .. # Go to strategiz-core directory from scripts
mvn clean install -N
if [ $? -ne 0 ]; then
    echo "Build failed installing root POM"
    cd scripts # Go back to scripts directory before exiting
    exit 1
fi
cd scripts # Go back to scripts directory to continue

echo "Step 1/6: Building framework modules"
cd ../framework/framework-core
mvn clean install -DskipTests
if [ $? -ne 0 ]; then
    echo "Build failed in framework-core"
    cd ../../scripts
    exit 1
fi

cd ../framework-api-docs
mvn clean install -DskipTests
if [ $? -ne 0 ]; then
    echo "Build failed in framework-api-docs"
    cd ../../scripts
    exit 1
fi
cd ../../scripts # Return to scripts directory from framework/framework-api-docs

# Install parent POM for data modules first
echo "Installing parent POM for data modules (data/pom.xml)..."
cd ../data # Relative to scripts directory
mvn clean install -N
if [ $? -ne 0 ]; then
    echo "Build failed installing data parent POM"
    cd ../scripts # Go back to scripts directory before exiting
    exit 1
fi
cd ../scripts # Go back to scripts directory to continue

echo "Step 2/6: Building data modules"
cd ../data/data-base
mvn clean install -DskipTests
if [ $? -ne 0 ]; then
    echo "Build failed in data-base"
    cd ../../scripts
    exit 1
fi
cd ../../scripts # Return to scripts directory

cd ../data/data-strategy
mvn clean install -DskipTests
if [ $? -ne 0 ]; then
    echo "Build failed in data-strategy"
    cd ../../scripts
    exit 1
fi
cd ../../scripts # Return to scripts directory

cd ../data/data-exchange
mvn clean install -DskipTests
if [ $? -ne 0 ]; then
    echo "Build failed in data-exchange"
    cd ../../scripts
    exit 1
fi
cd ../../scripts # Return to scripts directory

cd ../data/data-portfolio
mvn clean install -DskipTests
if [ $? -ne 0 ]; then
    echo "Build failed in data-portfolio"
    cd ../../scripts
    exit 1
fi
cd ../../scripts # Return to scripts directory

cd ../data/data-auth
mvn clean install -DskipTests
if [ $? -ne 0 ]; then
    echo "Build failed in data-auth"
    cd ../../scripts
    exit 1
fi
cd ../../scripts # Return to scripts directory

cd ../data/data-user
mvn clean install -DskipTests
if [ $? -ne 0 ]; then
    echo "Build failed in data-user"
    cd ../../scripts
    exit 1
fi
cd ../../scripts # Return to scripts directory

echo "Step 3/6: Building client modules"
cd ../client/client-base
mvn clean install -DskipTests
if [ $? -ne 0 ]; then
    echo "Build failed in client-base"
    cd ../../scripts
    exit 1
fi
cd ../../scripts

cd ../client/client-alphavantage
mvn clean install -DskipTests
if [ $? -ne 0 ]; then
    echo "Build failed in client-alphavantage"
    cd ../../scripts
    exit 1
fi
cd ../../scripts

cd ../client/client-kraken
mvn clean install -DskipTests
if [ $? -ne 0 ]; then
    echo "Build failed in client-kraken"
    cd ../../scripts
    exit 1
fi
cd ../../scripts

cd ../client/client-coinbase
mvn clean install -DskipTests
if [ $? -ne 0 ]; then
    echo "Build failed in client-coinbase"
    cd ../../scripts
    exit 1
fi
cd ../../scripts

cd ../client/client-binanceus
mvn clean install -DskipTests
if [ $? -ne 0 ]; then
    echo "Build failed in client-binanceus"
    cd ../../scripts
    exit 1
fi
cd ../..

echo "Step 4/6: Building service modules"
cd service/service-base
mvn clean install -DskipTests
if [ $? -ne 0 ]; then
    echo "Build failed in service-base"
    cd ../../scripts
    exit 1
fi
cd ../..

cd service/service-strategy
mvn clean install -DskipTests
if [ $? -ne 0 ]; then
    echo "Build failed in service-strategy"
    cd ../../scripts
    exit 1
fi
cd ../..

cd service/service-exchange
mvn clean install -DskipTests
if [ $? -ne 0 ]; then
    echo "Build failed in service-exchange"
    cd ../../scripts
    exit 1
fi
cd ../..

cd service/service-portfolio
mvn clean install -DskipTests
if [ $? -ne 0 ]; then
    echo "Build failed in service-portfolio"
    cd ../../scripts
    exit 1
fi
cd ../..

cd service/service-dashboard
mvn clean install -DskipTests
if [ $? -ne 0 ]; then
    echo "Build failed in service-dashboard"
    cd ../../scripts
    exit 1
fi
cd ../..

cd service/service-auth
mvn clean install -DskipTests
if [ $? -ne 0 ]; then
    echo "Build failed in service-auth"
    cd ../../scripts
    exit 1
fi
cd ../..

echo "Step 5/6: Building API modules"
cd api/api-base
mvn clean install -DskipTests
if [ $? -ne 0 ]; then
    echo "Build failed in api-base"
    cd ../../scripts
    exit 1
fi
cd ../..

cd api/api-dashboard
mvn clean install -DskipTests
if [ $? -ne 0 ]; then
    echo "Build failed in api-dashboard"
    cd ../../scripts
    exit 1
fi
cd ../..

cd api/api-exchange
mvn clean install -DskipTests
if [ $? -ne 0 ]; then
    echo "Build failed in api-exchange"
    cd ../../scripts
    exit 1
fi
cd ../../scripts # Return to scripts directory

cd ../data/data-userategy
mvn clean install -DskipTests
if [ $? -ne 0 ]; then
    echo "Build failed in data-userategy"
    cd ../scripts
    exit 1
fi
cd ../scripts # Return to scripts directory

cd api/api-strategy
mvn clean install -DskipTests
if [ $? -ne 0 ]; then
    echo "Build failed in api-strategy"
    cd ../../scripts
    exit 1
fi
cd ../..

cd api/api-portfolio
mvn clean install -DskipTests
if [ $? -ne 0 ]; then
    echo "Build failed in api-portfolio"
    cd ../../scripts
    exit 1
fi
cd ../..

cd api/api-auth
mvn clean install -DskipTests
if [ $? -ne 0 ]; then
    echo "Build failed in api-auth"
    cd ../../scripts
    exit 1
fi
cd ../..

echo "Step 6/6: Building application module"
cd application
mvn clean install -DskipTests
if [ $? -ne 0 ]; then
    echo "Build failed in application"
    cd ../scripts
    exit 1
fi
cd ..

# Return to scripts directory
cd scripts

# Calculate build time
end_time=$(date +%s)
elapsed=$((end_time - start_time))
hours=$((elapsed / 3600))
mins=$(((elapsed % 3600) / 60))
secs=$((elapsed % 60))

echo "===================================================================="
echo "Build completed successfully in $hours:$mins:$secs"
echo "===================================================================="
exit 0
