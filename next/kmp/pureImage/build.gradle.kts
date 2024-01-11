plugins {
  id("kmp-compose")
}

kotlin {
  kmpComposeTarget(libs) {
    dependencies {
      implementation(libs.ktor.server.cio)
      implementation(libs.ktor.server.websockets)

      implementation(projects.helper)
      implementation(projects.helperPlatform)
      implementation(projects.pureIO)
      implementation(projects.pureHttp)
    }
  }
  val androidAndIosMain by sourceSets.creating{
    dependencies {
      api(libs.ktor.http)
      api(libs.ktor.client.cio)
      api(libs.ktor.client.encoding)

      implementation(libs.ktor.io)
      implementation(libs.ktor.server.websockets)
      implementation(libs.ktor.server.cio)
    }
  }
  kmpAndroidTarget(libs){
    dependsOn(androidAndIosMain)
  }
  kmpIosTarget(libs){
    dependsOn(androidAndIosMain)
  }
}

android {
  namespace = "org.dweb_browser.pure.image"
  compileSdk = libs.versions.compileSdkVersion.get().toInt()
  defaultConfig {
    minSdk = libs.versions.minSdkVersion.get().toInt()
  }
}

