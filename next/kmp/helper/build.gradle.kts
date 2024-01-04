plugins {
  id("kmp-library")
}

kotlin {
  kmpJsTarget(libs, JsPlatform.Node, JsPlatform.Browser, JsPlatform.Electron)
  sourceSets {
    jsMain.dependencies {
      api(libs.whyoleg.cryptography.provider.webcrypto)
      api(libs.squareup.okio.nodefilesystem)
    }
    commonMain.dependencies {
      api(kotlin("stdlib"))
      api(libs.kotlinx.coroutines.core)
      api(libs.kotlinx.atomicfu)
      api(libs.ktor.http)
      api(libs.ktor.io)
      api(libs.ktor.client.encoding)
      api(libs.whyoleg.cryptography.core)

      api(libs.squareup.okio)
      api(libs.kotlinx.datetime)
      api(libs.kotlin.serialization.json)
      api(libs.kotlin.serialization.cbor)
    }
    commonTest.dependencies {
      implementation(kotlin("test"))
      implementation(libs.test.kotlin.coroutines.test)
      implementation(libs.test.kotlin.coroutines.debug)
      implementation(libs.kotlinx.atomicfu)
      implementation(libs.kotlinx.io)
    }
    androidMain.dependencies {
      implementation(libs.androidx.core.ktx)
      api(libs.whyoleg.cryptography.provider.jdk)
    }
    iosMain.dependencies {
      api(libs.whyoleg.cryptography.provider.openssl3.prebuilt)
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

