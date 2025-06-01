#!/bin/bash
echo "Building and running Strategiz Core application..."

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

cd target
java -jar application-1.0-SNAPSHOT.jar --spring.profiles.active=dev

echo ""
echo "Strategiz Core application stopped."
