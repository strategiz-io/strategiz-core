// Generated from pom.xml - please review and adjust as needed

description = "Secret management framework module using HashiCorp Vault for Strategiz"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-configuration-processor")
    implementation("org.springframework.cloud:spring-cloud-starter-vault-config:4.1.3")
    implementation(project(":framework:framework-exception"))
}
