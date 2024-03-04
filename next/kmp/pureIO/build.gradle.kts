plugins {
  id("kmp-library")
}

kotlin {
  kmpCommonTarget(project) {
    dependencies {
      api(libs.squareup.okio)

      implementation(projects.helper)
    }

    @Suppress("OPT_IN_USAGE")
    applyHierarchy {
      common {
        group("jvm") {
          withJvm()
          withAndroidTarget()
        }
        withIosTarget()
      }
    }
  }
  kmpAndroidTarget(project) {
  }
  kmpIosTarget(project) {
  }
  kmpDesktopTarget(project) {
  }
}
