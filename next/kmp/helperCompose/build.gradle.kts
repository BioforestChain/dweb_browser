plugins {
  id("kmp-compose")
}

kotlin {
  kmpCommonTarget(project) {
    @Suppress("OPT_IN_USAGE")
    applyHierarchy {
      common {
        group("mobile") {
          withAndroidTarget()
          withIosTarget()
        }
        group("skiko") {
          withDesktopTarget()
          withIosTarget()
        }
      }
    }
  }

  kmpComposeTarget(project) {
    dependencies {
      implementation(projects.helper)
      implementation(projects.helperPlatform)
    }
  }
  kmpAndroidTarget(project) {
    dependencies {
      implementation(libs.compose.material3)
    }
  }
  kmpIosTarget(project)
  kmpDesktopTarget(project)
}
