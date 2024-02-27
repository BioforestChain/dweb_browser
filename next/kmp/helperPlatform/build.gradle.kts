plugins {
  id("kmp-compose")
}

kotlin {
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
