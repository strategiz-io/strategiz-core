#!/bin/bash

# Script to fix business modules

echo "Fixing business modules..."

# Fix business-base module
cat > "business/business-base/build.gradle.kts" << 'EOF'
description = "Base business logic classes and utilities"

dependencies {
    // Spring dependencies - versions managed by parent
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework:spring-context")
    
    // Jackson - version managed by Spring Boot BOM
    implementation("com.fasterxml.jackson.core:jackson-databind")
    
    // Logging - version managed by Spring Boot BOM
    implementation("org.slf4j:slf4j-api")
    
    // Test dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
EOF

# Fix business-portfolio module
cat > "business/business-portfolio/build.gradle.kts" << 'EOF'
description = "Portfolio business logic and metrics"

dependencies {
    implementation(project(":business:business-base"))
    implementation(project(":data:data-portfolio"))
    implementation(project(":client:client-kraken"))
    
    // Spring dependencies - versions managed by parent
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework:spring-context")
    
    // Firebase - version managed by parent
    implementation("com.google.firebase:firebase-admin")
    
    // Jackson - version managed by Spring Boot BOM
    implementation("com.fasterxml.jackson.core:jackson-databind")
    
    // Logging - version managed by Spring Boot BOM
    implementation("org.slf4j:slf4j-api")
    
    // Test dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
EOF

# Fix business-provider-coinbase module
cat > "business/business-provider-coinbase/build.gradle.kts" << 'EOF'
description = "Coinbase provider business logic"

dependencies {
    implementation(project(":business:business-base"))
    implementation(project(":client:client-coinbase"))
    implementation(project(":framework:framework-exception"))
    
    // Spring dependencies - versions managed by parent
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework:spring-context")
    
    // Jackson - version managed by Spring Boot BOM
    implementation("com.fasterxml.jackson.core:jackson-databind")
    
    // Logging - version managed by Spring Boot BOM
    implementation("org.slf4j:slf4j-api")
    
    // Test dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
EOF

echo "Business modules fixed!"