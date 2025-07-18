#!/bin/bash

# Quick fix script for generated Gradle build files
# Removes Spring Boot plugin from modules that don't have main applications

echo "Fixing generated Gradle build files..."

# Find all build.gradle.kts files with problematic Spring Boot configs
find . -name "build.gradle.kts" -not -path "./application/*" -exec grep -l "module_name" {} \; | while read file; do
    echo "Fixing $file"
    
    # Remove Spring Boot plugin and configuration
    sed -i '' '/plugins {/,/}/d' "$file"
    sed -i '' '/springBoot {/,/}/d' "$file"
    sed -i '' '/tasks.named.*BootJar/,/}/d' "$file"
    sed -i '' '/tasks.named.*jar.*enabled = false/d' "$file"
    
    echo "Fixed $file"
done

echo "Build file fixes complete!"