description = "Wallet address service"

dependencies {
    implementation(project(":service:service-base"))
    implementation(project(":framework:framework-exception"))
    
    // Spring dependencies - versions managed by parent
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-beans")
    
    // Firebase and Firestore - versions managed by parent
    implementation("com.google.firebase:firebase-admin")
    implementation("com.google.cloud:google-cloud-firestore")
    implementation("com.google.cloud:spring-cloud-gcp-starter-data-firestore")
    
    // Jackson - version managed by Spring Boot BOM
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.core:jackson-annotations")
    
    // Logging - version managed by Spring Boot BOM
    implementation("org.slf4j:slf4j-api")
    
    // Client dependencies
    implementation(project(":client:client-walletaddress"))
    
    // Data dependencies
    implementation(project(":data:data-user"))
    
    // Test dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
