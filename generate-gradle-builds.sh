#!/bin/bash

# Script to generate build.gradle.kts files for all modules based on their pom.xml files
# This automates the conversion from Maven to Gradle build configuration

set -e

echo "================================================"
echo "Generating Gradle build files for all modules"
echo "================================================"

# Function to extract dependencies from pom.xml and generate build.gradle.kts
generate_build_file() {
    local module_path=$1
    local pom_file="$module_path/pom.xml"
    local build_file="$module_path/build.gradle.kts"
    
    # Skip if build.gradle.kts already exists
    if [ -f "$build_file" ]; then
        echo "✓ Skipping $module_path (build.gradle.kts already exists)"
        return
    fi
    
    # Skip if no pom.xml exists
    if [ ! -f "$pom_file" ]; then
        echo "⚠ Skipping $module_path (no pom.xml found)"
        return
    fi
    
    echo "→ Generating $build_file"
    
    # Extract module name and description
    local module_name=$(basename "$module_path")
    local description=$(grep -m1 "<description>" "$pom_file" | sed 's/.*<description>\(.*\)<\/description>.*/\1/' || echo "$module_name module")
    
    # Start building the gradle file
    cat > "$build_file" << 'EOF'
// Generated from pom.xml - please review and adjust as needed

EOF
    
    # Add description
    echo "description = \"$description\"" >> "$build_file"
    echo "" >> "$build_file"
    
    # Check if it's a Spring Boot application
    if grep -q "spring-boot-maven-plugin" "$pom_file"; then
        cat >> "$build_file" << 'EOF'
plugins {
    id("org.springframework.boot")
}

springBoot {
    mainClass.set("io.strategiz.${module_name.replace('-', '.')}.Application")
}

EOF
    fi
    
    # Add dependencies section
    echo "dependencies {" >> "$build_file"
    
    # Extract internal project dependencies
    grep -E "<groupId>io.strategiz</groupId>" "$pom_file" -A 1 | grep "<artifactId>" | while read -r line; do
        local artifact=$(echo "$line" | sed 's/.*<artifactId>\(.*\)<\/artifactId>.*/\1/')
        if [ "$artifact" != "strategiz-parent" ] && [ "$artifact" != "${module_name}" ]; then
            # Determine module path
            local dep_path=""
            case "$artifact" in
                framework-*) dep_path=":framework:$artifact" ;;
                data-*) dep_path=":data:$artifact" ;;
                client-*) dep_path=":client:$artifact" ;;
                business-*) dep_path=":business:$artifact" ;;
                service-*) dep_path=":service:$artifact" ;;
                application-*) dep_path=":$artifact" ;;
            esac
            
            if [ -n "$dep_path" ]; then
                echo "    implementation(project(\"$dep_path\"))" >> "$build_file"
            fi
        fi
    done
    
    # Extract Spring Boot dependencies
    if grep -q "spring-boot-starter-web" "$pom_file"; then
        echo "    implementation(\"org.springframework.boot:spring-boot-starter-web\")" >> "$build_file"
    fi
    if grep -q "spring-boot-starter-security" "$pom_file"; then
        echo "    implementation(\"org.springframework.boot:spring-boot-starter-security\")" >> "$build_file"
    fi
    if grep -q "spring-boot-starter-data-redis" "$pom_file"; then
        echo "    implementation(\"org.springframework.boot:spring-boot-starter-data-redis\")" >> "$build_file"
    fi
    if grep -q "spring-boot-starter-cache" "$pom_file"; then
        echo "    implementation(\"org.springframework.boot:spring-boot-starter-cache\")" >> "$build_file"
    fi
    if grep -q "spring-boot-starter-actuator" "$pom_file"; then
        echo "    implementation(\"org.springframework.boot:spring-boot-starter-actuator\")" >> "$build_file"
    fi
    if grep -q "spring-boot-starter-validation" "$pom_file"; then
        echo "    implementation(\"org.springframework.boot:spring-boot-starter-validation\")" >> "$build_file"
    fi
    
    # Extract other common dependencies
    if grep -q "firebase-admin" "$pom_file"; then
        echo "    implementation(\"com.google.firebase:firebase-admin:9.3.0\")" >> "$build_file"
    fi
    if grep -q "guava" "$pom_file"; then
        echo "    implementation(\"com.google.guava:guava:33.3.1-jre\")" >> "$build_file"
    fi
    if grep -q "commons-lang3" "$pom_file"; then
        echo "    implementation(\"org.apache.commons:commons-lang3:3.17.0\")" >> "$build_file"
    fi
    if grep -q "jackson" "$pom_file"; then
        echo "    implementation(\"com.fasterxml.jackson.core:jackson-databind\")" >> "$build_file"
    fi
    if grep -q "vault-java-driver" "$pom_file"; then
        echo "    implementation(\"com.bettercloud:vault-java-driver:5.1.0\")" >> "$build_file"
    fi
    if grep -q "openai-java" "$pom_file"; then
        echo "    implementation(\"com.theokanning.openai-gpt3-java:service:0.18.2\")" >> "$build_file"
    fi
    if grep -q "webauthn4j" "$pom_file"; then
        echo "    implementation(\"com.webauthn4j:webauthn4j-spring-security-core:0.7.0.RELEASE\")" >> "$build_file"
    fi
    if grep -q "paseto" "$pom_file"; then
        echo "    implementation(\"dev.paseto:jpaseto-api:0.7.0\")" >> "$build_file"
        echo "    implementation(\"dev.paseto:jpaseto-impl:0.7.0\")" >> "$build_file"
        echo "    implementation(\"dev.paseto:jpaseto-jackson:0.7.0\")" >> "$build_file"
    fi
    if grep -q "totp" "$pom_file"; then
        echo "    implementation(\"dev.samstevens.totp:totp:1.7.1\")" >> "$build_file"
    fi
    if grep -q "springdoc" "$pom_file"; then
        echo "    implementation(\"org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0\")" >> "$build_file"
    fi
    if grep -q "bucket4j" "$pom_file"; then
        echo "    implementation(\"com.bucket4j:bucket4j-core:8.13.1\")" >> "$build_file"
    fi
    if grep -q "micrometer" "$pom_file"; then
        echo "    implementation(\"io.micrometer:micrometer-registry-prometheus\")" >> "$build_file"
    fi
    
    # Add test dependencies if tests exist
    if [ -d "$module_path/src/test" ]; then
        echo "    " >> "$build_file"
        echo "    testImplementation(\"org.springframework.boot:spring-boot-starter-test\")" >> "$build_file"
    fi
    
    echo "}" >> "$build_file"
    
    # For Spring Boot applications, add boot jar configuration
    if grep -q "spring-boot-maven-plugin" "$pom_file"; then
        cat >> "$build_file" << 'EOF'

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = true
}

tasks.named<Jar>("jar") {
    enabled = false
}
EOF
    fi
}

# Process all modules
echo -e "\n🔧 Processing framework modules..."
for module in framework/*/; do
    [ -d "$module" ] && generate_build_file "$module"
done

echo -e "\n🔧 Processing data modules..."
for module in data/*/; do
    [ -d "$module" ] && generate_build_file "$module"
done

echo -e "\n🔧 Processing client modules..."
for module in client/*/; do
    [ -d "$module" ] && generate_build_file "$module"
done

echo -e "\n🔧 Processing business modules..."
for module in business/*/; do
    [ -d "$module" ] && generate_build_file "$module"
done

echo -e "\n🔧 Processing service modules..."
for module in service/*/; do
    [ -d "$module" ] && generate_build_file "$module"
done

echo -e "\n✅ Gradle build file generation complete!"
echo "📝 Please review the generated files and adjust dependencies as needed."
echo "🔍 Look for TODO comments in the generated files for manual adjustments."