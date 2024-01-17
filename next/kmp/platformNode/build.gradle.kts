plugins {
//  alias(libs.plugins.kotlinMultiplatform)
//  alias(libs.plugins.androidLibrary)
  id("kmp-library")
}

kotlin {
  kmpNodeJsTarget(project) {
    dependencies {
      api(libs.kotlin.js)
      api(libs.kotlin.node)
    }
  }
}
