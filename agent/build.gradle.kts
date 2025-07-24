plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
}

group = "rip.sunrise"
version = "0.0.1"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation(libs.asm)
    implementation(libs.asm.commons)
    implementation(libs.asm.tree)
    implementation("com.github.Sunderw3k:InjectAPI:master-SNAPSHOT")
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

    shadowJar {
        isEnableRelocation = true
        relocationPrefix = "rip.sunrise.agent.shaded"

        relocate("rip.sunrise.injectapi", "rip.sunrise.injectapi")
        relocate("kotlin", "kotlin")
    }
}

kotlin {
    jvmToolchain(21)
}