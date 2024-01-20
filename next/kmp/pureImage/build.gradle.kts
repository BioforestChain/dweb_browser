plugins {
  id("kmp-compose")
}

kotlin {
  kmpComposeTarget(project) {
    dependencies {
      implementation(libs.ktor.server.cio)
      implementation(libs.ktor.server.websockets)

      implementation(projects.helper)
      implementation(projects.helperPlatform)
      implementation(projects.pureIO)
      implementation(projects.pureHttp)

      // 加载图片 coil
      implementation(libs.coil.core)
      implementation(libs.coil.network)
      implementation(libs.coil.compose)
      implementation(libs.coil.svg)
      implementation(libs.coil.video)
      implementation(libs.coil.gif)
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

