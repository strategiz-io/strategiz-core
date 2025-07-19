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
