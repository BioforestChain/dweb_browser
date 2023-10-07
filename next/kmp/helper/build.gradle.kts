plugins {
  alias(libs.plugins.kotlinxMultiplatform)
  alias(libs.plugins.androidLibrary)
  alias(libs.plugins.kotlinPluginSerialization)
}

kotlin {
  androidTarget {
    compilations.all {
      kotlinOptions {
        jvmTarget = libs.versions.jvmTarget.get()
      }
    }
  }

  jvm("desktop")
  jvmToolchain {
    languageVersion.set(JavaLanguageVersion.of(libs.versions.jvmTarget.get()))
  }

  listOf(
    iosX64(), iosArm64(), iosSimulatorArm64()
  ).forEach {
    it.binaries.framework {
      baseName = "DwebHelper"
    }
  }

  sourceSets {
    val commonMain by getting {
      dependencies {
        api(kotlin("stdlib"))
        api(libs.kotlinx.coroutines.core)
        api(libs.kotlinx.atomicfu)
        api(libs.ktor.http)
        api(libs.ktor.io)
        api(libs.ktor.server.websockets)
        api(libs.ktor.server.cio)
        api(libs.ktor.client.cio)
        api(libs.ktor.client.encoding)

//        api(libs.kotlinx.io)
        api(libs.squareup.okio)
        api(libs.kotlinx.datetime)
        api(libs.kotlin.serialization.json)
        api(libs.kotlin.serialization.cbor)
      }
    }
    val commonTest by getting {
      dependencies {
        implementation(kotlin("test"))
        implementation(libs.test.kotlin.coroutines.test)
        implementation(libs.test.kotlin.coroutines.debug)
        implementation(libs.kotlinx.atomicfu)
      }
    }
    val androidMain by getting {
      dependencies {
        implementation(libs.androidx.core.ktx)
        implementation(libs.commons.compress)
        api(libs.data.store)
        api(libs.data.store.preferences)
      }
    }
    val androidUnitTest by getting
    val iosX64Main by getting
    val iosArm64Main by getting
    val iosSimulatorArm64Main by getting
    val iosMain by creating {
      dependsOn(commonMain)
      dependencies {
        api(libs.ktor.client.darwin)
      }
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
  compileSdk = libs.versions.compileSdkVersion.get().toInt()
  defaultConfig {
    minSdk = libs.versions.minSdkVersion.get().toInt()
  }
}