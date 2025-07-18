// Generated from pom.xml - please review and adjust as needed

description = "Device identity and fingerprinting service for Strategiz"

dependencies {
    implementation(project(":data:data-device"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("com.fasterxml.jackson.core:jackson-databind")
}
