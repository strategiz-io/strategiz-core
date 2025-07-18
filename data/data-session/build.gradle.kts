// Generated from pom.xml - please review and adjust as needed

description = "Session data module for Strategiz platform"

dependencies {
    implementation(project(":data:data-base"))
    implementation("com.google.firebase:firebase-admin:9.3.0")
    implementation("com.fasterxml.jackson.core:jackson-databind")
}
