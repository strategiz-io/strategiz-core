// Generated from pom.xml - please review and adjust as needed

description = "Portfolio business logic services"

dependencies {
    implementation(project(":data:data-portfolio"))
    implementation(project(":client:client-kraken"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("com.google.firebase:firebase-admin:9.3.0")
    implementation("com.fasterxml.jackson.core:jackson-databind")
}
