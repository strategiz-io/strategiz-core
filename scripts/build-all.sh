#!/bin/bash

echo "Building Strategiz Core in the correct order..."

echo "Step 1/6: Building framework modules"
mvn clean install -DskipTests -f framework/pom.xml
if [ $? -ne 0 ]; then
    echo "Build failed at step 1"
    exit 1
fi

echo "Step 2/6: Building data modules"
mvn clean install -DskipTests -f data/pom.xml
if [ $? -ne 0 ]; then
    echo "Build failed at step 2"
    exit 1
fi

echo "Step 3/6: Building client modules"
mvn clean install -DskipTests -f client/client-base/pom.xml
mvn clean install -DskipTests -f client/client-alphavantage/pom.xml
mvn clean install -DskipTests -f client/client-binanceus/pom.xml
mvn clean install -DskipTests -f client/client-coinbase/pom.xml
mvn clean install -DskipTests -f client/client-coingecko/pom.xml
mvn clean install -DskipTests -f client/client-kraken/pom.xml
if [ $? -ne 0 ]; then
    echo "Build failed at step 3"
    exit 1
fi

echo "Step 4/6: Building service modules"
mvn clean install -DskipTests -f service/pom.xml
if [ $? -ne 0 ]; then
    echo "Build failed at step 4"
    exit 1
fi

echo "Step 5/6: Building API modules"
mvn clean install -DskipTests -f api/pom.xml
if [ $? -ne 0 ]; then
    echo "Build failed at step 5"
    exit 1
fi

echo "Step 6/6: Building application module"
mvn clean install -DskipTests -f application/pom.xml
if [ $? -ne 0 ]; then
    echo "Build failed at step 6"
    exit 1
fi

echo "Build completed successfully!"
