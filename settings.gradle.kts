plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
dependencyResolutionManagement {
}

rootProject.name = "Pillow"
include("client", "server", "agent", "packets")