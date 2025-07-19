description = "Dashboard service for portfolio and market data"

dependencies {
    implementation(project(":service:service-base"))
    implementation(project(":framework:framework-exception"))
    implementation(project(":business:business-portfolio"))
    
    // Spring dependencies - versions managed by parent
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-beans")
    
    // Jakarta Validation API
    implementation("jakarta.validation:jakarta.validation-api")
    
    // Jackson - version managed by Spring Boot BOM
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.core:jackson-annotations")
    
    // Logging - version managed by Spring Boot BOM
    implementation("org.slf4j:slf4j-api")
    
    // Data dependencies
    implementation(project(":data:data-user"))
    implementation(project(":data:data-watchlist"))
    implementation(project(":data:data-portfolio"))
    
    // Test dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
