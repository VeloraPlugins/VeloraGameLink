import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.internal.extensions.stdlib.capitalized
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.3.21"
    id("com.gradleup.shadow") version "9.4.1" apply true
}

group = "online.veloraplugins"
version = "1.0.0-SNAPSHOT"

val jvmVersion = 21

allprojects {

    group = rootProject.group
    version = rootProject.version

    repositories {

        mavenCentral()

        maven("https://nexus.veloraplugins.online/repository/maven-public/") {

            val user = providers.gradleProperty("nexusUser").orNull
            val pass = providers.gradleProperty("nexusPassword").orNull

            if (user != null && pass != null) {
                credentials {
                    username = user
                    password = pass
                }
            }
        }

        maven("https://nexus.veloraplugins.online/repository/maven-snapshots/") {

            val user = providers.gradleProperty("nexusUser").orNull
            val pass = providers.gradleProperty("nexusPassword").orNull

            if (user != null && pass != null) {
                credentials {
                    username = user
                    password = pass
                }
            }
        }

        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.velocitypowered.com/snapshots/")
        maven("https://storehouse.okaeri.eu/repository/maven-public/")
        maven("https://storehouse.okaeri.eu/repository/maven-releases/")
        maven("https://repo.alessiodp.com/releases/")
        maven("https://jitpack.io/")
    }
}

subprojects {

    apply(plugin = "org.jetbrains.kotlin.jvm")

    dependencies {

        compileOnly("eu.okaeri:okaeri-configs:5.0.6")
        compileOnly("eu.okaeri:okaeri-configs-serdes-commons:5.0.6")
        compileOnly("eu.okaeri:okaeri-configs-yaml-snakeyaml:5.0.6")

        compileOnly("org.jetbrains.exposed:exposed-core:1.3.0")
        compileOnly("org.jetbrains.exposed:exposed-dao:1.3.0")
        compileOnly("org.jetbrains.exposed:exposed-jdbc:1.3.0")

        compileOnly("com.zaxxer:HikariCP:7.1.0")
        compileOnly("org.mariadb.jdbc:mariadb-java-client:3.5.9")

        testImplementation(kotlin("test"))
    }

    kotlin {

        jvmToolchain(jvmVersion)

        compilerOptions {
            jvmTarget.set(
                JvmTarget.fromTarget(
                    jvmVersion.toString()
                )
            )
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.release.set(jvmVersion)
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    tasks.withType<ShadowJar>().configureEach {

        dependencies {

            exclude(
                dependency("org.jetbrains:.*")
            )

            exclude(
                dependency("org.intellij:.*")
            )

            exclude(
                dependency("org.jetbrains.kotlin:.*")
            )

            exclude(
                dependency("org.jetbrains.kotlinx:.*")
            )
        }
    }

    tasks.processResources {

        filteringCharset = "UTF-8"

        inputs.property("version", project.version)

        filesMatching(
            listOf(
                "paper-plugin.yml",
                "velocity-plugin.json"
            )
        ) {
            expand("version" to project.version)
        }
    }

    tasks {

        build {

            dependsOn(
                ":paper:shadowJar",
                ":velocity:shadowJar"
            )
        }
    }

    plugins.withId("com.gradleup.shadow") {

        tasks.named<ShadowJar>("shadowJar") {

            archiveClassifier.set("")

            archiveFileName.set(
                "${rootProject.name}-${project.name.capitalized()}-${project.version}.jar"
            )

            destinationDirectory.set(
                rootProject.layout.buildDirectory.dir("libs")
            )

            val relocationBase =
                "online.veloraplugins.${project.name.lowercase()}.libs"

            relocate(
                "com.cryptomorin.xseries",
                "$relocationBase.xseries"
            )

            relocate(
                "online.velora.framework",
                "$relocationBase.framework"
            )

            if (project.name == "velocity") {
                relocate(
                    "com.github.shynixn.mccoroutine",
                    "$relocationBase.mccoroutine"
                )
            }
        }
    }
}