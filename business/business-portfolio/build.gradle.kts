description = "Portfolio business logic and metrics"

dependencies {
    implementation(project(":business:business-base"))
    implementation(project(":data:data-portfolio"))
    implementation(project(":client:client-base"))
    implementation(project(":client:client-kraken"))
    implementation(project(":client:client-binanceus"))
    
    // Spring dependencies - versions managed by parent
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework:spring-context")
    
    // JSON library - version managed by parent
    implementation("org.json:json")
    
    // Firebase - version managed by parent
    implementation("com.google.firebase:firebase-admin")
    
    // Jackson - version managed by Spring Boot BOM
    implementation("com.fasterxml.jackson.core:jackson-databind")
    
    // Logging - version managed by Spring Boot BOM
    implementation("org.slf4j:slf4j-api")
    
    // Test dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
