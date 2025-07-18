#!/bin/bash

# Build comparison script for Maven vs Gradle migration
# This script helps verify that both build systems produce identical artifacts

set -e

echo "============================================"
echo "Build Comparison Script - Maven vs Gradle"
echo "============================================"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Create comparison directory
COMPARE_DIR="build-comparison"
mkdir -p $COMPARE_DIR

# Clean everything first
echo -e "\n${YELLOW}Cleaning previous builds...${NC}"
mvn clean > /dev/null 2>&1 || true
./gradlew clean > /dev/null 2>&1 || true
rm -rf $COMPARE_DIR/*

# Build with Maven
echo -e "\n${YELLOW}Building with Maven...${NC}"
time mvn clean install -DskipTests

# Copy Maven artifacts
echo -e "\n${YELLOW}Collecting Maven artifacts...${NC}"
mkdir -p $COMPARE_DIR/maven
find . -name "*.jar" -path "*/target/*" ! -path "*/.mvn/*" ! -name "*-sources.jar" ! -name "*-javadoc.jar" -exec cp {} $COMPARE_DIR/maven/ \;

# Build with Gradle (when wrapper is available)
if [ -f "./gradlew" ]; then
    echo -e "\n${YELLOW}Building with Gradle...${NC}"
    time ./gradlew clean build -x test
    
    # Copy Gradle artifacts
    echo -e "\n${YELLOW}Collecting Gradle artifacts...${NC}"
    mkdir -p $COMPARE_DIR/gradle
    find . -name "*.jar" -path "*/build/libs/*" ! -name "*-plain.jar" ! -name "*-sources.jar" ! -name "*-javadoc.jar" -exec cp {} $COMPARE_DIR/gradle/ \;
else
    echo -e "\n${RED}Gradle wrapper not found. Run this first:${NC}"
    echo "curl -s https://get.sdkman.io | bash"
    echo "sdk install gradle"
    echo "gradle wrapper --gradle-version=8.7"
    exit 1
fi

# Compare artifacts
echo -e "\n${YELLOW}Comparing artifacts...${NC}"
echo "Maven artifacts:"
ls -la $COMPARE_DIR/maven/ | grep -E "\.jar$" || echo "No Maven artifacts found"

echo -e "\nGradle artifacts:"
ls -la $COMPARE_DIR/gradle/ | grep -E "\.jar$" || echo "No Gradle artifacts found"

# Compare each JAR
echo -e "\n${YELLOW}Detailed JAR comparison:${NC}"
for maven_jar in $COMPARE_DIR/maven/*.jar; do
    if [ -f "$maven_jar" ]; then
        jar_name=$(basename "$maven_jar")
        gradle_jar="$COMPARE_DIR/gradle/$jar_name"
        
        if [ -f "$gradle_jar" ]; then
            echo -e "\nComparing $jar_name:"
            
            # Compare sizes
            maven_size=$(stat -f%z "$maven_jar" 2>/dev/null || stat -c%s "$maven_jar")
            gradle_size=$(stat -f%z "$gradle_jar" 2>/dev/null || stat -c%s "$gradle_jar")
            
            if [ "$maven_size" -eq "$gradle_size" ]; then
                echo -e "  Size: ${GREEN}MATCH${NC} ($maven_size bytes)"
            else
                echo -e "  Size: ${RED}DIFFER${NC} (Maven: $maven_size, Gradle: $gradle_size)"
            fi
            
            # Compare contents (class files only)
            mkdir -p $COMPARE_DIR/extracted/maven/$jar_name $COMPARE_DIR/extracted/gradle/$jar_name
            unzip -q "$maven_jar" -d $COMPARE_DIR/extracted/maven/$jar_name
            unzip -q "$gradle_jar" -d $COMPARE_DIR/extracted/gradle/$jar_name
            
            # Count class files
            maven_classes=$(find $COMPARE_DIR/extracted/maven/$jar_name -name "*.class" | wc -l)
            gradle_classes=$(find $COMPARE_DIR/extracted/gradle/$jar_name -name "*.class" | wc -l)
            
            if [ "$maven_classes" -eq "$gradle_classes" ]; then
                echo -e "  Classes: ${GREEN}MATCH${NC} ($maven_classes files)"
            else
                echo -e "  Classes: ${RED}DIFFER${NC} (Maven: $maven_classes, Gradle: $gradle_classes)"
            fi
        else
            echo -e "\n${RED}Missing in Gradle:${NC} $jar_name"
        fi
    fi
done

# Check for Gradle-only JARs
echo -e "\n${YELLOW}Checking for Gradle-only artifacts:${NC}"
for gradle_jar in $COMPARE_DIR/gradle/*.jar; do
    if [ -f "$gradle_jar" ]; then
        jar_name=$(basename "$gradle_jar")
        maven_jar="$COMPARE_DIR/maven/$jar_name"
        if [ ! -f "$maven_jar" ]; then
            echo -e "${RED}Gradle-only:${NC} $jar_name"
        fi
    fi
done

# Summary
echo -e "\n${YELLOW}============================================${NC}"
echo -e "${YELLOW}Build Comparison Complete${NC}"
echo -e "${YELLOW}============================================${NC}"
echo "Check $COMPARE_DIR/ for detailed artifact comparison"

# Cleanup extracted files
rm -rf $COMPARE_DIR/extracted