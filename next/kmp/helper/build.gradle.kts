plugins {
  id("kmp-library")
}

kotlin {
  kmpCommonTarget(libs) {
    dependencies {
      api(libs.kotlinx.datetime)
      api(libs.ktor.http)
      api(libs.ktor.io)
    }
  }
  kmpBrowserJsTarget(libs)
  kmpAndroidTarget(libs)
  kmpIosTarget(libs)
  kmpNodeWasmTarget(libs) {
    dependencies {
      //noinspection GradleDependency
      api(libs.ktor.http.wasm)
      api(libs.ktor.io.wasm)
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

