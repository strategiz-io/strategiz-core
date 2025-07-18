description = "Yahoo Finance API client for real-time stock data"

dependencies {
    // Spring dependencies - versions managed by parent
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-web")
    
    // Jackson - version managed by Spring Boot BOM
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.core:jackson-annotations")
    
    // Caching support
    implementation("org.springframework.boot:spring-boot-starter-cache")
    
    // Logging - version managed by Spring Boot BOM
    implementation("org.slf4j:slf4j-api")
    
    // Test dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}