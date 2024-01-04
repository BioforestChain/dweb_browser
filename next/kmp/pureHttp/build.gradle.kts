plugins {
  id("kmp-library")
}

kotlin {
//  desktopBackendTarget()
  sourceSets {
//    jsMain {
//
//    }
    commonMain.dependencies {
      api(kotlin("stdlib"))
      api(libs.kotlinx.coroutines.core)
      api(libs.kotlinx.atomicfu)
      implementation(libs.kotlinx.io)
      api(libs.whyoleg.cryptography.core)

      implementation(libs.kotlinx.datetime)
      implementation(libs.kotlin.serialization.json)
      implementation(libs.kotlin.serialization.cbor)

      implementation(projects.helper)
      implementation(projects.helperCompose)
      implementation(projects.helperPlatform)
    }
    val androidAndIosMain by creating {
      dependencies {
        api(libs.ktor.http)
        api(libs.ktor.client.cio)
        api(libs.ktor.client.encoding)

        implementation(libs.ktor.io)
        implementation(libs.ktor.server.websockets)
        implementation(libs.ktor.server.cio)
      }
    }
    commonTest.dependencies {
      implementation(kotlin("test"))
      implementation(libs.test.kotlin.coroutines.test)
      implementation(libs.test.kotlin.coroutines.debug)
    }
    androidMain {
      dependsOn(androidAndIosMain)
      dependencies {
        implementation(libs.androidx.core.ktx)
        api(libs.whyoleg.cryptography.provider.jdk)
      }
    }
    iosMain {
      dependsOn(androidAndIosMain)
      dependencies {
        api(libs.whyoleg.cryptography.provider.openssl3.prebuilt)
      }
    }
  }
}

android {
  namespace = "org.dweb_browser.pure.http"
  compileSdk = libs.versions.compileSdkVersion.get().toInt()
  defaultConfig {
    minSdk = libs.versions.minSdkVersion.get().toInt()
  }
}

