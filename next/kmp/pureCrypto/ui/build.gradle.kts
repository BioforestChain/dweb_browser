plugins {
  id("kmp-library")
}

kotlin {
  val commonCryptoMain by sourceSets.creating {
    dependencies {
      implementation(libs.whyoleg.cryptography.core)
    }
  }

  kmpBrowserJsTarget(project) {
    dependsOn(commonCryptoMain)
    dependencies {
      implementation(libs.whyoleg.cryptography.provider.webcrypto)
    }
  }
}
