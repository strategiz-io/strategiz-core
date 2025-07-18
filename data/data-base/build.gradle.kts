description = "Base module for all data modules in Strategiz"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("com.fasterxml.jackson.core:jackson-annotations")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("com.google.firebase:firebase-admin:9.3.0")
    implementation("com.google.api-client:google-api-client")
    implementation("com.google.auth:google-auth-library-credentials")
    implementation("com.google.api:gax-grpc")
    implementation("io.grpc:grpc-auth")
    implementation("com.google.http-client:google-http-client")
    implementation("com.google.auth:google-auth-library-oauth2-http")
    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("jakarta.persistence:jakarta.persistence-api")
    runtimeOnly("com.h2database:h2")
}