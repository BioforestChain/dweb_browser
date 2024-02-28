plugins {
//  alias(libs.plugins.kotlinMultiplatform)
//  alias(libs.plugins.androidLibrary)
  id("kmp-library")
}

kotlin {
  kmpDesktopTarget(project) {
    dependencies {
    }
  }
}
