// Generated from pom.xml - please review and adjust as needed

description = "System monitoring and health check services"

dependencies {
    implementation(project(":service:service-exchange"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")
}
