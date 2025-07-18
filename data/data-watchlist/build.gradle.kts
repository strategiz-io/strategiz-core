// Generated from pom.xml - please review and adjust as needed

description = "Data layer for user watchlist subcollection"

dependencies {
    implementation(project(":data:data-base"))
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("com.fasterxml.jackson.core:jackson-databind")
}
