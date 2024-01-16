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
  applyDefaultHierarchyTemplate {
    common {
      group("ktor") {
        withAndroidTarget()
        withIos()
      }
    }
  }
  val ktorMain by sourceSets.creating {
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
  }
  kmpIosTarget(project) {
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

