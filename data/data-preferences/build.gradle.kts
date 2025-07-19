description = "data preferences data models and repositories"

dependencies {
    implementation(project(":data:data-base"))
    
    // Spring dependencies - versions managed by parent
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework:spring-context")
    implementation("org.springframework.data:spring-data-commons")
    
    // Jakarta Persistence API - version managed by Spring Boot BOM
    implementation("jakarta.persistence:jakarta.persistence-api")
    implementation("jakarta.validation:jakarta.validation-api")
    
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
