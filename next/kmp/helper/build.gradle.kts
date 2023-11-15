import org.dweb_browser.buildsrc.commonMobileTarget

plugins {
  id(libs.plugins.kotlinxMultiplatform.get().pluginId)
  id(libs.plugins.androidLibrary.get().pluginId)
  kotlin("plugin.serialization") version (libs.versions.kotlin.version)
}

kotlin {
  commonMobileTarget()

  sourceSets.commonMain.dependencies {
    api(kotlin("stdlib"))
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.atomicfu)
    api(libs.ktor.http)
    api(libs.ktor.io)
    api(libs.ktor.server.websockets)
    api(libs.ktor.server.cio)
    api(libs.ktor.client.cio)
    api(libs.ktor.client.encoding)
    api(libs.whyoleg.cryptography.core)

    api(libs.squareup.okio)
    api(libs.kotlinx.datetime)
    api(libs.kotlin.serialization.json)
    api(libs.kotlin.serialization.cbor)
  }
  sourceSets.commonTest.dependencies {
    implementation(kotlin("test"))
    implementation(libs.test.kotlin.coroutines.test)
    implementation(libs.test.kotlin.coroutines.debug)
    implementation(libs.kotlinx.atomicfu)
    implementation(libs.kotlinx.io)
  }
  sourceSets.androidMain.dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.commons.compress)
    api(libs.data.store)
    api(libs.data.store.preferences)
    api(libs.whyoleg.cryptography.provider.jdk)
  }
  sourceSets.iosMain.dependencies {
    api(libs.whyoleg.cryptography.provider.openssl3.prebuilt)
  }
}

android {
  namespace = "org.dweb_browser.helper"
  compileSdk = libs.versions.compileSdkVersion.get().toInt()
  defaultConfig {
    minSdk = libs.versions.minSdkVersion.get().toInt()
  }
}