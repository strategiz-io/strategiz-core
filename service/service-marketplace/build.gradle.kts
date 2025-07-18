// Generated from pom.xml - please review and adjust as needed

description = "Marketplace business logic services"

dependencies {
    implementation(project(":service:service-base"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("com.google.firebase:firebase-admin:9.3.0")
}
