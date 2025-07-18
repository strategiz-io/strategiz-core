// Service User module - User aggregate service

description = "User aggregate service module"

dependencies {
    implementation(project(":service:service-base"))
    implementation(project(":data:data-user"))
    implementation(project(":data:data-auth"))
    implementation(project(":data:data-session"))
    implementation(project(":data:data-device"))
    implementation(project(":data:data-devices"))
    implementation(project(":data:data-preferences"))
    implementation(project(":data:data-providers"))
    implementation(project(":data:data-watchlist"))
    
    implementation("org.springframework.boot:spring-boot-starter-web")
    
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}