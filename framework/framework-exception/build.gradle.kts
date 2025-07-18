description = "Standardized exception handling and logging framework"

dependencies {
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-web")
    implementation("org.springframework:spring-webmvc")
    implementation("org.slf4j:slf4j-api")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    compileOnly("jakarta.servlet:jakarta.servlet-api")
}
