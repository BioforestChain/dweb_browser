plugins {
  id("kmp-compose")
}

kotlin {
  kmpComposeTarget(project) {
    dependencies {
      implementation(libs.ktor.io)
      implementation(libs.ktor.client.cio)
      implementation(libs.ktor.client.core)
      implementation(libs.ktor.server.websockets)
      implementation(libs.ktor.server.cio)
      implementation(libs.ktor.server.core)

      implementation(projects.helper)
      implementation(projects.helperCompose)
      implementation(projects.helperPlatform)
      implementation(projects.pureIO)
      implementation(projects.pureCrypto)
      implementation(projects.pureHttp)
      implementation(projects.multipart)
    }
  }
  kmpAndroidTarget(project)
  kmpIosTarget(project)
}
