#!/bin/bash

# Script to fix service modules with proper Spring dependencies

echo "Fixing service modules..."

# Fix service modules that need proper Spring dependencies
fix_service_module() {
    local module_path=$1
    local description=$2
    local extra_deps=$3
    
    echo "Fixing $module_path..."
    
    cat > "$module_path/build.gradle.kts" << EOF
description = "$description"

dependencies {
    implementation(project(":service:service-base"))
    implementation(project(":framework:framework-exception"))
    
    // Spring dependencies - versions managed by parent
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-beans")
    
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

# Fix service-base module (foundational)
cat > "service/service-base/build.gradle.kts" << 'EOF'
description = "Base service classes and utilities"

dependencies {
    // Spring dependencies - versions managed by parent
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework:spring-context")
    
    // Logging - version managed by Spring Boot BOM
    implementation("org.slf4j:slf4j-api")
    
    // Test dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
EOF

# Service modules with their specific dependencies
fix_service_module "service/service-dashboard" "Dashboard service for portfolio and market data" \
    "    // Data dependencies
    implementation(project(\":data:data-user\"))
    implementation(project(\":data:data-watchlist\"))
    implementation(project(\":data:data-portfolio\"))"

fix_service_module "service/service-device" "Device management service" \
    "    // Data dependencies
    implementation(project(\":data:data-device\"))"

fix_service_module "service/service-exchange" "Exchange integration service" \
    "    // Data dependencies
    implementation(project(\":data:data-exchange\"))
    implementation(project(\":data:data-portfolio\"))"

fix_service_module "service/service-marketplace" "Strategy marketplace service" \
    "    // Data dependencies
    implementation(project(\":data:data-strategy\"))
    implementation(project(\":data:data-user\"))"

fix_service_module "service/service-marketing" "Marketing and ticker service" \
    "    // Client dependencies
    implementation(project(\":client:client-coinbase\"))
    implementation(project(\":client:client-coingecko\"))
    implementation(project(\":client:client-alphavantage\"))
    
    // Cache support
    implementation(\"org.springframework.boot:spring-boot-starter-cache\")"

fix_service_module "service/service-monitoring" "Monitoring and observability service" \
    "    // Monitoring dependencies
    implementation(\"io.micrometer:micrometer-registry-prometheus\")"

fix_service_module "service/service-portfolio" "Portfolio management service" \
    "    // Data dependencies
    implementation(project(\":data:data-portfolio\"))
    implementation(project(\":client:client-kraken\"))
    
    // Firebase
    implementation(\"com.google.firebase:firebase-admin\")"

fix_service_module "service/service-profile" "User profile service" \
    "    // Data dependencies
    implementation(project(\":data:data-user\"))
    implementation(project(\":data:data-preferences\"))"

fix_service_module "service/service-provider" "Provider integration service" \
    "    // Data dependencies
    implementation(project(\":data:data-providers\"))
    implementation(project(\":data:data-user\"))"

fix_service_module "service/service-strategy" "Strategy management service" \
    "    // Data dependencies
    implementation(project(\":data:data-strategy\"))
    implementation(project(\":data:data-user\"))"

fix_service_module "service/service-walletaddress" "Wallet address service" \
    "    // Client dependencies
    implementation(project(\":client:client-walletaddress\"))
    
    // Data dependencies
    implementation(project(\":data:data-user\"))"

echo "Service modules fixed!"