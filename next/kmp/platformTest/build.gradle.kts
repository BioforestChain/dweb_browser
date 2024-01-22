plugins {
//  alias(libs.plugins.kotlinMultiplatform)
//  alias(libs.plugins.androidLibrary)
  id("kmp-library")
}

kotlin {
  kmpAndroidTarget(project)
  kmpIosTarget(project)
  kmpNodeJsTarget(project)
}
