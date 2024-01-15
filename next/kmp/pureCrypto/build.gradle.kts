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

  kmpAndroidTarget(project) {
    dependsOn(commonCryptoMain)
    dependencies {
      implementation(libs.whyoleg.cryptography.provider.jdk)
    }
  }
  kmpIosTarget(project) {
    dependsOn(commonCryptoMain)
    dependencies {
      implementation(libs.whyoleg.cryptography.provider.openssl3.prebuilt)
    }
  }
  kmpNodeJsTarget(project) {
    dependsOn(commonCryptoMain)
    dependencies {
      implementation(libs.whyoleg.cryptography.provider.webcrypto)
    }
  }
}


