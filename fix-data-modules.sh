#!/bin/bash

# Script to fix remaining data modules with proper Spring dependencies

echo "Fixing remaining data modules..."

# Common Spring dependencies template for data modules
read -r -d '' SPRING_DEPS << 'EOF'
description = "DESCRIPTION_PLACEHOLDER"

dependencies {
    implementation(project(":data:data-base"))
    
    // Spring dependencies - versions managed by parent
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework:spring-context")
    
    // Jackson - version managed by Spring Boot BOM
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.core:jackson-annotations")
    
    // Firebase - version managed by parent
    implementation("com.google.firebase:firebase-admin")
    
    // Logging - version managed by Spring Boot BOM
    implementation("org.slf4j:slf4j-api")
    
    // Test dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
EOF

# Fix data modules that are simple (no JPA/Firestore)
fix_simple_data_module() {
    local module_path=$1
    local description=$2
    
    echo "Fixing $module_path..."
    
    local deps=$(echo "$SPRING_DEPS" | sed "s/DESCRIPTION_PLACEHOLDER/$description/")
    echo "$deps" > "$module_path/build.gradle.kts"
}

# Fix data modules that need JPA/Firestore
fix_complex_data_module() {
    local module_path=$1
    local description=$2
    
    echo "Fixing $module_path..."
    
    cat > "$module_path/build.gradle.kts" << EOF
description = "$description"

dependencies {
    implementation(project(":data:data-base"))
    
    // Spring dependencies - versions managed by parent
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework:spring-context")
    implementation("org.springframework.data:spring-data-commons")
    
    // Jakarta Persistence API - version managed by Spring Boot BOM
    implementation("jakarta.persistence:jakarta.persistence-api")
    
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
}

# Fix simple data modules
fix_simple_data_module "data/data-devices" "User devices data models and repositories"
fix_simple_data_module "data/data-exchange" "Exchange credentials data models and repositories"
fix_simple_data_module "data/data-portfolio" "Portfolio data models and repositories"
fix_simple_data_module "data/data-preferences" "User preferences data models and repositories"
fix_simple_data_module "data/data-providers" "Provider data models and repositories"
fix_simple_data_module "data/data-strategy" "Strategy data models and repositories"
fix_simple_data_module "data/data-watchlist" "Watchlist data models and repositories"

# Fix complex data modules
fix_complex_data_module "data/data-session" "Session data models and repositories"
fix_complex_data_module "data/data-user" "User data models and repositories"

echo "Data modules fixed!"