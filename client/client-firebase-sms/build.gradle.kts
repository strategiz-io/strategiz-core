description = "Firebase SMS client"

dependencies {
    implementation(project(":client:client-base"))
    implementation(project(":framework:framework-exception"))
    
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
    
    // Firebase
    implementation("com.google.firebase:firebase-admin")
    
    // Test dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
