plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "space.resolvingcode.eStatMCP"
version = "0.1.0"

repositories {
    mavenCentral()
}

val mcpVersion = "0.3.0"
val slf4jVersion = "2.0.9"
val ktorVersion = "3.1.1"

dependencies {
    implementation("io.modelcontextprotocol:kotlin-sdk:$mcpVersion")
    implementation("org.slf4j:slf4j-nop:$slf4jVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-xml:$ktorVersion")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}