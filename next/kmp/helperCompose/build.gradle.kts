plugins {
  id("kmp-compose")
}

kotlin {
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
  kmpBrowserJsTarget(project)
}
