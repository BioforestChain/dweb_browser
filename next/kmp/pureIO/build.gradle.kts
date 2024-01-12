plugins {
  id("kmp-library")
}

kotlin {
  kmpCommonTarget(libs) {
    dependencies {
      api(libs.squareup.okio)

      implementation(projects.helper)
    }
  }
  kmpAndroidTarget(libs) {
  }
  kmpIosTarget(libs) {
  }
  kmpBrowserJsTarget(libs) {
  }
  kmpNodeWasmTarget(libs) {
    dependencies {
      //noinspection GradleDependency
      implementation(libs.ktor.http.wasm)
      implementation(libs.ktor.io.wasm)

      implementation(projects.helperPlatformNode)
    }
  }
}

android {
  namespace = "org.dweb_browser.pure.io"
  compileSdk = libs.versions.compileSdkVersion.get().toInt()
  defaultConfig {
    minSdk = libs.versions.minSdkVersion.get().toInt()
  }
}

