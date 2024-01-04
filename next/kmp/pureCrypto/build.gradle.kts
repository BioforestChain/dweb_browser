plugins {
  id("kmp-library")
}

kotlin {
  kmpLibraryTarget(libs)
//  kmpWasiTarget(libs)
  sourceSets {
//    jsMain {
//      dependencies {
//        implementation(libs.whyoleg.cryptography.provider.webcrypto)
//      }
//    }
    val androidAndIosMain by creating {
      dependencies {
        implementation(libs.whyoleg.cryptography.core)
      }
    }
    androidMain {
      dependsOn(androidAndIosMain)
      dependencies {
        implementation(libs.androidx.core.ktx)
        implementation(libs.whyoleg.cryptography.provider.jdk)
      }
    }
    iosMain {
      dependsOn(androidAndIosMain)
      dependencies {
        implementation(libs.whyoleg.cryptography.provider.openssl3.prebuilt)
      }
    }
  }
}

android {
  namespace = "org.dweb_browser.pure.crypto"
  compileSdk = libs.versions.compileSdkVersion.get().toInt()
  defaultConfig {
    minSdk = libs.versions.minSdkVersion.get().toInt()
  }
}

