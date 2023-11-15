import org.dweb_browser.buildsrc.commonMobileTarget

plugins {
  id(libs.plugins.kotlinxMultiplatform.get().pluginId)
  id(libs.plugins.androidLibrary.get().pluginId)
  kotlin("plugin.serialization") version (libs.versions.kotlin.version)
}

kotlin {
  commonMobileTarget()

  sourceSets.commonMain.dependencies {
    implementation(kotlin("stdlib"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.atomicfu)

    api(libs.ktor.server.websockets)
    api(libs.ktor.server.cio)
    api(libs.ktor.client.cio)
    api(libs.ktor.server.core)
    api(libs.ktor.io)
    api(libs.kotlin.serialization.json)

    implementation(projects.helper)
    implementation(projects.helperCompose)
    implementation(projects.helperPlatform)
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

  composeOptions {
    kotlinCompilerExtensionVersion = libs.versions.kotlinCompilerExtensionVersion.get()
  }

  buildFeatures {
    compose = true
  }
}