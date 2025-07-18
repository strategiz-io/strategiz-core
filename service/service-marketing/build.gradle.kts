// Generated from pom.xml - please review and adjust as needed

description = "Marketing service with real-time market data for landing page"

dependencies {
    implementation(project(":service:service-base"))
    implementation(project(":client:client-coingecko"))
    implementation(project(":client:client-alphavantage")) // Keep for backtesting
    implementation(project(":client:client-coinbase"))
    implementation(project(":client:client-yahoo-finance")) // Add Yahoo Finance for landing page
    implementation("org.springframework.boot:spring-boot-starter-web")
    
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
