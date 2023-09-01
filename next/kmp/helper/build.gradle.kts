plugins {

  alias(libs.plugins.kotlinxMultiplatform)
  alias(libs.plugins.androidLibrary)
//  id("kotlinx-atomicfu")
//    alias(libs.plugins.jetbrainsCompose)
}

kotlin {
  androidTarget {
    compilations.all {
      kotlinOptions {
        jvmTarget = "1.8"
      }
    }
  }

  jvm("desktop")

  listOf(
    iosX64(),
    iosArm64(),
    iosSimulatorArm64()
  ).forEach {
    it.binaries.framework {
      baseName = "helper"
    }
  }

  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(libs.okio)
        implementation(libs.ktor.http)
        implementation(libs.ktor.io)
        implementation(libs.kotlinx.datetime)
        implementation(libs.kotlin.serialization.json)
      }
    }
    val commonTest by getting {
      dependencies {
        implementation(kotlin("test"))
        implementation(libs.test.kotlin.coroutines.test)
        implementation(libs.test.kotlin.coroutines.debug)
//                implementation("org.jetbrains.kotlinx:atomicfu-gradle-plugin:0.22.0")
      }
    }
    val androidMain by getting
    val androidUnitTest by getting
    val iosX64Main by getting
    val iosArm64Main by getting
    val iosSimulatorArm64Main by getting
    val iosMain by creating {
      dependsOn(commonMain)
      iosX64Main.dependsOn(this)
      iosArm64Main.dependsOn(this)
      iosSimulatorArm64Main.dependsOn(this)
    }
    val iosX64Test by getting
    val iosArm64Test by getting
    val iosSimulatorArm64Test by getting
    val iosTest by creating {
      dependsOn(commonTest)
      iosX64Test.dependsOn(this)
      iosArm64Test.dependsOn(this)
      iosSimulatorArm64Test.dependsOn(this)
    }
  }
}

android {
  namespace = "org.dweb_browser.helper"
  compileSdk = 33
  defaultConfig {
    minSdk = 28
  }
}