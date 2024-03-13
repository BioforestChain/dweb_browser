plugins {
  id("kmp-compose")
}

kotlin {
  kmpCommonTarget(project) {
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
