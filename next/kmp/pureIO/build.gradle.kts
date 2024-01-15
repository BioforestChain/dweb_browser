plugins {
  id("kmp-library")
}

kotlin {
  kmpCommonTarget(project) {
    dependencies {
      api(libs.squareup.okio)

      implementation(projects.helper)
    }
  }
  kmpAndroidTarget(project) {
  }
  kmpIosTarget(project) {
  }
  kmpNodeJsTarget(project) {
  }
}

android {
  namespace = "org.dweb_browser.pure.io"
  compileSdk = libs.versions.compileSdkVersion.get().toInt()
  defaultConfig {
    minSdk = libs.versions.minSdkVersion.get().toInt()
  }
}

