pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.minecraftforge.net/") { name = "Forge" }
        maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
        maven("https://maven.fabricmc.net/")
        maven("https://maven.architectury.dev/") { name = "Architectury" }
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.8"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

stonecutter {
    create(rootProject) {
        versions("1.20.1", "1.21.1")
        branch("forge") { versions("1.20.1") }
        branch("neoforge") { versions("1.21.1") }
        branch("fabric") { versions("1.20.1", "1.21.1") }
        vcsVersion = "1.21.1"
    }
}

rootProject.name = "Siegeworks"
