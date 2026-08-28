import org.gradle.kotlin.dsl.compileOnly

plugins {
    kotlin("jvm")
    id("com.gradleup.shadow")
}

repositories {
    maven("https://repo.helpch.at/releases")
    maven("https://maven.enginehub.org/repo/")
}

dependencies {

    implementation(project(":api"))
    implementation("online.veloraplugins:velora-engine:3.4.3")
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")

    compileOnly("com.github.shynixn.mccoroutine:mccoroutine-bukkit-api:2.22.0") {
        exclude(group = "org.jetbrains.kotlin")
    }

    compileOnly("com.github.shynixn.mccoroutine:mccoroutine-bukkit-core:2.22.0") {
        exclude(group = "org.jetbrains.kotlin")
    }

    compileOnly("org.incendo:cloud-paper:2.0.0-beta.17")
    compileOnly("org.incendo:cloud-minecraft-extras:2.0.0-beta.17")
    compileOnly("org.incendo:cloud-kotlin-coroutines:2.0.0")

    compileOnly("eu.okaeri:okaeri-configs-yaml-bukkit:5.0.6")
    compileOnly("eu.okaeri:okaeri-configs-serdes-bukkit:5.0.6")

    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.12") {
        exclude(group = "com.google.guava", module = "guava")
        exclude(group = "com.google.code.gson", module = "gson")
        exclude(group = "it.unimi.dsi", module = "fastutil")
    }

    compileOnly("me.clip:placeholderapi:2.12.2")
}

