description = "Marketing and ticker service"

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
    
    // Client dependencies
    implementation(project(":client:client-coinbase"))
    implementation(project(":client:client-coingecko"))
    implementation(project(":client:client-alphavantage"))
    
    // Cache support
    implementation("org.springframework.boot:spring-boot-starter-cache")
    
    // Test dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
