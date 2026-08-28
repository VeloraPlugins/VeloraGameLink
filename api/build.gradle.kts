import org.gradle.api.publish.maven.MavenPublication

plugins {
    kotlin("jvm")
    `maven-publish`
}

group = "online.veloraplugins"
version = "1.0.0-SNAPSHOT"

dependencies {

    api(
        "online.velora.framework:eventbus:3.3.2-SNAPSHOT"
    )
}

kotlin {
    jvmToolchain(
        21
    )
}

java {
    withSourcesJar()
}

tasks.test {
    useJUnitPlatform()
}

/*
 * Publishing
 */

publishing {

    publications {

        create<MavenPublication>(
            "maven"
        ) {

            groupId =
                project.group.toString()

            artifactId =
                "velora-gameapi"

            version =
                project.version.toString()

            from(
                components[
                    "java"
                ]
            )
        }
    }

    repositories {

        maven {

            name =
                "Velora"

            url =
                uri(
                    if (
                        project.version
                            .toString()
                            .endsWith(
                                "-SNAPSHOT"
                            )
                    ) {

                        "https://nexus.veloraplugins.online/repository/maven-snapshots/"

                    } else {

                        "https://nexus.veloraplugins.online/repository/maven-releases/"
                    }
                )

            credentials {

                username =
                    providers
                        .gradleProperty(
                            "nexusUser"
                        )
                        .orNull

                password =
                    providers
                        .gradleProperty(
                            "nexusPassword"
                        )
                        .orNull
            }
        }
    }
}