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
  jvmToolchain {
    languageVersion.set(JavaLanguageVersion.of(libs.versions.jvmTarget.get()))
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
        api(libs.whyoleg.cryptography.core)

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
        implementation(libs.kotlinx.io)
      }
    }
    val androidMain by getting {
      dependencies {
        implementation(libs.androidx.core.ktx)
        implementation(libs.commons.compress)
        api(libs.data.store)
        api(libs.data.store.preferences)
        api(libs.whyoleg.cryptography.provider.jdk)
      }
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