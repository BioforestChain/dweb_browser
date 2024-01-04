plugins {
  id("kmp-compose")
}

kotlin {
  sourceSets.commonMain.dependencies {
    implementation(kotlin("stdlib"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.atomicfu)

    api(libs.ktor.server.websockets)
    api(libs.ktor.server.cio)
    api(libs.ktor.client.cio)
    api(libs.ktor.server.core)
    api(libs.ktor.io)
    api(libs.ktor.client.core)
    api(libs.kotlin.serialization.json)

    implementation(projects.helper)
    implementation(projects.helperCompose)
    implementation(projects.helperPlatform)
    implementation(projects.pureHttp)
    implementation(projects.multipart)
  }
  sourceSets.commonTest.dependencies {
    implementation(kotlin("test"))
    implementation(libs.test.kotlin.coroutines.test)
    implementation(libs.test.kotlin.coroutines.debug)
    implementation(libs.kotlinx.atomicfu)
  }
  sourceSets.androidMain.dependencies {
    api(libs.data.gson)
  }
}

android {
  namespace = "org.dweb_browser.microservice"
  compileSdk = libs.versions.compileSdkVersion.get().toInt()
  defaultConfig {
    minSdk = libs.versions.minSdkVersion.get().toInt()
  }
}

