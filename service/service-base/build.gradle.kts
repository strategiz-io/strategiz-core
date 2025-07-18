// Generated from pom.xml - please review and adjust as needed

description = "Base components for Strategiz service modules"

dependencies {
    implementation(project(":framework:framework-exception"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("com.google.firebase:firebase-admin:9.3.0")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0")
}
