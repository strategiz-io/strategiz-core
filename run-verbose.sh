#!/bin/bash
echo "Building and running Strategiz Core application with verbose output..."

cd application
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "Build failed!"
    exit $?
fi

echo ""
echo "Starting Strategiz Core application..."
echo "Press Ctrl+C to stop the application when finished."
echo ""

# Run the application with verbose output
cd target
java -jar application-1.0-SNAPSHOT.jar --debug --spring.profiles.active=dev --logging.level.root=INFO --logging.level.io.strategiz=DEBUG --logging.level.org.springframework=DEBUG

echo ""
echo "Strategiz Core application stopped."
