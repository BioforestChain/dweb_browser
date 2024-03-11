plugins {
  id("kmp-library")
}

kotlin {
  kmpCommonTarget(project) {
    dependencies {
      implementation(libs.whyoleg.cryptography.core)
//      implementation(projects.helper)
    }

    @Suppress("OPT_IN_USAGE")
    applyHierarchy {
      common {
        group("jvm") {
          dependencies {
            implementation(libs.whyoleg.cryptography.provider.jdk)
          }
          withDesktopTarget()
          withAndroidTarget()
        }
        withIosTarget()
      }
    }
  }

  kmpAndroidTarget(project)
  kmpIosTarget(project) {
    dependencies {
      implementation(libs.whyoleg.cryptography.provider.openssl3.prebuilt)
    }
  }
  kmpDesktopTarget(project)
}


