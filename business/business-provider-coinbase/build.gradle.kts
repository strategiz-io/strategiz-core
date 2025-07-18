// Generated from pom.xml - please review and adjust as needed

description = "Business logic for Coinbase provider integration"

dependencies {
    implementation(project(":business:business-base"))
    implementation(project(":client:client-coinbase"))
    implementation(project(":framework:framework-exception"))
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("com.fasterxml.jackson.core:jackson-databind")
}
