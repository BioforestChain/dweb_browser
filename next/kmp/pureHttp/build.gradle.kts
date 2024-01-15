plugins {
  id("kmp-library")
}

kotlin {
  kmpComposeTarget(project) {
    dependencies {
      implementation(projects.helper)
      implementation(projects.helperCompose)
      implementation(projects.helperPlatform)
    }
  }
  val androidAndIosMain by sourceSets.creating {
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
    dependsOn(androidAndIosMain)
  }
  kmpIosTarget(project) {
    dependsOn(androidAndIosMain)
  }
}

android {
  namespace = "org.dweb_browser.pure.http"
  compileSdk = libs.versions.compileSdkVersion.get().toInt()
  defaultConfig {
    minSdk = libs.versions.minSdkVersion.get().toInt()
  }
}

