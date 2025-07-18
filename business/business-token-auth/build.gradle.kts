// Generated from pom.xml - please review and adjust as needed

description = "Token authentication business logic for Strategiz"

dependencies {
    implementation(project(":data:data-auth"))
    implementation(project(":data:data-session"))
    implementation(project(":business:business-base"))
    implementation("dev.paseto:jpaseto-api:0.7.0")
    implementation("dev.paseto:jpaseto-impl:0.7.0")
    implementation("dev.paseto:jpaseto-jackson:0.7.0")
}
