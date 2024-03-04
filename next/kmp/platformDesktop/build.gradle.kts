plugins {
//  alias(libs.plugins.kotlinMultiplatform)
//  alias(libs.plugins.androidLibrary)
  id("kmp-library")
}

kotlin {
  kmpDesktopTarget(project) {
    dependencies {
      api(libs.jxbrowser.cross.platform)
      api(libs.jxbrowser.swing)
    }
  }
}
