#!/bin/bash

# Script to fix remaining client modules

echo "Fixing remaining client modules..."

# Fix client modules with proper Spring dependencies
fix_client_module() {
    local module_path=$1
    local description=$2
    local extra_deps=$3
    
    echo "Fixing $module_path..."
    
    cat > "$module_path/build.gradle.kts" << EOF
description = "$description"

dependencies {
    implementation(project(":client:client-base"))
    
    // Spring dependencies - versions managed by parent
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-beans")
    
    // HTTP client - version managed by parent
    implementation("org.apache.httpcomponents:httpclient")
    
    // Jackson - version managed by Spring Boot BOM
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.core:jackson-annotations")
    
    // Logging - version managed by Spring Boot BOM
    implementation("org.slf4j:slf4j-api")
    
$extra_deps
    
    // Test dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
EOF
}

# Fix client modules
fix_client_module "client/client-binanceus" "BinanceUS API client"

fix_client_module "client/client-facebook" "Facebook OAuth client"

fix_client_module "client/client-firebase-sms" "Firebase SMS client" \
    "    // Firebase
    implementation(\"com.google.firebase:firebase-admin\")"

fix_client_module "client/client-google" "Google OAuth client"

fix_client_module "client/client-walletaddress" "Wallet address client"

# Fix yahoofinance client if it exists
if [ -d "client/client-yahoo-finance" ]; then
    fix_client_module "client/client-yahoo-finance" "Yahoo Finance API client"
fi

echo "Client modules fixed!"