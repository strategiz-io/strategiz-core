// Generated from pom.xml - please review and adjust as needed

description = "Provider and brokerage integration service for Strategiz"

dependencies {
    implementation(project(":data:data-user"))
    implementation(project(":service:service-base"))
    implementation(project(":business:business-provider-coinbase"))
    implementation(project(":client:client-kraken"))
    implementation(project(":client:client-binanceus"))
    implementation(project(":framework:framework-exception"))
    implementation(project(":business:business-token-auth"))
    implementation(project(":service:service-auth"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
}
