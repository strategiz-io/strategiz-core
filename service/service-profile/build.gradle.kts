// Generated from pom.xml - please review and adjust as needed

description = ""

dependencies {
    implementation(project(":data:data-user"))
    implementation(project(":data:data-base"))
    implementation(project(":service:service-base"))
    implementation(project(":business:business-token-auth"))
    implementation(project(":framework:framework-exception"))
    implementation(project(":service:service-auth"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
}
