plugins {
    kotlin("jvm")
}

group = "online.veloraplugins"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    api("online.velora.framework:eventbus:3.3.2-SNAPSHOT")
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}