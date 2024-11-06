plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
}

group = "rip.sunrise"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.asm)
    implementation(libs.asm.commons)
    implementation(libs.asm.tree)
}

tasks {
    jar {
        manifest.attributes(
            "Manifest-Version" to 1.0,
            "Premain-Class" to "rip.sunrise.agent.MainKt",
            "Can-Redefined-Classes" to true,
            "Can-Retransform-Classes" to true
        )
    }
    build {
        finalizedBy(shadowJar)
    }
}

kotlin {
    jvmToolchain(21)
}