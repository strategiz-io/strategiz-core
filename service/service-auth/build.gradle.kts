description = "Authentication and authorization service module"

dependencies {
    // Internal dependencies
    implementation(project(":framework:framework-exception"))
    implementation(project(":framework:framework-logging"))
    implementation(project(":framework:framework-secrets"))
    implementation(project(":data:data-auth"))
    implementation(project(":data:data-user"))
    implementation(project(":data:data-session"))
    implementation(project(":data:data-device"))
    implementation(project(":business:business-token-auth"))
    implementation(project(":service:service-base"))
    implementation(project(":client:client-google"))
    implementation(project(":client:client-facebook"))
    implementation(project(":client:client-firebase-sms"))
    
    // Spring Boot dependencies
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    
    // Security
    implementation("org.springframework.security:spring-security-oauth2-client")
    implementation("org.springframework.security:spring-security-oauth2-jose")
    
    // JWT
    implementation("io.jsonwebtoken:jjwt-api:${property("jwtVersion")}")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:${property("jwtVersion")}")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:${property("jwtVersion")}")
    
    // Passkeys/WebAuthn
    implementation("com.webauthn4j:webauthn4j-core:0.22.1.RELEASE")
    implementation("com.webauthn4j:webauthn4j-spring-security:0.22.1.RELEASE")
    
    // TOTP (Time-based One-Time Password)
    implementation("dev.samstevens.totp:totp:1.7.1")
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.google.zxing:javase:3.5.3")
    
    // Firebase Auth
    implementation("com.google.firebase:firebase-auth")
    
    // OpenAPI/Swagger
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.4.0")
    
    // Apache Commons
    implementation("org.apache.commons:commons-lang3")
    implementation("commons-codec:commons-codec")
}