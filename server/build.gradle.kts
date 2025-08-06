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
    implementation(libs.netty)
    implementation(libs.gson)
    implementation(libs.msgpack)

    implementation(project(":packets"))
}

tasks {
    jar {
        manifest {
            attributes(
                "Main-Class" to "rip.sunrise.server.MainKt"
            )
        }
    }

    build {
        finalizedBy(shadowJar)
    }
}

kotlin {
    jvmToolchain(21)
}