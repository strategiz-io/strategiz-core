// Template build.gradle.kts for modules
// Copy this to each module and adjust dependencies as needed

description = "TODO: Add module description"

dependencies {
    // Internal module dependencies
    // implementation(project(":framework:framework-exception"))
    // implementation(project(":data:data-base"))
    
    // Spring dependencies (add as needed)
    // implementation("org.springframework.boot:spring-boot-starter")
    // implementation("org.springframework.boot:spring-boot-starter-web")
    
    // Common dependencies (usually inherited from root)
    // compileOnly("org.projectlombok:lombok") - already in root
    // annotationProcessor("org.projectlombok:lombok") - already in root
    
    // Testing (usually inherited from root)
    // testImplementation("org.springframework.boot:spring-boot-starter-test") - already in root
}

// Module-specific configurations (if needed)
// tasks.jar {
//     enabled = true
// }