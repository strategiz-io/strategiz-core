description = "Base components for Strategiz client modules"

dependencies {
    // Spring dependencies - versions managed by parent
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework:spring-web")
    implementation("org.springframework:spring-webmvc")
    
    // HTTP client - version managed by parent
    implementation("org.apache.httpcomponents:httpclient")
    
    // Logging
    implementation("org.slf4j:slf4j-api")
    
    // Test dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
