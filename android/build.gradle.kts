// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.androidApplication) apply false
  alias(libs.plugins.kotlinAndroid) apply false
  alias(libs.plugins.androidLibrary) apply false
  alias(libs.plugins.kotlinJvm) apply false
  alias(libs.plugins.kspAndroid) apply false
  alias(libs.plugins.jetbrainsCompose) apply false
  alias(libs.plugins.kotlinxMultiplatform) apply false
}

/*
plugins {
  id("com.google.devtools.ksp") version "1.9.0-1.0.13" apply false
}

buildscript {
  repositories {
    google()
    mavenCentral()
    maven(
      "https://maven.pkg.jetbrains.space/public/p/ktor/eap"
    )
    maven("https://jitpack.io")
  }
  dependencies {
    classpath("com.android.tools.build:gradle:8.1.0")
    classpath(kotlin("gradle-plugin", version = "1.9.0"))
  }
}

tasks.register<Delete>("clean").configure {
  delete(rootProject.buildDir)
}
*/

//
