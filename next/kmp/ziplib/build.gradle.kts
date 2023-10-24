import co.touchlab.cklib.gradle.CompileToBitcode.Language.C

plugins {
    alias(libs.plugins.kotlinxMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.cklib)
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
kotlin {
    targetHierarchy.default()

    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = libs.versions.jvmTarget.get()
            }
        }
    }
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(libs.versions.jvmTarget.get()))
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries {
            framework {
                baseName = "ziplib"
            }
        }
        val main by it.compilations.getting
        main.cinterops.create("miniz") {
            header("native/miniz/zip.h")
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                //put your multiplatform dependencies here
            }
        }
        val commonTest by getting {
            dependencies {
                kotlin("test")
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(project(":helper"))
            }
        }

        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by getting {
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
        }
    }
}

cklib {
    config.kotlinVersion = libs.versions.kotlin.version.get()
    create("miniz") {
        language = C
        srcDirs = project.files(file("native/miniz"))
    }
}

android {
    namespace = "org.dweb_browser.ziplib"
    compileSdk = 34
    defaultConfig {
        minSdk = 28

        ndk {
            abiFilters += listOf("x86", "x86_64", "armeabi-v7a", "arm64-v8a")
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/androidMain/CMakeLists.txt")
        }
    }
}
