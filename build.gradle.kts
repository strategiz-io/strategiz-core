plugins {
    id("org.springframework.boot") version "3.5.7" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    java
}

// Centralized version catalog - like Maven's properties section
extra["springBootVersion"] = "3.5.7"
extra["springCloudGcpVersion"] = "7.4.1"
extra["firebaseAdminVersion"] = "9.4.1"
extra["jacksonVersion"] = "2.15.2"
extra["guavaVersion"] = "33.3.1-jre"
extra["commonsLang3Version"] = "3.17.0"
extra["vaultDriverVersion"] = "5.1.0"
extra["openaiVersion"] = "0.18.2"
extra["webauthn4jVersion"] = "0.7.0.RELEASE"
extra["pasetoVersion"] = "0.7.0"
extra["totpVersion"] = "1.7.1"
extra["springdocVersion"] = "2.8.0"
extra["bucket4jVersion"] = "8.13.1"
extra["httpclient5Version"] = "5.3.1"
extra["httpclientVersion"] = "4.5.14"
extra["jjwtVersion"] = "0.12.3"

allprojects {
    group = "io.strategiz"
    version = "1.0-SNAPSHOT"
    
    repositories {
        mavenCentral()
        maven { url = uri("https://repo.spring.io/milestone") }
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "io.spring.dependency-management")
    
    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    
    the<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension>().apply {
        imports {
            mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
            mavenBom("com.google.cloud:spring-cloud-gcp-dependencies:${rootProject.extra["springCloudGcpVersion"]}")
        }
        
        dependencies {
            // Centralized dependency versions - like Maven's dependencyManagement
            dependency("com.google.firebase:firebase-admin:${rootProject.extra["firebaseAdminVersion"]}")
            dependency("com.google.guava:guava:${rootProject.extra["guavaVersion"]}")
            dependency("org.apache.commons:commons-lang3:${rootProject.extra["commonsLang3Version"]}")
            dependency("com.bettercloud:vault-java-driver:${rootProject.extra["vaultDriverVersion"]}")
            dependency("com.theokanning.openai-gpt3-java:service:${rootProject.extra["openaiVersion"]}")
            dependency("com.webauthn4j:webauthn4j-spring-security-core:${rootProject.extra["webauthn4jVersion"]}")
            dependency("dev.paseto:jpaseto-api:${rootProject.extra["pasetoVersion"]}")
            dependency("dev.paseto:jpaseto-impl:${rootProject.extra["pasetoVersion"]}")
            dependency("dev.paseto:jpaseto-jackson:${rootProject.extra["pasetoVersion"]}")
            dependency("dev.samstevens.totp:totp:${rootProject.extra["totpVersion"]}")
            dependency("org.springdoc:springdoc-openapi-starter-webmvc-ui:${rootProject.extra["springdocVersion"]}")
            dependency("com.bucket4j:bucket4j-core:${rootProject.extra["bucket4jVersion"]}")
            dependency("org.apache.httpcomponents.client5:httpclient5:${rootProject.extra["httpclient5Version"]}")
            dependency("org.apache.httpcomponents:httpclient:${rootProject.extra["httpclientVersion"]}")
            dependency("io.jsonwebtoken:jjwt-api:${rootProject.extra["jjwtVersion"]}")
            dependency("io.jsonwebtoken:jjwt-impl:${rootProject.extra["jjwtVersion"]}")
            dependency("io.jsonwebtoken:jjwt-jackson:${rootProject.extra["jjwtVersion"]}")
            dependency("org.json:json:20240303")
        }
    }
    
    dependencies {
        // Common dependencies for all modules - no versions needed!
        compileOnly("org.projectlombok:lombok")
        annotationProcessor("org.projectlombok:lombok")
        
        // Test dependencies
        testImplementation("org.springframework.boot:spring-boot-starter-test")
        testImplementation("org.junit.jupiter:junit-jupiter")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    }
    
    tasks.withType<Test> {
        useJUnitPlatform()
    }
    
    // Only disable BootJar for non-application modules
    tasks.withType<org.springframework.boot.gradle.tasks.bundling.BootJar> {
        enabled = false
    }
    
    tasks.withType<Jar> {
        enabled = true
    }
}
