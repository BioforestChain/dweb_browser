plugins {
  id("kmp-library")
}

kotlin {
  kmpCommonTarget(project) {
    dependencies {
      implementation(libs.whyoleg.cryptography.core)
//      implementation(projects.helper)
    }
  }

  kmpAndroidTarget(project) {
    dependencies {
      implementation(libs.whyoleg.cryptography.provider.jdk)
    }
  }
  kmpIosTarget(project) {
    dependencies {
      implementation(libs.whyoleg.cryptography.provider.openssl3.prebuilt)
    }
  }
  kmpDesktopTarget(project) {
    dependencies {
      implementation(libs.whyoleg.cryptography.provider.jdk)
    }
  }
}


