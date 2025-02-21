plugins {
    kotlin("jvm") version "2.1.10"
}

group = "com.github.valentinaebi.capybara"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.ow2.asm:asm-tree:9.7")
    implementation("org.ow2.asm:asm-analysis:9.7")
    implementation("org.ow2.asm:asm-util:9.7")
    implementation("io.ksmt:ksmt-core:0.5.25")
    implementation("io.ksmt:ksmt-z3:0.5.25")
    testImplementation(kotlin("test"))
    testImplementation("org.apache.maven.shared:maven-invoker:3.3.0")
}

tasks.test {
    useJUnitPlatform()
}