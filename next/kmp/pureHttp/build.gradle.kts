plugins {
  id("kmp-library")
}

kotlin {
  kmpCommonTarget(project) {
    dependencies {
      implementation(projects.helper)
      implementation(projects.helperPlatform)
    }
  }
  val ktorMain by sourceSets.creating {
    dependsOn(sourceSets.commonMain.get())
    dependencies {
      api(libs.ktor.http)
      api(libs.ktor.client.cio)
      api(libs.ktor.client.encoding)

      implementation(libs.ktor.io)
      implementation(libs.ktor.server.websockets)
      implementation(libs.ktor.server.cio)
    }
  }
  kmpAndroidTarget(project) {
    dependsOn(ktorMain)
  }
  kmpIosTarget(project) {
    dependsOn(ktorMain)
  }
  kmpNodeJsTarget(project) {
//    dependsOn(ktorMain)
  }
}

android {
  namespace = "org.dweb_browser.pure.http"
  compileSdk = libs.versions.compileSdkVersion.get().toInt()
  defaultConfig {
    minSdk = libs.versions.minSdkVersion.get().toInt()
  }
}

