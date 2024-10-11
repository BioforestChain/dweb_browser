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
  kmpAndroidTarget(project) {
    dependencies {
      api(libs.androidx.test.core)
      api(libs.androidx.test.core.ktx)
    }
  }
  kmpIosTarget(project)
  kmpDesktopTarget(project) {
    dependencies {
      implementation(libs.test.kotlin.coroutines.debug)
    }
  }
}
