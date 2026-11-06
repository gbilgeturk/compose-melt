import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    `maven-publish`
}

// JitPack passes -Pgroup and -Pversion; these defaults are used for local builds.
val libGroup: String = providers.gradleProperty("group").orNull?.takeIf { it.isNotBlank() } ?: "dev.melt"
val libVersion: String = providers.gradleProperty("version").orNull?.takeIf { it.isNotBlank() } ?: "0.1.0"

android {
    namespace = "dev.melt"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

kotlin {
    explicitApi()
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    api(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.animation.core)

    testImplementation(libs.junit)
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = libGroup
            artifactId = "melt"
            version = libVersion

            afterEvaluate {
                from(components["release"])
            }

            pom {
                name.set("compose-melt")
                description.set("A Jetpack Compose Modifier that makes any composable melt and drip downward.")
                url.set("https://github.com/gbilgeturk/compose-melt")
                inceptionYear.set("2026")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("gbilgeturk")
                        name.set("Gokturk Bilgeturk")
                        url.set("https://github.com/gbilgeturk")
                    }
                }
                scm {
                    url.set("https://github.com/gbilgeturk/compose-melt")
                    connection.set("scm:git:https://github.com/gbilgeturk/compose-melt.git")
                    developerConnection.set("scm:git:ssh://git@github.com/gbilgeturk/compose-melt.git")
                }
                issueManagement {
                    system.set("GitHub Issues")
                    url.set("https://github.com/gbilgeturk/compose-melt/issues")
                }
            }
        }
    }
}
