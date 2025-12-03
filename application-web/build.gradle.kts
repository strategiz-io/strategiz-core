import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("org.springframework.boot")
}

description = "Main application module for bootstrapping Strategiz Core"

springBoot {
    mainClass.set("io.strategiz.application.Application")
}

dependencies {
    // Essential Spring Boot Starter
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    
    // Framework modules
    implementation(project(":framework:framework-api-docs"))
    
    // Service modules
    implementation(project(":service:service-auth"))
    implementation(project(":service:service-dashboard"))
    implementation(project(":service:service-device"))
    implementation(project(":service:service-exchange"))
    implementation(project(":service:service-marketplace"))
    implementation(project(":service:service-marketing"))
    implementation(project(":service:service-monitoring"))
    implementation(project(":service:service-portfolio"))
    implementation(project(":service:service-profile"))
    implementation(project(":service:service-provider"))
    implementation(project(":service:service-strategy"))
    
    // Business modules
    implementation(project(":business:business-token-auth"))
    
    // Test dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.named<BootJar>("bootJar") {
    enabled = true
    archiveClassifier.set("")
    
    manifest {
        attributes["Main-Class"] = "io.strategiz.application.Application"
        attributes["Implementation-Title"] = "Strategiz Core"
        attributes["Implementation-Version"] = project.version
    }
}

tasks.named<Jar>("jar") {
    enabled = false
}

// Task to run the application
tasks.named<JavaExec>("bootRun") {
    jvmArgs = listOf(
        "-Xmx1024m",
        "-Dspring.profiles.active=dev",
        "-Dspring.output.ansi.enabled=always"
    )
}