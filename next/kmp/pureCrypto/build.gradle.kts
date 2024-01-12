plugins {
  id("kmp-library")
}

kotlin {
  val commonCryptoMain by sourceSets.creating {
    dependencies {
      implementation(libs.whyoleg.cryptography.core)

      implementation(projects.helper)
    }
  }
  kmpAndroidTarget(libs) {
    dependsOn(commonCryptoMain)
    dependencies {
      implementation(libs.whyoleg.cryptography.provider.jdk)
    }
  }
  kmpIosTarget(libs) {
    dependsOn(commonCryptoMain)
    dependencies {
      implementation(libs.whyoleg.cryptography.provider.openssl3.prebuilt)
    }
  }
  kmpBrowserJsTarget(libs) {
    dependsOn(commonCryptoMain)
    dependencies {
      implementation(libs.whyoleg.cryptography.provider.webcrypto)
    }
  }
  kmpNodeWasmTarget(libs) {
    dependencies {
      implementation(projects.helperPlatformNode)
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

