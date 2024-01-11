plugins {
  id("kmp-library")
}

kotlin {
  kmpCommonTarget(libs) {
    dependencies {
      api(libs.squareup.okio)
      api(libs.kotlinx.datetime)
      api(libs.ktor.http)
      api(libs.ktor.io)
    }
  }
  kmpBrowserJsTarget(libs)
  kmpAndroidTarget(libs)
  kmpIosTarget(libs)
  kmpNodeWasmTarget(libs) {
    val ktorWasmVersion = "3.0.0-wasm2"
    dependencies {
      //noinspection GradleDependency
      api("io.ktor:ktor-io:$ktorWasmVersion")
      api("io.ktor:ktor-http:$ktorWasmVersion")
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

