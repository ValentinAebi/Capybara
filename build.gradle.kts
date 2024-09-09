plugins {
    kotlin("jvm") version "2.0.20"
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
    implementation("io.ksmt:ksmt-cvc5:0.5.25")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}