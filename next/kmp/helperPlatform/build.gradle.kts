plugins {
  id("kmp-compose")
}

kotlin {
  kmpCommonTarget(project) {

    @Suppress("OPT_IN_USAGE")
    applyHierarchy {
      common {
        group("skiko") {
          withIosTarget()
          withDesktopTarget()
        }
      }
    }
  }
  kmpComposeTarget(project) {
    dependencies {
      implementation(projects.helper)
    }
  }
  kmpAndroidTarget(project) {
    dependencies {
      implementation(libs.accompanist.systemui.controller)
      implementation(libs.google.material)
    }
  }
  kmpIosTarget(project) {
  }
  kmpDesktopTarget(project) {
  }
}
