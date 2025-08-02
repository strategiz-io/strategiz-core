#!/bin/bash

# Final comprehensive fix for all remaining dependency issues

echo "Applying final fixes..."

# Add JSON library to centralized dependencies
cat >> "build.gradle.kts" << 'EOF'

    // JSON library for wallet client
    dependency("org.json:json:20240303")
EOF

# Fix service-base with all needed dependencies
cat > "service/service-base/build.gradle.kts" << 'EOF'
description = "Base service classes and utilities"

dependencies {
    // Spring dependencies - versions managed by parent
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-web")
    implementation("org.springframework:spring-webmvc")
    
    // Jakarta Servlet API
    implementation("jakarta.servlet:jakarta.servlet-api")
    
    // HTTP client - version managed by parent
    implementation("org.apache.httpcomponents.client5:httpclient5")
    
    // Firebase - version managed by parent
    implementation("com.google.firebase:firebase-admin")
    implementation("com.google.cloud:google-cloud-firestore")
    
    // Jackson - version managed by Spring Boot BOM
    implementation("com.fasterxml.jackson.core:jackson-databind")
    
    // Logging - version managed by Spring Boot BOM
    implementation("org.slf4j:slf4j-api")
    
    // Test dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
EOF

# Fix data modules that need Spring Data JPA
for module in "data-watchlist" "data-preferences" "data-providers" "data-devices" "data-user" "data-strategy"; do
    cat > "data/$module/build.gradle.kts" << EOF
description = "${module//-/ } data models and repositories"

dependencies {
    implementation(project(":data:data-base"))
    
    // Spring dependencies - versions managed by parent
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework:spring-context")
    implementation("org.springframework.data:spring-data-commons")
    
    // Jakarta Persistence API - version managed by Spring Boot BOM
    implementation("jakarta.persistence:jakarta.persistence-api")
    implementation("jakarta.validation:jakarta.validation-api")
    
    // Spring Cloud GCP for Firestore - version managed by parent
    implementation("com.google.cloud:spring-cloud-gcp-starter-data-firestore")
    
    // Firebase and Google Cloud - versions managed by parent
    implementation("com.google.firebase:firebase-admin")
    implementation("com.google.cloud:google-cloud-firestore")
    
    // Jackson - version managed by Spring Boot BOM
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.core:jackson-annotations")
    
    // Logging - version managed by Spring Boot BOM
    implementation("org.slf4j:slf4j-api")
    
    // Test dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
EOF
done

# Fix business modules that need Spring Web
cat > "business/business-provider-coinbase/build.gradle.kts" << 'EOF'
description = "Coinbase provider business logic"

dependencies {
    implementation(project(":business:business-base"))
    implementation(project(":client:client-coinbase"))
    implementation(project(":framework:framework-exception"))
    
    // Spring dependencies - versions managed by parent
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework:spring-context")
    
    // Jackson - version managed by Spring Boot BOM
    implementation("com.fasterxml.jackson.core:jackson-databind")
    
    // Logging - version managed by Spring Boot BOM
    implementation("org.slf4j:slf4j-api")
    
    // Test dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
EOF

# Fix client-walletaddress with JSON dependency
cat > "client/client-walletaddress/build.gradle.kts" << 'EOF'
description = "Wallet address client"

dependencies {
    implementation(project(":client:client-base"))
    
    // Spring dependencies - versions managed by parent
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-beans")
    
    // HTTP client - version managed by parent
    implementation("org.apache.httpcomponents:httpclient")
    
    // JSON library - version managed by parent
    implementation("org.json:json")
    
    // Jackson - version managed by Spring Boot BOM
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.core:jackson-annotations")
    
    // Logging - version managed by Spring Boot BOM
    implementation("org.slf4j:slf4j-api")
    
    // Test dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
EOF

echo "Final fixes applied!"