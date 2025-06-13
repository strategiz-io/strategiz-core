#!/bin/bash
echo "===================================================================="
echo "Building and deploying Strategiz Core application..."
echo "===================================================================="
echo

# Run the build script
./build.sh
if [ $? -ne 0 ]; then
    echo "Build failed with error code $?"
    exit 1
fi

echo
echo "Build completed successfully. Starting deployment..."
echo

# Run the deploy script
./deploy.sh
if [ $? -ne 0 ]; then
    echo "Deployment failed with error code $?"
    exit 1
fi

exit 0
