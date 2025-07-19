description = "Base service classes and utilities"

dependencies {
    implementation(project(":framework:framework-exception"))
    
    // Spring dependencies - versions managed by parent
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-web")
    implementation("org.springframework:spring-webmvc")
    
    // SpringDoc OpenAPI - version managed by parent
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui")
    
    // Jakarta APIs
    implementation("jakarta.servlet:jakarta.servlet-api")
    implementation("jakarta.validation:jakarta.validation-api")
    
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
