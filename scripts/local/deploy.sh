#!/bin/bash
echo "===================================================================="
echo "Deploying Strategiz Core application..."
echo "===================================================================="
echo

# Check if application JAR exists
if [ ! -f "../../application/target/application-1.0-SNAPSHOT.jar" ]; then
    echo "Error: Application JAR not found."
    echo "Please run build.sh first or use build-and-deploy.sh"
    exit 1
fi

echo "Starting Strategiz Core application..."
echo "Press Ctrl+C to stop the application when finished."
echo

cd ../../application/target
java -jar application-1.0-SNAPSHOT.jar --spring.profiles.active=dev

echo
echo "Strategiz Core application stopped."

# Return to scripts directory
cd ../../../scripts
exit 0
