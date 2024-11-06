plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
}

group = "rip.sunrise.packets"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.netty)
}

tasks {
    build {
        finalizedBy(shadowJar)
    }
}

kotlin {
    jvmToolchain(21)
}