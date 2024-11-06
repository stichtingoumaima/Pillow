plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
}

group = "rip.sunrise.client"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.netty)
    implementation(libs.gson)
    implementation(project(":packets"))
}

tasks {
    build {
        finalizedBy(shadowJar)
    }
}

kotlin {
    jvmToolchain(21)
}