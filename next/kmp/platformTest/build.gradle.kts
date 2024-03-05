plugins {
//  alias(libs.plugins.kotlinMultiplatform)
//  alias(libs.plugins.androidLibrary)
  id("kmp-library")
}


kotlin {
  kmpCommonTarget(project) {
    dependencies {
      implementation(libs.test.kotlin.coroutines.test)
    }
  }
  kmpAndroidTarget(project)
  kmpIosTarget(project)
  kmpNodeJsTarget(project)
  kmpDesktopTarget(project)
}
