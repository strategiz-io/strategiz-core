// Generated from pom.xml - please review and adjust as needed

description = "Exchange and brokerage business logic services"

dependencies {
    implementation(project(":service:service-base"))
    implementation(project(":data:data-exchange"))
    implementation(project(":data:data-base"))
    implementation(project(":framework:framework-exception"))
    implementation(project(":client:client-kraken"))
    implementation(project(":client:client-coinbase"))
    implementation(project(":client:client-binanceus"))
    implementation("com.google.firebase:firebase-admin:9.3.0")
    implementation("com.fasterxml.jackson.core:jackson-databind")
}
