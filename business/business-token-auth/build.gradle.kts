description = "Token authentication business logic for Strategiz"

dependencies {
    implementation(project(":data:data-auth"))
    implementation(project(":data:data-session"))
    implementation(project(":data:data-base"))
    implementation(project(":business:business-base"))
    
    // Spring dependencies - versions managed by parent
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-beans")
    implementation("org.springframework.data:spring-data-commons")
    
    // Jakarta Annotation API (replaces javax.annotation)
    implementation("jakarta.annotation:jakarta.annotation-api")
    
    // PASETO dependencies - versions managed by parent (no version numbers!)
    implementation("dev.paseto:jpaseto-api")
    implementation("dev.paseto:jpaseto-impl")
    implementation("dev.paseto:jpaseto-jackson")
    
    // Logging - version managed by Spring Boot BOM
    implementation("org.slf4j:slf4j-api")
    
    // Test dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
